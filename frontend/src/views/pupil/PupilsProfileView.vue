<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import {
  clearAuthSessionStorage,
  del,
  get,
  post,
  put,
  type BackendAccountRole,
} from '@/utils/apiUtils'
import { useAudio } from '@/composables/useAudio'
import { clearMeCache } from '@/router'
import volume2Icon from '@/assets/icons/volume-2.svg'
import volumeXIcon from '@/assets/icons/volume-x.svg'

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

const router = useRouter()
const profile = ref<ProfileResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const actionError = ref<string | null>(null)
const leavingClassroom = ref(false)
const signingOut = ref(false)
const editingNickname = ref(false)

const { volume, isMuted, setVolume, toggleMute } = useAudio()
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

onMounted(async () => {
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
    clearMeCache()
    await router.push('/pupil')
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Could not leave classroom'
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
    clearMeCache()
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
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Could not update nickname'
  } finally {
    savingNickname.value = false
  }
}
</script>

<template>
  <TopBar title="Profil" />

  <main class="page pupil-page-center">
    <section class="profile-card pupil-card">
      <p v-if="loading">Loading profile...</p>
      <p v-else-if="error" class="pupil-error">{{ error }}</p>

      <div v-else-if="profile" class="profile-layout">
        <section class="avatar-panel pupil-panel">
          <img class="profile-img" :src="avatarUrl" alt="Profile avatar" />
          <p class="pupil-meta-text">{{ profile.legalName || 'No name' }} - {{ profile.email }}</p>
          <p class="pupil-meta-text">{{ profile.role }}</p>

          <RouterLink to="/pupil/profile/avatar">
            <button
              v-if="isInClassroom"
              class="pupil-button pupil-button-secondary"
              type="button"
            >
              Bytt avatar
            </button>
          </RouterLink>
        </section>

        <section class="settings-panel pupil-panel">
          <div v-if="isInClassroom" class="setting-card pupil-inner-card">
            <span class="pupil-label">Kallenavn for klasserom</span>
            <p>{{ profile.username ?? 'Ikke satt enda' }}</p>

            <div v-if="isInClassroom && editingNickname" class="nickname-editor">
              <input
                v-model="nicknameDraft"
                class="pupil-input"
                type="text"
                placeholder="Skriv nytt kallenavn"
              />

              <div class="nickname-actions">
                <button
                  class="pupil-button pupil-button-secondary"
                  type="button"
                  :disabled="savingNickname"
                  @click="cancelNicknameEdit"
                >
                  Avbryt
                </button>

                <button
                  class="pupil-button pupil-button-secondary"
                  type="button"
                  :disabled="savingNickname"
                  @click="saveNickname"
                >
                  {{ savingNickname ? 'Lagrer...' : 'Lagre kallenavn' }}
                </button>
              </div>
            </div>

            <button
              v-else-if="isInClassroom"
              class="pupil-button pupil-button-secondary"
              type="button"
              @click="startNicknameEdit"
            >
              Bytt kallenavn for klasserom
            </button>
          </div>

          <div v-if="isInClassroom" class="setting-card pupil-inner-card">
            <span class="pupil-label">Klasseromskode</span>
            <p>{{ profile.classroomCode }}</p>
          </div>

          <div class="setting-card pupil-inner-card">
            <label class="pupil-label" for="volume-slider">Volum</label>
            <div class="volume-row">
              <button type="button" class="mute-btn" :aria-label="isMuted ? 'Skru på lyd' : 'Demp lyd'" @click="toggleMute">
                <img :src="isMuted ? volumeXIcon : volume2Icon" alt="" aria-hidden="true" class="volume-icon" />
              </button>
              <input
                id="volume-slider"
                :value="volume"
                type="range"
                min="0"
                max="100"
                :disabled="isMuted"
                @input="setVolume(+($event.target as HTMLInputElement).value)"
              />
            </div>
            <p>{{ isMuted ? 'Dempet' : volume + '%' }}</p>
          </div>

          <p v-if="actionError" class="action-error pupil-error">{{ actionError }}</p>

          <div class="actions">
            <button
              v-if="isInClassroom"
              class="pupil-button pupil-button-danger"
              type="button"
              :disabled="leavingClassroom"
              @click="leaveClassroom"
            >
              {{ leavingClassroom ? 'Forlater...' : 'Forlat klasserom' }}
            </button>

            <button
              class="pupil-button pupil-button-danger"
              type="button"
              :disabled="signingOut"
              @click="signOut"
            >
              {{ signingOut ? 'Logger ut...' : 'Logg ut' }}
            </button>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.profile-card {
  width: min(64rem, 100%);
  padding: 1.25rem;
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(16rem, 22rem) minmax(0, 1fr);
  gap: 1.25rem;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.75rem;
}

.avatar-panel p,
.setting-card p {
  margin: 0;
}

.profile-img {
  width: min(100%, 14rem);
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 999px;
  background-color: var(--beige-light);
  border: var(--border-thin) solid var(--brown-dark);
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
}

input[type='range'] {
  width: 100%;
}

.nickname-editor {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.nickname-actions,
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
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
