<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import { clearAuthSessionStorage, del, get, post, put, type BackendAccountRole } from '@/utils/apiUtils'

type ProfileResponse = {
  email: string
  legalName: string
  role: BackendAccountRole
  username: string | null
  classroomCode: string | null
  avatarFileObjectId: number | null
}

type UpdateMeRequest = {
  username: string
}

const NETTDET_VOLUME_KEY = 'nettdet_volume'

const router = useRouter()
const profile = ref<ProfileResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const actionError = ref<string | null>(null)
const leavingClassroom = ref(false)
const signingOut = ref(false)
const volumeValue = ref(65)
const editingNickname = ref(false)
const nicknameDraft = ref('')
const savingNickname = ref(false)

const avatarUrl = computed(() =>
  profile.value?.avatarFileObjectId
    ? `/api/objects/${profile.value.avatarFileObjectId}`
    : '/api/avatar-assets/costume/detective.png',
)
const isInClassroom = computed(() => Boolean(profile.value?.classroomCode))

async function loadProfile() {
  loading.value = true
  error.value = null
  actionError.value = null

  try {
    profile.value = await get<ProfileResponse>('/api/me')
    nicknameDraft.value = profile.value.username ?? ''
  } catch (fetchError) {
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load profile'
  } finally {
    loading.value = false
  }
}

function loadSavedVolume() {
  if (typeof window === 'undefined') return

  const savedVolume = window.localStorage.getItem(NETTDET_VOLUME_KEY)
  if (!savedVolume) return

  const parsedVolume = Number(savedVolume)
  if (!Number.isNaN(parsedVolume) && parsedVolume >= 0 && parsedVolume <= 100) {
    volumeValue.value = parsedVolume
  }
}

function persistVolume() {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(NETTDET_VOLUME_KEY, String(volumeValue.value))
}

onMounted(async () => {
  loadSavedVolume()
  await loadProfile()
})

async function leaveClassroom() {
  if (!profile.value?.classroomCode || leavingClassroom.value) return

  const confirmed = window.confirm('Er du sikker på at du vil forlate klasserommet?')
  if (!confirmed) return

  leavingClassroom.value = true
  actionError.value = null

  try {
    await del(`/api/classrooms/${profile.value.classroomCode}/pupils/me`)
    await router.push('/pupil')
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Could not leave classroom'
  } finally {
    leavingClassroom.value = false
  }
}

async function signOut() {
  if (signingOut.value) return

  signingOut.value = true
  actionError.value = null

  try {
    await post('/api/auth/logout', {})
  } catch {
    // Clearing local auth state still signs the user out from the frontend.
  } finally {
    clearAuthSessionStorage()
    signingOut.value = false
    await router.push('/sign-in')
  }
}

function startNicknameEdit() {
  if (!profile.value?.classroomCode) return

  actionError.value = null
  editingNickname.value = true
  nicknameDraft.value = profile.value.username ?? ''
}

function cancelNicknameEdit() {
  editingNickname.value = false
  nicknameDraft.value = profile.value?.username ?? ''
}

async function saveNickname() {
  if (!profile.value?.classroomCode || savingNickname.value) return

  savingNickname.value = true
  actionError.value = null

  try {
    const payload: UpdateMeRequest = {
      username: nicknameDraft.value,
    }

    profile.value = await put<ProfileResponse>('/api/me', payload)
    nicknameDraft.value = profile.value.username ?? ''
    editingNickname.value = false
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Could not update nickname'
  } finally {
    savingNickname.value = false
  }
}
</script>

<template>
  <TopBar title="Profil" :profile-avatar-url="avatarUrl" />

  <main class="page">
    <section class="profile-card">
      <p v-if="loading">Loading profile...</p>
      <p v-else-if="error" class="error">{{ error }}</p>

      <div v-else-if="profile" class="profile-layout">
        <section class="avatar-panel">
          <img class="profile-img" :src="avatarUrl" alt="Profile avatar" />
          <p class="meta-text">{{ profile.legalName || "No name" }} - {{ profile.email }}</p>
          <p class="meta-text">{{ profile.role }}</p>
          <button v-if="isInClassroom" class="secondary-button" type="button" disabled>Bytt avatar</button>
        </section>

        <section class="settings-panel">
          <div v-if="isInClassroom" class="setting-card">
            <span class="setting-label">Kallenavn for klasserom</span>
            <p>{{ profile.username ?? 'Ikke satt enda' }}</p>

            <div v-if="isInClassroom && editingNickname" class="nickname-editor">
              <input v-model="nicknameDraft" class="text-input" type="text" placeholder="Skriv nytt kallenavn" />
              <div class="nickname-actions">
                <button class="secondary-button" type="button" :disabled="savingNickname" @click="cancelNicknameEdit">
                  Avbryt
                </button>
                <button class="secondary-button" type="button" :disabled="savingNickname" @click="saveNickname">
                  {{ savingNickname ? 'Lagrer...' : 'Lagre kallenavn' }}
                </button>
              </div>
            </div>

            <button
              v-else-if="isInClassroom"
              class="secondary-button"
              type="button"
              @click="startNicknameEdit"
            >
              Bytt kallenavn for klasserom
            </button>
          </div>

          <div v-if="isInClassroom" class="setting-card">
            <span class="setting-label">Klasseromskode</span>
            <p>{{ profile.classroomCode }}</p>
          </div>

          <div class="setting-card">
            <label class="setting-label" for="volume-slider">Volum</label>
            <input id="volume-slider" v-model.number="volumeValue" type="range" min="0" max="100" @input="persistVolume" />
            <p>{{ volumeValue }}%</p>
          </div>

          <p v-if="actionError" class="error action-error">{{ actionError }}</p>

          <div class="actions">
            <button
              v-if="isInClassroom"
              class="danger-button"
              type="button"
              :disabled="leavingClassroom"
              @click="leaveClassroom"
            >
              {{ leavingClassroom ? 'Forlater...' : 'Forlat klasserom' }}
            </button>
            <button class="danger-button" type="button" :disabled="signingOut" @click="signOut">
              {{ signingOut ? 'Logger ut...' : 'Logg ut' }}
            </button>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: calc(100vh - 4rem);
  display: flex;
  justify-content: center;
  padding: 1.5rem;
}

.profile-card {
  width: min(64rem, 100%);
  height: fit-content;
  padding: 1.25rem;
  border-radius: 1.25rem;
  border: var(--border-wide) solid var(--brown-dark);
  background: linear-gradient(180deg, var(--beige-light), var(--beige-medium));
  box-shadow: 0 1rem 2rem rgba(55, 32, 18, 0.15);
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) minmax(0, 1fr);
  gap: 1.25rem;
}

.avatar-panel,
.settings-panel {
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  background: rgba(255, 248, 235, 0.82);
  padding: 1.25rem;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.75rem;
}

.avatar-panel h1,
.avatar-panel p,
.setting-card p {
  margin: 0;
}

.profile-img {
  width: min(100%, 14rem);
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 999px;
  background-color: white;
  border: var(--border-thin) solid var(--brown-dark);
}

.meta-text {
  color: var(--brown-dark);
}

.settings-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.setting-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 1rem;
  border-radius: 0.9rem;
  background: var(--beige-light);
  border: 1px solid rgba(85, 50, 27, 0.22);
}

.setting-label {
  font-weight: 700;
  color: var(--brown-dark);
}

input[type='range'] {
  width: 100%;
}

.nickname-editor {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.nickname-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}

.text-input {
  width: 100%;
  box-sizing: border-box;
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 0.85rem;
  padding: 0.8rem 0.9rem;
  background: white;
  font: inherit;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.secondary-button,
.danger-button {
  border: var(--border-thin) solid var(--brown-dark);
  padding: 0.85rem 1rem;
  color: black;
  border-radius: 1rem;
  font-weight: 600;
}

.secondary-button {
  background-color: var(--beige-medium);
}

.danger-button {
  background-color: var(--red-danger);
}

.secondary-button:disabled,
.danger-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.error {
  color: #9f1d1d;
}

.action-error {
  margin: 0;
}

@media (max-width: 768px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
