<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AvatarAssetsContainer from '@/components/pupil/AvatarAssetsContainer.vue'
import { patch } from '@/utils/apiUtils'

type AvatarAsset = {
  assetType: string
  name: string
  filetype: string
}

type UpdateAvatarRequest = {
  avatar: string
}

const router = useRouter()

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
const avatarSelection = computed(() => {
  if (!currentSkincolor.value || !currentHairstyle.value || !currentCostume.value) {
    return ''
  }

  return `${currentSkincolor.value.name}_${currentHairstyle.value.name}_${currentCostume.value.name}`
})
const canSubmit = computed(() => avatarSelection.value.length > 0 && !loading.value && !saving.value)

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

async function saveAvatar() {
  if (!canSubmit.value) return

  saving.value = true
  saveError.value = ''

  try {
    const payload: UpdateAvatarRequest = {
      avatar: avatarSelection.value,
    }

    await patch('/api/me', payload)
    await router.push('/pupil/profile')
  } catch (error) {
    saveError.value = error instanceof Error ? error.message : 'Kunne ikke lagre avatar'
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(async () => {
  await loadAvatarAssets()
})
</script>

<template>
  <main class="page">
    <section class="layout-card">
      <section class="selectors-panel">
        <div class="copy-panel">
          <h1>Bytt avatar</h1>
          <p class="description">
            Velg ny hudfarge, hårstil og antrekk for figuren din.
          </p>
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
        <div class="button-row">
          <button type="button" class="secondary-button" @click="goBack">Tilbake</button>
          <button type="button" class="primary-button" :disabled="!canSubmit" @click="saveAvatar">
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

.copy-panel h1 {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3.25rem);
  color: var(--brown-dark);
}

.description,
.preview-label {
  margin: 0;
  color: var(--brown-dark);
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
  color: #9f1d1d;
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
