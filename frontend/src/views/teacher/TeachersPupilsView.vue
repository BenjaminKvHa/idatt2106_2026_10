<script setup lang="ts">
import { computed, ref, watch } from 'vue'

type PupilStatus = 'APPROVED' | 'PENDING'
type ActivePanel = 'NONE' | 'NOTES' | 'MYSTERY'
type StopName = 'Nyhetskvartalet' | 'Fotografen' | 'Postkontoret' | 'Datasenteret'

interface StopNote {
  stopName: StopName
  note: string
}

interface Pupil {
  email: string
  legalName: string
  username: string
  classroomCode: string
  status: PupilStatus
  level: number
  progression: number
  notes: StopNote[]
  mysterySuggestion: string
  avatarUrl?: string | null
}

const STORAGE_KEY = 'teacher-pupils-test-data'

const classroomCode = ref('TESTAA')
const selectedPupilEmail = ref('')
const activePanel = ref<ActivePanel>('NONE')
const selectedStopName = ref<StopName | ''>('')

const stopNames: StopName[] = ['Nyhetskvartalet', 'Fotografen', 'Postkontoret', 'Datasenteret']

const defaultPupils: Pupil[] = [
  {
    email: 'demo.alice@example.com',
    legalName: 'Alice',
    username: 'alice',
    classroomCode: 'TESTAA',
    status: 'APPROVED',
    level: 3,
    progression: 85,
    avatarUrl: '/api/objects/avatar-images/demo_alice.png',
    mysterySuggestion: 'Lag et mysterium om falske nyheter på TikTok.',
    notes: [],
  },
  {
    email: 'demo.bob@example.com',
    legalName: 'Bob',
    username: 'bob',
    classroomCode: 'TESTAA',
    status: 'APPROVED',
    level: 2,
    progression: 62,
    avatarUrl: '/api/objects/avatar-images/demo_bob.png',
    mysterySuggestion: 'Et mysterium om en rar e-post fra banken.',
    notes: [],
  },
  {
    email: 'test.elev@example.com',
    legalName: 'Emma',
    username: 'emma',
    classroomCode: 'TESTAA',
    status: 'APPROVED',
    level: 2,
    progression: 42,
    avatarUrl: null,
    mysterySuggestion: 'Noen har laget et KI-bilde av skolen.',
    notes: [],
  },
  {
    email: 'demo.pending.one@example.com',
    legalName: 'Henrik',
    username: 'henrik1',
    classroomCode: 'TESTAA',
    status: 'PENDING',
    level: 1,
    progression: 0,
    avatarUrl: null,
    mysterySuggestion: '',
    notes: [],
  },
  {
    email: 'demo.pending.two@example.com',
    legalName: 'Sara',
    username: 'sara2',
    classroomCode: 'TESTAA',
    status: 'PENDING',
    level: 1,
    progression: 0,
    avatarUrl: null,
    mysterySuggestion: '',
    notes: [],
  },
]

const loadPupils = (): Pupil[] => {
  const savedPupils = localStorage.getItem(STORAGE_KEY)

  if (!savedPupils) return defaultPupils

  try {
    return JSON.parse(savedPupils) as Pupil[]
  } catch {
    return defaultPupils
  }
}

const allPupils = ref<Pupil[]>(loadPupils())

watch(
  allPupils,
  (newPupils) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newPupils))
  },
  { deep: true },
)

const approvedPupils = computed(() =>
  allPupils.value.filter(
    (pupil) => pupil.classroomCode === classroomCode.value && pupil.status === 'APPROVED',
  ),
)

const pendingPupils = computed(() =>
  allPupils.value.filter(
    (pupil) => pupil.classroomCode === classroomCode.value && pupil.status === 'PENDING',
  ),
)

const selectedPupil = computed(
  () =>
    allPupils.value.find(
      (pupil) =>
        pupil.email === selectedPupilEmail.value && pupil.classroomCode === classroomCode.value,
    ) ?? null,
)

const selectedStopNote = computed(() => {
  if (!selectedPupil.value || !selectedStopName.value) return ''

  return (
    selectedPupil.value.notes.find((note) => note.stopName === selectedStopName.value)?.note ?? ''
  )
})

const editUsername = ref('')

const progressionPercentage = computed(() => {
  if (!selectedPupil.value) return 0
  return Math.min(Math.max(selectedPupil.value.progression, 0), 100)
})

watch(
  selectedPupil,
  (pupil) => {
    editUsername.value = pupil?.username ?? ''
  },
  { immediate: true },
)

const selectPupil = (email: string): void => {
  selectedPupilEmail.value = email
  activePanel.value = 'NONE'
  selectedStopName.value = ''
}

const closeDetails = (): void => {
  selectedPupilEmail.value = ''
  activePanel.value = 'NONE'
  selectedStopName.value = ''
}

const openNotes = (): void => {
  activePanel.value = activePanel.value === 'NOTES' ? 'NONE' : 'NOTES'
  selectedStopName.value = ''
}

const openMysterySuggestion = (): void => {
  activePanel.value = activePanel.value === 'MYSTERY' ? 'NONE' : 'MYSTERY'
  selectedStopName.value = ''
}

const selectStopNote = (stopName: StopName): void => {
  selectedStopName.value = stopName
}

const saveChanges = (): void => {
  if (!selectedPupil.value) return

  const pupil = allPupils.value.find((item) => item.email === selectedPupil.value?.email)
  if (!pupil) return

  pupil.username = editUsername.value.trim()

  closeDetails()
}

const removePupil = (): void => {
  if (!selectedPupil.value) return

  const confirmed = window.confirm(
    `Er du sikker på at du vil fjerne ${selectedPupil.value.legalName} fra klasserommet?`,
  )

  if (!confirmed) return

  allPupils.value = allPupils.value.filter((pupil) => pupil.email !== selectedPupil.value?.email)

  closeDetails()
}

const approvePendingPupil = (): void => {
  if (!selectedPupil.value) return

  selectedPupil.value.status = 'APPROVED'
  selectedPupil.value.level = 1
  selectedPupil.value.progression = 0
  selectedPupil.value.notes = []

  closeDetails()
}

const rejectPendingPupil = (): void => {
  if (!selectedPupil.value) return

  const confirmed = window.confirm(
    `Er du sikker på at du vil avslå ${selectedPupil.value.legalName}?`,
  )

  if (!confirmed) return

  allPupils.value = allPupils.value.filter((pupil) => pupil.email !== selectedPupil.value?.email)

  closeDetails()
}

const resetTestData = (): void => {
  localStorage.removeItem(STORAGE_KEY)
  allPupils.value = [...defaultPupils]
  closeDetails()
}

const getInitials = (name: string): string => {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join('')
}
</script>

<template>
  <main class="pupils-view">
    <section class="pupils-layout">
      <aside class="pupil-list-panel">
        <h1 class="page-title">Elever</h1>

        <div class="list-section">
          <button
            v-for="pupil in approvedPupils"
            :key="pupil.email"
            class="pupil-row"
            :class="{ active: pupil.email === selectedPupilEmail }"
            type="button"
            @click="selectPupil(pupil.email)"
          >
            <span class="pupil-icon">
              <img
                v-if="pupil.avatarUrl"
                :src="pupil.avatarUrl"
                :alt="pupil.legalName"
                class="pupil-icon-image"
              />
              <span v-else>{{ getInitials(pupil.legalName) }}</span>
            </span>

            <span class="pupil-text">
              <span class="pupil-name inika">{{ pupil.legalName }}</span>
              <span class="pupil-username inika">@{{ pupil.username }}</span>
            </span>
          </button>
        </div>

        <div v-if="pendingPupils.length > 0" class="pending-section">
          <h2 class="pending-title">Venter på godkjenning:</h2>

          <button
            v-for="pupil in pendingPupils"
            :key="pupil.email"
            class="pupil-row pending-row"
            :class="{ active: pupil.email === selectedPupilEmail }"
            type="button"
            @click="selectPupil(pupil.email)"
          >
            <span class="pupil-icon">
              <img
                v-if="pupil.avatarUrl"
                :src="pupil.avatarUrl"
                :alt="pupil.legalName"
                class="pupil-icon-image"
              />
              <span v-else>{{ getInitials(pupil.legalName) }}</span>
            </span>

            <span class="pupil-text">
              <span class="pupil-name pending-name">{{ pupil.legalName }}</span>
              <span class="pupil-username pending-username">@{{ pupil.username }}</span>
            </span>
          </button>
        </div>

        <button class="reset-btn" type="button" @click="resetTestData">Nullstill testdata</button>
      </aside>

      <section class="details-panel">
        <div v-if="selectedPupil" class="details-card">
          <div class="profile-summary-card">
            <div class="avatar-area">
              <div v-if="selectedPupil.avatarUrl" class="avatar-image-wrapper">
                <img
                  :src="selectedPupil.avatarUrl"
                  :alt="selectedPupil.legalName"
                  class="avatar-image"
                />
              </div>

              <div v-else class="avatar-fallback">
                {{ getInitials(selectedPupil.legalName) }}
              </div>

              <div class="profile-text">
                <p class="profile-name inika">{{ selectedPupil.legalName }}</p>
                <p class="username-display inika">@{{ selectedPupil.username }}</p>
              </div>

              <div class="username-edit">
                <label class="field-label inika" for="username-input"> Brukernavn </label>

                <input
                  id="username-input inter"
                  v-model="editUsername"
                  type="text"
                  class="small-input"
                  placeholder="Brukernavn"
                />
              </div>
            </div>

            <div class="progress-area">
              <div class="stat-card">
                <div class="stat-item">
                  <span class="stat-label inika">Nivå</span>
                  <strong class="stat-value inika">{{ selectedPupil.level }}</strong>
                </div>
              </div>

              <div class="progress-section">
                <div class="progress-top inika">
                  <span class="inika">Progresjon</span>
                  <span>{{ progressionPercentage }}%</span>
                </div>

                <div class="progress-bar inika">
                  <div
                    class="progress-fill inika"
                    :style="{ width: `${progressionPercentage}%` }"
                  />
                </div>
              </div>
            </div>
          </div>

          <div v-if="selectedPupil.status === 'APPROVED'" class="button-grid">
            <button class="main-btn primary-btn inika" type="button" @click="saveChanges">
              Lagre endringer
            </button>

            <button
              class="main-btn secondary-btn inika"
              type="button"
              @click="openMysterySuggestion"
            >
              Ukens mysterium, forslag
            </button>

            <button class="danger-btn inika" type="button" @click="removePupil">
              Fjern fra klasserom
            </button>

            <button class="main-btn secondary-btn inika" type="button" @click="openNotes">
              Elev-notater
            </button>
          </div>

          <div
            v-if="selectedPupil.status === 'APPROVED' && activePanel === 'MYSTERY'"
            class="extra-fields"
          >
            <h2>Forslag til ukens mysterium</h2>

            <div class="read-only-box inter">
              {{ selectedPupil.mysterySuggestion || 'Eleven har ikke sendt inn forslag enda.' }}
            </div>
          </div>

          <div
            v-if="selectedPupil.status === 'APPROVED' && activePanel === 'NOTES'"
            class="extra-fields inika"
          >
            <h2>Elev-notater</h2>

            <div class="stop-button-grid">
              <button
                v-for="stopName in stopNames"
                :key="stopName"
                class="stop-note-btn inika"
                :class="{ active: selectedStopName === stopName }"
                type="button"
                @click="selectStopNote(stopName)"
              >
                {{ stopName }}
              </button>
            </div>

            <div v-if="selectedStopName" class="read-only-box inter">
              <h3>{{ selectedStopName }}</h3>
              <p>
                {{ selectedStopNote || 'Eleven har ikke skrevet inn noe.' }}
              </p>
            </div>
          </div>

          <div v-else-if="selectedPupil.status === 'PENDING'" class="pending-actions-inside">
            <p class="pending-message inika">
              {{ selectedPupil.legalName }} venter på godkjenning for å bli med i klasserommet.
            </p>

            <button class="main-btn primary-btn inika" type="button" @click="approvePendingPupil">
              Godkjenn elev
            </button>

            <button class="danger-btn inika" type="button" @click="rejectPendingPupil">
              Avslå forespørsel
            </button>
          </div>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.pupils-view,
.pupils-view * {
  box-sizing: border-box;
  color: var(--txt);
}

.pupils-view {
  width: 100%;
  min-height: calc(100vh - 120px);
  padding: 24px;
}

.pupils-view h1,
.pupils-view h2,
.pupils-view h3,
.pupils-view h4,
.pupils-view h5,
.pupils-view h6 {
  font-family: 'Inika', serif;
}

.pupils-view :where(p, span, button, input, textarea, label, strong, div) {
  font-family: 'Inter', sans-serif;
}

.pupils-view p {
  display: flex;
  flex-direction: column;
}

.pupils-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 34px;
  align-items: start;
}

.page-title {
  margin: 0 0 18px;
  font-size: 2.2rem;
  font-weight: 700;
}

.list-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pupil-row {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}

.pupil-row:hover {
  background: var(--obscure);
  transform: translateX(2px);
}

.pupil-row.active {
  background: var(--accent);
}

.pupil-row.active,
.pupil-row.active * {
  color: var(--txt-sec);
}

.pupil-icon {
  width: 38px;
  min-width: 38px;
  height: 38px;
  border: 3px solid currentColor;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.85rem;
  font-weight: 700;
  overflow: hidden;
  background: var(--background);
}

.pupil-icon-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pupil-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.1;
}

.pupil-name {
  font-size: 1.05rem;
  font-weight: 700;
}

.pupil-username {
  font-size: 0.78rem;
  opacity: 0.72;
}

.pending-section {
  margin-top: 26px;
}

.pending-title {
  margin: 0 0 10px;
  font-size: 1.1rem;
  font-weight: 700;
}

.pending-name,
.pending-username {
  color: var(--warning);
}

.pending-row.active .pending-name,
.pending-row.active .pending-username {
  color: var(--txt-sec);
}

.reset-btn {
  margin-top: 24px;
  border: 3px solid var(--accent);
  border-radius: 999px;
  background: var(--background);
  padding: 9px 14px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
}

.details-card {
  width: min(820px, 100%);
  border: 7px solid var(--accent);
  border-radius: 38px;
  padding: 28px;
}

.profile-summary-card {
  border: 5px solid var(--accent);
  border-radius: 28px;
  padding: 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  align-items: center;
  background: var(--background);
}

.avatar-area {
  display: grid;
  grid-template-columns: 120px 1fr;
  column-gap: 18px;
  row-gap: 10px;
  align-items: center;
}

.avatar-image-wrapper,
.avatar-fallback {
  width: 120px;
  height: 120px;
  grid-row: span 2;
  border-radius: 50%;
  overflow: hidden;
  background: var(--background-sec);
  display: grid;
  place-items: center;
  font-size: 2.6rem;
  font-weight: 700;
  border: 4px solid var(--accent);
  box-shadow: inset 0 0 0 4px var(--background);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-text {
  align-self: end;
}

.profile-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
}

.username-display {
  margin: 4px 0 0;
  font-size: 0.95rem;
  font-weight: 700;
  opacity: 0.72;
}

.username-edit {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.field-label {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
}

.small-input {
  width: 100%;
  border: 3px solid var(--background-sec);
  border-radius: 12px;
  background: var(--background);
  padding: 9px 12px;
  font-size: 1rem;
}

.progress-area {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.stat-card {
  display: grid;
  place-items: center;
  width: 100%;
  max-width: 180px;
  background: var(--background);
  border: 3px solid var(--background-sec);
  border-radius: 18px;
  padding: 14px 18px;
  margin: 0 auto;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.95rem;
  font-weight: 700;
  opacity: 0.75;
}

.stat-value {
  display: block;
  margin-top: 4px;
  font-size: 1.8rem;
}

.progress-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 1.05rem;
  font-weight: 700;
}

.progress-bar {
  height: 16px;
  background: var(--background-sec);
  border: 3px solid var(--accent);
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent);
  border-radius: 999px;
  transition: width 0.25s ease;
}

.button-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
  margin-top: 28px;
}

.main-btn,
.danger-btn,
.stop-note-btn {
  border: 4px solid var(--accent);
  border-radius: 999px;
  padding: 13px 18px;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.primary-btn {
  background: var(--accent);
}

.primary-btn,
.primary-btn * {
  color: var(--txt-sec);
}

.secondary-btn,
.stop-note-btn {
  background: var(--background);
}

.danger-btn {
  background: var(--warning);
}

.danger-btn,
.danger-btn * {
  color: var(--txt-contrast);
}

.main-btn:hover,
.danger-btn:hover,
.stop-note-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 0 var(--accent);
}

.extra-fields {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.extra-fields h2 {
  margin: 0;
  font-size: 1.35rem;
}

.read-only-box {
  width: 100%;
  min-height: 90px;
  border: 3px solid var(--background-sec);
  border-radius: 14px;
  background: var(--background);
  padding: 14px 16px;
  font-size: 1rem;
}

.read-only-box h3 {
  margin: 0 0 8px;
  font-size: 1.15rem;
}

.read-only-box p {
  margin: 0;
  line-height: 1.35;
}

.stop-button-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stop-note-btn.active {
  background: var(--accent);
  color: var(--txt-sec);
}

.pending-actions-inside {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  margin-top: 24px;
}

.pending-message {
  width: 100%;
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--warning);
}

@media (max-width: 1100px) {
  .pupils-layout {
    grid-template-columns: 1fr;
  }

  .details-card {
    width: 100%;
  }

  .profile-summary-card {
    grid-template-columns: 1fr;
  }

  .button-grid,
  .stop-button-grid {
    grid-template-columns: 1fr;
  }
}
</style>
