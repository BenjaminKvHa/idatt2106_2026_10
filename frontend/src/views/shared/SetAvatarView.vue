<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AvatarAssetsContainer from '@/components/pupil/AvatarAssetsContainer.vue'
import { put } from '@/utils/apiUtils'

type AvatarAsset = {
  assetType: string
  name: string
  filetype: string
}

type UpdateMembershipRequest = {
  username: string
  avatar: string
}

const route = useRoute()
const router = useRouter()

const username = ref('')
const loading = ref(true)
const errorMessage = ref('')
const saveError = ref('')
const saving = ref(false)
const skinAssets = ref<AvatarAsset[]>([])
const hairAssets = ref<AvatarAsset[]>([])
const costumeAssets = ref<AvatarAsset[]>([])
const currentSkincolor = ref<AvatarAsset | null>(null)
const currentHairstyle = ref<AvatarAsset | null>(null)
const currentCostume = ref<AvatarAsset | null>(null)

const previewLayers = computed(() => [currentSkincolor.value, currentHairstyle.value, currentCostume.value].filter(Boolean) as AvatarAsset[])
const classroomCode = computed(() => {
  const value = route.query.classroomCode
  return typeof value === 'string' ? value.trim() : ''
})
const avatarSelection = computed(() => {
  if (!currentSkincolor.value || !currentHairstyle.value || !currentCostume.value) {
    return ''
  }

  return `${currentSkincolor.value.name}_${currentHairstyle.value.name}_${currentCostume.value.name}`
})
const canSubmit = computed(() => classroomCode.value.length > 0 && username.value.trim().length > 0 && avatarSelection.value.length > 0 && !loading.value && !saving.value)

function getAssetImagePath(asset: AvatarAsset): string {
  return `/api/avatar-assets/${asset.assetType}/${asset.name}.${asset.filetype}`
}

function setCurrentHairstyle(hairstyle: AvatarAsset) {
  currentHairstyle.value = hairstyle
}

function setCurrentSkincolor(skincolor: AvatarAsset) {
  currentSkincolor.value = skincolor
}

function setCurrentCostume(costume: AvatarAsset) {
  currentCostume.value = costume
}

async function loadAvatarAssets() {
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await fetch('/api/avatar-assets', {
      credentials: 'include',
    })

    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`)
    }

    const assets = (await response.json()) as AvatarAsset[]
    skinAssets.value = assets.filter((asset) => asset.assetType === 'person')
    hairAssets.value = assets.filter((asset) => asset.assetType === 'hair')
    costumeAssets.value = assets.filter((asset) => asset.assetType === 'costume')

    currentSkincolor.value = skinAssets.value[0] ?? null
    currentHairstyle.value = hairAssets.value[0] ?? null
    currentCostume.value = costumeAssets.value[0] ?? null
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke laste avatarvalg'
  } finally {
    loading.value = false
  }
}

async function saveMembership() {
  if (!canSubmit.value) return

  saving.value = true
  saveError.value = ''

  try {
    const payload: UpdateMembershipRequest = {
      username: username.value.trim(),
      avatar: avatarSelection.value,
    }

    await put(`/api/pupil/classrooms/${classroomCode.value}/membership`, payload)
    await router.push('/join-classroom/waiting-classroom')
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : 'Kunne ikke lagre avatar og kallenavn'
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(async () => {
  if (!classroomCode.value) {
    errorMessage.value = 'Mangler klasseromskode for å kunne lagre karakteren.'
    loading.value = false
    return
  }

  await loadAvatarAssets()
})
</script>

<template>
  <main class="page">
    <section class="layout-card">
      <section class="selectors-panel">
        <div class="copy-panel">
        <h1>Lag din karakter</h1>
        <p class="description">
          Velg hudfarge, hårstil og antrekk for figuren din. Du kan også skrive inn kallenavnet du vil bruke i klasserommet.
        </p>

        <label class="username-field" for="classroom-username">
          <span>Kallenavn</span>
          <input
            id="classroom-username"
            v-model="username"
            type="text"
            maxlength="30"
            placeholder="Hva skal kallenavnet ditt være i dette klasserommet?"
          />
        </label>

        <p class="helper-text">Velg hvordan figuren din skal se ut og hvilket kallenavn du vil bruke i klasserommet.</p>
        <p v-if="saveError" class="state-message error">{{ saveError }}</p>
        </div>

        <p v-if="loading" class="state-message">Laster avatarvalg...</p>
        <p v-else-if="errorMessage" class="state-message error">{{ errorMessage }}</p>

        <div v-else class="picker-grid">
          <AvatarAssetsContainer
            title="Hudfarge"
            :assets="skinAssets"
            :selected-asset-name="currentSkincolor?.name ?? null"
            @select="setCurrentSkincolor"
          />
          <AvatarAssetsContainer
            title="Hårstil"
            :assets="hairAssets"
            :selected-asset-name="currentHairstyle?.name ?? null"
            @select="setCurrentHairstyle"
          />
          <AvatarAssetsContainer
            title="Antrekk"
            :assets="costumeAssets"
            :selected-asset-name="currentCostume?.name ?? null"
            @select="setCurrentCostume"
          />
        </div>
      </section>

      <aside class="preview-panel">
        <div class="avatar-preview">
          <img
            v-for="asset in previewLayers"
            :key="`${asset.assetType}-${asset.name}`"
            :src="getAssetImagePath(asset)"
            :alt="asset.name"
          />
        </div>
        <p class="preview-label">{{ username || "Du har ikke lagd et nytt brukernavn enda" }}</p>
        <div class="button-row">
          <button type="button" class="secondary-button" @click="goBack">Tilbake</button>
          <button type="button" class="primary-button" :disabled="!canSubmit" @click="saveMembership">
            {{ saving ? 'Lagrer...' : 'Bekreft' }}
          </button>
        </div>
      </aside>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, var(--beige-medium), var(--beige-light));
  padding: 2rem 1.25rem 3rem;
}

.layout-card {
  width: min(80rem, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(18rem, 24rem);
  gap: 1.25rem;
}

.selectors-panel,
.preview-panel {
  border-radius: 1.5rem;
  border: var(--border-wide) solid var(--brown-dark);
  background: rgba(255, 250, 241, 0.92);
  box-shadow: 0 1rem 2rem rgba(55, 32, 18, 0.12);
}

.selectors-panel {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 1.5rem;
}

.copy-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.eyebrow {
  margin: 0;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brown-dark);
}

.copy-panel h1 {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3.25rem);
  color: var(--brown-dark);
}

.description,
.helper-text,
.preview-label {
  margin: 0;
  color: var(--brown-dark);
}

.username-field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  font-weight: 700;
  color: var(--brown-dark);
}

.username-field input {
  width: 100%;
  box-sizing: border-box;
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  padding: 0.95rem 1rem;
  font: inherit;
  background: white;
}

.button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.secondary-button,
.primary-button {
  min-width: 10rem;
  border-radius: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
  padding: 0.85rem 1rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.secondary-button {
  background: var(--beige-light);
  color: var(--brown-dark);
}

.primary-button {
  background: var(--green-confirm);
  color: var(--brown-dark);
}

.primary-button:disabled,
.secondary-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 0.75rem;
  background: linear-gradient(180deg, #fef3dc, #f8e0b8);
  padding: 1.5rem 1rem;
  position: sticky;
  top: 1.25rem;
  height: fit-content;
}

.avatar-preview {
  position: relative;
  width: min(18rem, 100%);
  aspect-ratio: 1;
  border-radius: 100%;
  background: white;
  border: var(--border-thin) solid var(--brown-dark);
  overflow: hidden;
}

.avatar-preview img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.picker-grid {
  display: grid;
  gap: 1.25rem;
}

.state-message {
  margin: 0;
  text-align: center;
  font-weight: 600;
  color: var(--brown-dark);
}

.state-message.error {
  color: var(--red-danger);
}

@media (max-width: 900px) {
  .layout-card {
    grid-template-columns: 1fr;
  }

  .preview-panel {
    order: -1;
    position: static;
  }
}
</style>
