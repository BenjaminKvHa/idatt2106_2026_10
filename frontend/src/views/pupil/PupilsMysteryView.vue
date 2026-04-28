<script setup lang="ts">
import { computed, ref } from 'vue'
import TopBar from '@/components/TopBar.vue'

type SubmittedMystery = {
  id: number
  imageUrl: string
  comment: string
}

type WeeklyMystery = {
  id: number
  title: string
  weekLabel: string
  mysteries: SubmittedMystery[]
}

const activeTab = ref<'current' | 'previous'>('current')
const currentIndex = ref(0)
const showSubmitPopup = ref(false)

const uploadedImage = ref<File | null>(null)
const uploadedImagePreview = ref('')
const imageComment = ref('')

const mysteries = ref<WeeklyMystery[]>([
  {
    id: 1,
    title: 'Denne ukens innsendte mysterier',
    weekLabel: 'Uke 17',
    mysteries: [
      {
        id: 1,
        imageUrl: '',
        comment:
          'Dette bildet ser mistenkelig ut fordi teksten i bakgrunnen er uklar og noen detaljer virker unaturlige.',
      },
      {
        id: 2,
        imageUrl: '',
        comment:
          'Jeg tror dette kan være KI-generert fordi ansiktene i bildet ikke ser helt riktige ut.',
      },
      {
        id: 3,
        imageUrl: '',
        comment: 'Bildet virker redigert fordi lyset og skyggene ikke passer sammen.',
      },
    ],
  },
  {
    id: 2,
    title: 'Tidligere mysterier',
    weekLabel: 'Uke 16',
    mysteries: [
      {
        id: 4,
        imageUrl: '',
        comment:
          'Et bilde så ekte ut ved første øyekast, men flere detaljer avslørte at det kunne være manipulert.',
      },
    ],
  },
  {
    id: 3,
    title: 'Tidligere mysterier',
    weekLabel: 'Uke 15',
    mysteries: [
      {
        id: 5,
        imageUrl: '',
        comment: 'En påstand ble delt av mange, men ingen kunne finne en troverdig kilde.',
      },
    ],
  },
])

const currentMystery = computed(() => mysteries.value[0])
const previousMysteries = computed(() => mysteries.value.slice(1))

const visibleMysteries = computed(() =>
  activeTab.value === 'current' ? [currentMystery.value] : previousMysteries.value,
)

const selectedMystery = computed(() => visibleMysteries.value[currentIndex.value])

const setTab = (tab: 'current' | 'previous') => {
  activeTab.value = tab
  currentIndex.value = 0
}

const nextMystery = () => {
  if (currentIndex.value < visibleMysteries.value.length - 1) {
    currentIndex.value++
  }
}

const previousMystery = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
  }
}

const openSubmitPopup = () => {
  showSubmitPopup.value = true
}

const closeSubmitPopup = () => {
  showSubmitPopup.value = false
  uploadedImage.value = null
  uploadedImagePreview.value = ''
  imageComment.value = ''
}

const handleImageUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  uploadedImage.value = file
  uploadedImagePreview.value = URL.createObjectURL(file)
}

const submitMystery = () => {
  if (!uploadedImagePreview.value || !imageComment.value.trim()) return
  if (!currentMystery.value) return

  currentMystery.value.mysteries.unshift({
    id: Date.now(),
    imageUrl: uploadedImagePreview.value,
    comment: imageComment.value,
  })

  closeSubmitPopup()
}
</script>

<template>
  <div class="page">
    <TopBar title="Ukens mysterium" />

    <main class="content">
      <section class="mystery-panel">
        <div class="header">
          <div>
            <p class="eyebrow">Detektivbyrået presenterer</p>
            <h1>UKENS MYSTERIUM</h1>
          </div>

          <button class="create-button" @click="openSubmitPopup">Send inn mysterium</button>
        </div>

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

        <div v-if="selectedMystery" class="mystery-card">
          <div class="week-nav">
            <button
              v-if="activeTab === 'previous'"
              class="arrow-button"
              :disabled="currentIndex === 0"
              @click="previousMystery"
            >
              ←
            </button>

            <h2>{{ selectedMystery.weekLabel }}</h2>

            <button
              v-if="activeTab === 'previous'"
              class="arrow-button"
              :disabled="currentIndex === visibleMysteries.length - 1"
              @click="nextMystery"
            >
              →
            </button>
          </div>

          <div class="scroll-box">
            <div
              v-for="mystery in selectedMystery.mysteries"
              :key="mystery.id"
              class="submitted-card"
            >
              <div v-if="mystery.imageUrl" class="image-preview">
                <img :src="mystery.imageUrl" alt="Innsendt mysterium" />
              </div>

              <div v-else class="image-placeholder">
                <span>Eksempelbilde</span>
              </div>

              <div class="comment-box">
                <span class="case-label">Kommentar</span>
                <p>{{ mystery.comment }}</p>
              </div>
            </div>
          </div>

          <p v-if="activeTab === 'previous'" class="page-number">
            {{ currentIndex + 1 }} / {{ visibleMysteries.length }}
          </p>
        </div>

        <div v-else class="empty-state">Ingen mysterier er lagt inn enda.</div>
      </section>
    </main>

    <div v-if="showSubmitPopup" class="popup-overlay" @click.self="closeSubmitPopup">
      <div class="popup">
        <button class="close-button" @click="closeSubmitPopup">×</button>

        <h2>Send inn mysterium</h2>
        <p class="popup-text">
          Last opp et bilde og legg ved en kommentar om hvorfor bildet er mistenkelig.
        </p>

        <label>
          Bilde
          <input type="file" accept="image/*" @change="handleImageUpload" />
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

        <div class="popup-buttons">
          <button class="cancel-button" @click="closeSubmitPopup">Avbryt</button>

          <button
            class="submit-button"
            :disabled="!uploadedImagePreview || !imageComment.trim()"
            @click="submitMystery"
          >
            Send inn
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background:
    linear-gradient(rgba(20, 12, 30, 0.45), rgba(20, 12, 30, 0.65)),
    url('@/assets/backgrounds/ukens_mysterium-bg.png') center center / cover no-repeat fixed;
  background-attachment: fixed;
}

.content {
  min-height: calc(100vh - 80px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2rem;
}

.mystery-panel {
  width: min(900px, 94vw);
  background: rgba(35, 22, 45, 0.9);
  border: 4px solid #d99b54;
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
  color: #f5c16c;
  font-family: 'Inter', sans-serif;
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
  color: #fff2d2;
  font-size: clamp(2rem, 4vw, 3.1rem);
  text-shadow: 0 3px 0 rgba(0, 0, 0, 0.35);
}

.create-button {
  background: #fff2d2;
  color: #2b1935;
  border: 4px solid #9b6335;
  border-radius: 20px;
  padding: 0.8rem 1.4rem;
  font-size: 1.15rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow:
    0 8px 0 #5b351d,
    0 13px 18px rgba(0, 0, 0, 0.45);
}

.create-button:hover {
  transform: translateY(-2px);
  box-shadow:
    0 10px 0 #5b351d,
    0 15px 20px rgba(0, 0, 0, 0.5);
}

.create-button:active {
  transform: translateY(5px);
  box-shadow:
    0 3px 0 #5b351d,
    0 8px 12px rgba(0, 0, 0, 0.45);
}

.tabs {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin: 2rem 0 1.4rem;
}

.tab-button {
  background: #fff2d2;
  border: 3px solid #101010;
  border-radius: 18px;
  padding: 0.65rem 1.5rem;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 5px 0 rgba(0, 0, 0, 0.35);
}

.tab-button.active {
  background: #9b6335;
  color: #fff2d2;
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
  color: #fff2d2;
  font-size: 2rem;
}

.arrow-button {
  background: transparent;
  border: none;
  color: #f5c16c;
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
  max-width: 720px;
  max-height: 440px;
  overflow-y: auto;
  padding: 1rem;
  background: rgba(18, 10, 24, 0.65);
  border: 3px solid #9b6335;
  border-radius: 24px;
  box-shadow: inset 0 0 18px rgba(0, 0, 0, 0.35);
}

.scroll-box::-webkit-scrollbar {
  width: 16px;
}

.scroll-box::-webkit-scrollbar-track {
  background: #fff2d2;
  border-radius: 999px;
  border: 3px solid #101010;
}

.scroll-box::-webkit-scrollbar-thumb {
  background: #9b6335;
  border-radius: 999px;
  border: 3px solid #fff2d2;
}

.scroll-box::-webkit-scrollbar-thumb:hover {
  background: #6f4324;
}

.submitted-card {
  background: rgba(255, 242, 210, 0.96);
  border: 3px solid #101010;
  border-radius: 22px;
  padding: 1rem;
  margin-bottom: 1rem;
  box-shadow: 0 7px 0 rgba(0, 0, 0, 0.3);
}

.submitted-card:last-child {
  margin-bottom: 0;
}

.image-preview,
.image-placeholder {
  width: 100%;
  height: 230px;
  border: 3px solid #101010;
  border-radius: 18px;
  overflow: hidden;
  background: linear-gradient(135deg, #6f4324, #2b1935);
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder span {
  color: #fff2d2;
  font-family: 'Inika', serif;
  font-size: 1.6rem;
}

.comment-box {
  margin-top: 1rem;
  text-align: center;
}

.case-label {
  display: inline-block;
  background: #9b6335;
  color: #fff2d2;
  border: 2px solid #101010;
  border-radius: 999px;
  padding: 0.25rem 0.9rem;
  margin-bottom: 0.6rem;
  font-family: 'Inter', sans-serif;
  font-size: 0.9rem;
  font-weight: 800;
  text-transform: uppercase;
}

.comment-box p {
  margin: 0;
  font-family: 'Inter', sans-serif;
  font-size: 1.05rem;
  line-height: 1.45;
}

.page-number {
  margin: 1rem 0 0;
  color: #fff2d2;
  font-family: 'Inika', serif;
  font-size: 1.5rem;
  font-weight: 700;
}

.empty-state {
  background: rgba(255, 242, 210, 0.96);
  border: 3px solid #101010;
  border-radius: 20px;
  padding: 2rem;
  text-align: center;
  font-family: 'Inter', sans-serif;
  font-size: 1.2rem;
}

.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(12, 7, 18, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem;
  z-index: 1000;
}

.popup {
  position: relative;
  width: min(560px, 92vw);
  max-height: 90vh;
  overflow-y: auto;
  background: #fff2d2;
  border: 4px solid #101010;
  border-radius: 24px;
  padding: 2rem;
  box-shadow: 0 12px 0 rgba(0, 0, 0, 0.45);
}

.popup h2 {
  margin: 0 0 0.5rem;
  font-size: 2rem;
  color: #2b1935;
}

.popup-text {
  margin: 0 0 1.3rem;
  font-family: 'Inter', sans-serif;
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
  font-family: 'Inter', sans-serif;
  font-weight: 700;
}

input,
textarea {
  border: 3px solid #101010;
  border-radius: 14px;
  padding: 0.8rem;
  font-family: 'Inter', sans-serif;
  font-size: 1rem;
  background: #fffaf0;
}

textarea {
  min-height: 120px;
  resize: vertical;
}

.popup-image-preview {
  width: 100%;
  height: 220px;
  border: 3px solid #101010;
  border-radius: 18px;
  overflow: hidden;
  margin-bottom: 1rem;
}

.popup-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.popup-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.2rem;
}

.cancel-button,
.submit-button {
  border: 3px solid #101010;
  border-radius: 16px;
  padding: 0.65rem 1.2rem;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
}

.cancel-button {
  background: #e8d9bd;
}

.submit-button {
  background: #9b6335;
  color: #fff2d2;
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
    max-height: 390px;
  }

  .image-preview,
  .image-placeholder {
    height: 190px;
  }

  .popup-buttons {
    flex-direction: column;
  }

  .cancel-button,
  .submit-button {
    width: 100%;
  }
}
</style>
