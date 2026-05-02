<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import TopBar from '@/components/pupil/TopBar.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue'
import mysteryBackground from '@/assets/backgrounds/ukens_mysterium-bg.png'
import { get, postForm } from '@/utils/apiUtils'

type SubmittedMystery = {
  id: number
  fileObjectId: number
  comment: string
  altText: string | null
}

type WeekGroup = {
  weekKey: string
  weekLabel: string
  mysteries: SubmittedMystery[]
}

type PupilWeeklyMysteryResponse = {
  id: number
  fileObjectId: number
  comment: string
  altText: string | null
  createdAt: string
  approvedAt: string | null
}

type ProfileResponse = {
  classroomCode: string | null
}

const activeTab = ref<'current' | 'previous'>('current')
const currentIndex = ref(0)
const showSubmitPopup = ref(false)

const uploadedImage = ref<File | null>(null)
const uploadedImagePreview = ref('')
const imageComment = ref('')

const classroomCode = ref<string | null>(null)
const loading = ref(true)
const loadError = ref<string | null>(null)
const submitError = ref<string | null>(null)
const submitSuccess = ref<string | null>(null)
const submitting = ref(false)

const groups = ref<WeekGroup[]>([])

function isoWeekParts(date: Date): { key: string; week: number; year: number } {
  const tmp = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
  const dayNum = tmp.getUTCDay() || 7
  tmp.setUTCDate(tmp.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1))
  const week = Math.ceil(((tmp.getTime() - yearStart.getTime()) / 86400000 + 1) / 7)
  const year = tmp.getUTCFullYear()
  return { key: `${year}-W${String(week).padStart(2, '0')}`, week, year }
}

async function loadMysteries() {
  loading.value = true
  loadError.value = null
  groups.value = []

  try {
    const profile = await get<ProfileResponse>('/api/me')
    classroomCode.value = profile.classroomCode
    if (!classroomCode.value) {
      loading.value = false
      return
    }

    const items = await get<PupilWeeklyMysteryResponse[]>(
      `/api/pupil/classrooms/${classroomCode.value}/weekly-mysteries`,
    )

    const buckets = new Map<string, WeekGroup>()
    const now = isoWeekParts(new Date())

    for (const item of items) {
      const ts = item.approvedAt ?? item.createdAt
      const wk = isoWeekParts(new Date(ts))
      let group = buckets.get(wk.key)
      if (!group) {
        group = { weekKey: wk.key, weekLabel: `Uke ${wk.week}`, mysteries: [] }
        buckets.set(wk.key, group)
      }

      group.mysteries.push({ id: item.id, fileObjectId: item.fileObjectId, comment: item.comment, altText: item.altText })
    }

    const sorted = Array.from(buckets.values()).sort((a, b) => (a.weekKey < b.weekKey ? 1 : -1))
    const currentIdx = sorted.findIndex((g) => g.weekKey === now.key)
    if (currentIdx === -1) {
      sorted.unshift({ weekKey: now.key, weekLabel: `Uke ${now.week}`, mysteries: [] })
    } else if (currentIdx > 0) {
      const [cur] = sorted.splice(currentIdx, 1)
      if (cur) sorted.unshift(cur)
    }
    groups.value = sorted
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : 'Kunne ikke laste mysterier'
  } finally {
    loading.value = false
  }
}

const currentGroup = computed(() => groups.value[0])
const previousGroups = computed(() => groups.value.slice(1))

const visibleGroups = computed(() =>
  activeTab.value === 'current'
    ? currentGroup.value
      ? [currentGroup.value]
      : []
    : previousGroups.value,
)

const selectedGroup = computed(() => visibleGroups.value[currentIndex.value])

const pageStyle = computed(() => ({
  background: `
    linear-gradient(rgba(20, 12, 30, 0.45), rgba(20, 12, 30, 0.65)),
    url(${mysteryBackground}) center center / cover no-repeat fixed
  `,
}))

const setTab = (tab: 'current' | 'previous') => {
  activeTab.value = tab
  currentIndex.value = 0
}

const nextMystery = () => {
  if (currentIndex.value < visibleGroups.value.length - 1) {
    currentIndex.value++
  }
}

const previousMystery = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const openSubmitPopup = () => {
  submitError.value = null
  submitSuccess.value = null
  showSubmitPopup.value = true
}

const closeSubmitPopup = () => {
  showSubmitPopup.value = false
  uploadedImage.value = null
  if (uploadedImagePreview.value) {
    URL.revokeObjectURL(uploadedImagePreview.value)
  }
  uploadedImagePreview.value = ''
  imageComment.value = ''
  submitError.value = null
}

const handleImageUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  if (uploadedImagePreview.value) {
    URL.revokeObjectURL(uploadedImagePreview.value)
  }
  uploadedImage.value = file
  uploadedImagePreview.value = URL.createObjectURL(file)
}

const submitMystery = async () => {
  if (!uploadedImage.value || !imageComment.value.trim()) return
  if (!classroomCode.value) {
    submitError.value = 'Du må være med i et klasserom for å sende inn.'
    return
  }

  submitting.value = true
  submitError.value = null
  try {
    const formData = new FormData()
    formData.append('file', uploadedImage.value)
    formData.append('comment', imageComment.value.trim())
    await postForm(
      `/api/pupil/classrooms/${classroomCode.value}/weekly-mysteries`,
      formData,
    )
    closeSubmitPopup()
    submitSuccess.value = 'Sendt inn til godkjenning!'
    await loadMysteries()
  } catch (err) {
    submitError.value = err instanceof Error ? err.message : 'Kunne ikke sende inn mysterium'
  } finally {
    submitting.value = false
  }
}

onMounted(loadMysteries)
onBeforeUnmount(() => {
  if (uploadedImagePreview.value) {
    URL.revokeObjectURL(uploadedImagePreview.value)
  }
})
</script>

<template>
  <div class="page" :style="pageStyle">
    <TopBar title="Ukens mysterium" />

    <main class="content pupil-fullscreen-center">
      <section class="mystery-panel">
        <div class="header">
          <div>
            <h1>UKENS MYSTERIUM</h1>
          </div>

          <button
            class="create-button"
            :disabled="!classroomCode"
            @click="openSubmitPopup"
          >
            Send inn mysterium
          </button>
        </div>

        <p v-if="submitSuccess" class="info-banner success">{{ submitSuccess }}</p>
        <p v-if="loadError" class="info-banner error">{{ loadError }}</p>

        <div v-if="loading" class="empty-state">Laster mysterier...</div>

        <div v-else-if="!classroomCode" class="empty-state">
          Du må være medlem av et klasserom for å se ukens mysterium.
        </div>

        <template v-else>
          <div class="tabs">
            <button
              class="tab-button"
              :class="{ active: activeTab === 'current' }"
              @click="setTab('current')"
            >
              Denne uken
            </button>

            <button
              class="tab-button"
              :class="{ active: activeTab === 'previous' }"
              @click="setTab('previous')"
            >
              Tidligere
            </button>
          </div>

          <div v-if="selectedGroup" class="mystery-card">
            <div class="week-nav">
              <button
                v-if="activeTab === 'previous'"
                class="arrow-button"
                :disabled="currentIndex === 0"
                @click="previousMystery"
              >
                ←
              </button>

              <h2>{{ selectedGroup.weekLabel }}</h2>

              <button
                v-if="activeTab === 'previous'"
                class="arrow-button"
                :disabled="currentIndex === visibleGroups.length - 1"
                @click="nextMystery"
              >
                →
              </button>
            </div>

            <div v-if="selectedGroup.mysteries.length === 0" class="empty-state">
              Ingen godkjente mysterier denne uken enda.
            </div>

            <div v-else class="scroll-box">
              <div
                v-for="mystery in selectedGroup.mysteries"
                :key="mystery.id"
                class="submitted-card"
              >
                <AuthenticatedImage
                  :src="`/api/objects/${mystery.fileObjectId}`"
                  :alt="mystery.altText ?? 'Innsendt mysterium'"
                  class="image-preview"
                  preview-on-click
                />

                <div class="comment-box">
                  <span class="case-label">Kommentar</span>
                  <p>{{ mystery.comment }}</p>
                </div>
              </div>
            </div>

            <p v-if="activeTab === 'previous'" class="page-number">
              {{ currentIndex + 1 }} / {{ visibleGroups.length }}
            </p>
          </div>

          <div v-else class="empty-state">Ingen mysterier er lagt inn enda.</div>
        </template>
      </section>
    </main>

    <BaseModal
      v-if="showSubmitPopup"
      :close-on-backdrop="true"
      :close-on-escape="true"
      @close="closeSubmitPopup"
    >
      <div class="mystery-submit-popup-wrapper">
        <h2>Send inn mysterium</h2>

        <p class="popup-text">
          Last opp et bilde og legg ved en kommentar om hvorfor bildet er mistenkelig.
        </p>

        <label>
          Bilde
          <input type="file" accept="image/png,image/jpeg" @change="handleImageUpload" />
        </label>

        <div v-if="uploadedImagePreview" class="popup-image-preview">
          <img :src="uploadedImagePreview" alt="Forhåndsvisning" />
        </div>

      <label>
        Kommentar
        <textarea
          v-model="imageComment"
          placeholder="Skriv hvorfor bildet er mistenkelig..."
        ></textarea>
      </label>

      <div class="popup-buttons pupil-responsive-button-row">
        <button class="cancel-button" @click="closeSubmitPopup">Avbryt</button>

        <button
          class="submit-button"
          :disabled="!uploadedImagePreview || !imageComment.trim() || submitting"
          @click="submitMystery"
        >
          {{ submitting ? 'Sender...' : 'Send inn' }}
        </button>
      </div>
    </div>
  </BaseModal>

  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background-attachment: fixed;
}

.content {
  padding: 2rem;
}

.mystery-panel {
  width: min(1100px, 94vw);
  background: rgba(35, 22, 45, 0.9);
  border: var(--border-wide) solid var(--brown-medium);
  border-radius: 28px;
  padding: 2rem;
  box-shadow: 0 18px 45px rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(4px);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1.5rem;
}

.eyebrow {
  margin: 0 0 0.3rem;
  color: var(--yellow-award);
  font-size: 0.95rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1,
h2,
h3,
button {
  font-family: 'Inika', serif;
}

h1 {
  margin: 0;
  color: var(--beige-light);
  font-size: clamp(2rem, 4vw, 3.1rem);
  text-shadow: 0 3px 0 rgba(0, 0, 0, 0.35);
}

.create-button {
  background: var(--beige-light);
  color: var(--brown-dark);
  border: var(--border-wide) solid var(--brown-medium);
  border-radius: 20px;
  padding: 0.8rem 1.4rem;
  font-size: 1.15rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow:
    0 8px 0 var(--brown-dark),
    0 13px 18px rgba(0, 0, 0, 0.45);
}

.create-button:hover {
  transform: translateY(-2px);
  box-shadow:
    0 10px 0 var(--brown-dark),
    0 15px 20px rgba(0, 0, 0, 0.5);
}

.create-button:active {
  transform: translateY(5px);
  box-shadow:
    0 3px 0 var(--brown-dark),
    0 8px 12px rgba(0, 0, 0, 0.45);
}

.tabs {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin: 2rem 0 1.4rem;
}

.tab-button {
  background: var(--beige-light);
  border: var(--border-thin) solid var(--color-text);
  border-radius: 18px;
  padding: 0.65rem 1.5rem;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 5px 0 rgba(0, 0, 0, 0.35);
}

.tab-button.active {
  background: var(--brown-medium);
  color: var(--beige-light);
}

.mystery-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.week-nav {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 1.2rem;
  margin-bottom: 1rem;
}

.week-nav h2 {
  margin: 0;
  color: var(--beige-light);
  font-size: 2rem;
}

.arrow-button {
  background: transparent;
  border: none;
  color: var(--yellow-award);
  font-size: 2rem;
  font-weight: 700;
  cursor: pointer;
}

.arrow-button:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.scroll-box {
  width: 100%;
  max-width: 900px;
  max-height: 560px;
  overflow-y: auto;
  padding: 1rem;
  background: rgba(18, 10, 24, 0.65);
  border: var(--border-thin) solid var(--brown-medium);
  border-radius: 24px;
  box-shadow: inset 0 0 18px rgba(0, 0, 0, 0.35);
}

.scroll-box::-webkit-scrollbar {
  width: 16px;
}

.scroll-box::-webkit-scrollbar-track {
  background: var(--beige-light);
  border-radius: 999px;
  border: var(--border-thin) solid var(--color-text);
}

.scroll-box::-webkit-scrollbar-thumb {
  background: var(--brown-medium);
  border-radius: 999px;
  border: var(--border-thin) solid var(--beige-light);
}

.scroll-box::-webkit-scrollbar-thumb:hover {
  background: var(--brown-dark);
}

.submitted-card {
  background: var(--beige-light);
  border: var(--border-thin) solid var(--color-text);
  border-radius: 22px;
  padding: 1.2rem;
  margin-bottom: 1rem;
  box-shadow: 0 7px 0 rgba(0, 0, 0, 0.3);
}

.submitted-card:last-child {
  margin-bottom: 0;
}

.image-preview {
  width: 100%;
  min-height: 320px;
  border: var(--border-thin) solid var(--color-text);
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, #170e20, #2b1935);
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-preview {
  padding: 0;
  width: 100%;
  height: auto;
  max-height: 520px;
  object-fit: contain;
  display: block;
}

.comment-box {
  margin-top: 1rem;
  text-align: center;
}

.case-label {
  display: inline-block;
  background: var(--brown-medium);
  color: var(--beige-light);
  border: 2px solid var(--color-text);
  border-radius: 999px;
  padding: 0.25rem 0.9rem;
  margin-bottom: 0.6rem;
  font-size: 0.9rem;
  font-weight: 800;
  text-transform: uppercase;
}

.comment-box p {
  margin: 0;
  font-size: 1.05rem;
  line-height: 1.45;
}

.page-number {
  margin: 1rem 0 0;
  color: var(--beige-light);
  font-family: 'Inika', serif;
  font-size: 1.5rem;
  font-weight: 700;
}

.empty-state {
  background: var(--beige-light);
  border: var(--border-thin) solid var(--color-text);
  border-radius: 20px;
  padding: 2rem;
  text-align: center;
  font-size: 1.2rem;
}

.info-banner {
  margin: 1rem 0 0;
  padding: 0.7rem 1rem;
  border-radius: 14px;
  border: 2px solid var(--color-text);
  font-weight: 600;
  text-align: center;
}

.info-banner.success {
  background: #d6f0d2;
  color: #1f4d1a;
}

.info-banner.error {
  background: #f6c6c6;
  color: #5a1010;
}

.create-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.popup-overlay {
  background: rgba(12, 7, 18, 0.75);
}

.mystery-submit-popup-wrapper {
  position: relative;
  width: min(560px, 92vw);
  max-height: 90vh;
  overflow-y: auto;
  background: var(--beige-light);
  border: var(--border-wide) solid var(--color-text);
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 12px 0 rgba(0, 0, 0, 0.45);
}

.mystery-submit-popup-wrapper h2 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
  color: var(--brown-dark);
}

.mystery-submit-popup-wrapper .popup-text {
  margin: 0 0 1.3rem;
  line-height: 1.45;
}

.mystery-submit-popup-wrapper label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 1rem;
  font-weight: 700;
}

.mystery-submit-popup-wrapper input,
.mystery-submit-popup-wrapper textarea {
  border: var(--border-thin) solid var(--color-text);
  border-radius: 14px;
  padding: 0.8rem;
  font-size: 1rem;
  background: var(--beige-light);
}

.mystery-submit-popup-wrapper textarea {
  min-height: 120px;
  resize: vertical;
}

.mystery-submit-popup-wrapper .popup-image-preview {
  width: 100%;
  height: 220px;
  border: var(--border-thin) solid var(--color-text);
  border-radius: 18px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.mystery-submit-popup-wrapper .popup-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.popup-text {
  margin: 0 0 1.3rem;
  line-height: 1.45;
}

.close-button {
  position: absolute;
  top: 0.8rem;
  right: 1rem;
  background: transparent;
  border: none;
  font-size: 2rem;
  cursor: pointer;
}

label {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-bottom: 1rem;
  font-weight: 700;
}

input,
textarea {
  border: var(--border-thin) solid var(--color-text);
  border-radius: 14px;
  padding: 0.8rem;
  font-size: 1rem;
  background: var(--beige-light);
}

textarea {
  min-height: 120px;
  resize: vertical;
}

.popup-image-preview {
  width: 100%;
  height: 220px;
  border: var(--border-thin) solid var(--color-text);
  border-radius: 18px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.popup-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cancel-button,
.submit-button {
  border: var(--border-thin) solid var(--color-text);
  border-radius: 16px;
  padding: 0.65rem 1.2rem;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
}

.cancel-button {
  background: var(--beige-medium);
}

.submit-button {
  background: var(--brown-medium);
  color: var(--beige-light);
}

.submit-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .content {
    padding: 1rem;
  }

  .mystery-panel {
    padding: 1.2rem;
  }

  .header {
    flex-direction: column;
    text-align: center;
  }

  .create-button {
    width: 100%;
  }

  .tabs {
    flex-direction: column;
  }

  .tab-button {
    width: 100%;
  }

  .scroll-box {
    max-height: 450px;
  }

  .image-preview {
    min-height: 240px;
  }

  .cancel-button,
  .submit-button {
    width: 100%;
  }
}
</style>
