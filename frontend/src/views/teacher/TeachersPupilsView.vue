<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import TeacherPupilsLeftPanel from '@/components/teacher/pupils/TeacherPupilsLeftPanel.vue'
import TeacherPupilsRightPanel from '@/components/teacher/pupils/TeacherPupilsRightPanel.vue'
import type { ActivePanel, Pupil, StopName } from '@/components/teacher/pupils/types'
import { del, get, patch, put } from '@/utils/apiUtils'

type AcceptedPupilResponse = {
  accountEmail: string
  legalName: string
  username: string
  avatarObjectKey: string | null
  totalXp: number
}

type PendingPupilResponse = {
  accountEmail: string
  legalName: string
  username: string
  avatarObjectKey: string | null
}

type ChapterNotebookResponse = {
  chapterId: number
  chapterName: string
  tips: string[]
  ownNotes: string
}

type UpdatePupilUsernameRequest = {
  username: string
}

const CHAPTER_ID_BY_STOP_NAME: Record<StopName, number> = {
  Nyhetskvartalet: 1,
  Fotografen: 2,
  Datasenteret: 3,
}

const route = useRoute()
const selectedPupilEmail = ref('')
const activePanel = ref<ActivePanel>('NONE')
const selectedStopName = ref<StopName | ''>('')
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const notebookLoading = ref(false)
const notebookError = ref('')
const selectedNotebookNote = ref('')
const selectedNotebookChapterName = ref('')
const savingUsername = ref(false)
const pupilPendingRemoval = ref<Pupil | null>(null)
const removingPupil = ref(false)

const stopNames: StopName[] = ['Nyhetskvartalet', 'Fotografen', 'Datasenteret']

const approvedPupils = ref<Pupil[]>([])
const pendingPupils = ref<Pupil[]>([])

const classroomCode = computed(() =>
  typeof route.params.classroomCode === 'string' ? route.params.classroomCode : '',
)

const allPupils = computed(() => [...approvedPupils.value, ...pendingPupils.value])

const selectedPupil = computed(
  () =>
    allPupils.value.find(
      (pupil) =>
        pupil.email === selectedPupilEmail.value && pupil.classroomCode === classroomCode.value,
    ) ?? null,
)

const selectedStopNote = computed(() => {
  if (!selectedStopName.value) return ''
  return selectedNotebookNote.value
})

const hasUnsavedUsernameChanges = computed(() => {
  return (
    selectedPupil.value?.status === 'APPROVED' &&
    editUsername.value !== selectedPupil.value.username
  )
})

const editUsername = ref('')

watch(
  selectedPupil,
  (pupil) => {
    editUsername.value = pupil?.username ?? ''
  },
  { immediate: true },
)

const selectPupil = (email: string): void => {
  if (email !== selectedPupilEmail.value && !confirmDiscardUsernameChanges()) {
    return
  }

  selectedPupilEmail.value = email
  activePanel.value = 'NONE'
  selectedStopName.value = ''
  notebookError.value = ''
  selectedNotebookNote.value = ''
  selectedNotebookChapterName.value = ''
}

const closeDetails = (): void => {
  selectedPupilEmail.value = ''
  activePanel.value = 'NONE'
  selectedStopName.value = ''
  notebookError.value = ''
  selectedNotebookNote.value = ''
  selectedNotebookChapterName.value = ''
}

const openNotes = (): void => {
  activePanel.value = activePanel.value === 'NOTES' ? 'NONE' : 'NOTES'
  selectedStopName.value = ''
  notebookError.value = ''
  selectedNotebookNote.value = ''
  selectedNotebookChapterName.value = ''
}

const selectStopNote = (stopName: StopName): void => {
  selectedStopName.value = stopName
}

function confirmDiscardUsernameChanges(): boolean {
  if (!hasUnsavedUsernameChanges.value) {
    return true
  }

  return window.confirm('Er du sikker på at du vil forlate uten å lagre endringene dine')
}

function handleBeforeUnload(event: BeforeUnloadEvent) {
  if (!hasUnsavedUsernameChanges.value) {
    return
  }

  event.preventDefault()
  event.returnValue = ''
}

function mapAcceptedPupil(response: AcceptedPupilResponse): Pupil {
  return {
    email: response.accountEmail,
    legalName: response.legalName,
    username: response.username,
    classroomCode: classroomCode.value,
    status: 'APPROVED',
    totalXp: response.totalXp,
    notes: [],
    mysterySuggestion: '',
    avatarUrl: response.avatarObjectKey ? `/api/${response.avatarObjectKey}` : null,
  }
}

function mapPendingPupil(response: PendingPupilResponse): Pupil {
  return {
    email: response.accountEmail,
    legalName: response.legalName,
    username: response.username,
    classroomCode: classroomCode.value,
    status: 'PENDING',
    totalXp: 0,
    notes: [],
    mysterySuggestion: '',
    avatarUrl: response.avatarObjectKey ? `/api/${response.avatarObjectKey}` : null,
  }
}

async function loadPupils() {
  if (!classroomCode.value) {
    error.value = 'Could not find classroom for this route'
    approvedPupils.value = []
    pendingPupils.value = []
    return
  }

  loading.value = true
  error.value = ''

  try {
    const [accepted, pending] = await Promise.all([
      get<AcceptedPupilResponse[]>(`/api/classrooms/${classroomCode.value}/pupils`),
      get<PendingPupilResponse[]>(`/api/classrooms/${classroomCode.value}/pupils/pending`),
    ])

    approvedPupils.value = accepted.map(mapAcceptedPupil)
    pendingPupils.value = pending.map(mapPendingPupil)
  } catch (fetchError) {
    approvedPupils.value = []
    pendingPupils.value = []
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load pupils'
  } finally {
    loading.value = false
  }
}

async function loadSelectedNotebook() {
  if (
    activePanel.value !== 'NOTES' ||
    !selectedPupil.value ||
    selectedPupil.value.status !== 'APPROVED' ||
    !selectedStopName.value ||
    !classroomCode.value
  ) {
    notebookLoading.value = false
    notebookError.value = ''
    selectedNotebookNote.value = ''
    selectedNotebookChapterName.value = ''
    return
  }

  notebookLoading.value = true
  notebookError.value = ''
  selectedNotebookNote.value = ''
  selectedNotebookChapterName.value = ''

  try {
    const chapterId = CHAPTER_ID_BY_STOP_NAME[selectedStopName.value]
    const response = await get<ChapterNotebookResponse>(
      `/api/teacher/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}/chapters/${chapterId}/notebook`,
    )
    selectedNotebookChapterName.value = response.chapterName
    selectedNotebookNote.value = response.ownNotes
  } catch (fetchError) {
    if (fetchError instanceof Error && fetchError.message.includes('409')) {
      selectedNotebookNote.value = ''
      notebookError.value = ''
    } else {
      notebookError.value =
        fetchError instanceof Error ? fetchError.message : 'Failed to load notebook'
    }
  } finally {
    notebookLoading.value = false
  }
}

const saveChanges = async (): Promise<void> => {
  if (!selectedPupil.value) return
  if (savingUsername.value) return

  const pupil = approvedPupils.value.find((item) => item.email === selectedPupil.value?.email)
  if (!pupil) return

  savingUsername.value = true

  try {
    actionError.value = ''
    const payload: UpdatePupilUsernameRequest = {
      username: editUsername.value.trim(),
    }

    const updatedPupil = await patch<AcceptedPupilResponse>(
      `/api/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}/username`,
      payload,
    )

    pupil.username = updatedPupil.username
    pupil.totalXp = updatedPupil.totalXp
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to save pupil changes'
    return
  } finally {
    savingUsername.value = false
  }

  closeDetails()
}

const removePupil = (): void => {
  if (!selectedPupil.value) return

  actionError.value = ''
  pupilPendingRemoval.value = selectedPupil.value
}

const closeRemovePupilPopup = (): void => {
  if (removingPupil.value) return
  pupilPendingRemoval.value = null
}

const confirmRemovePupil = async (): Promise<void> => {
  if (!pupilPendingRemoval.value || removingPupil.value) return

  const pupilToRemove = pupilPendingRemoval.value
  removingPupil.value = true

  try {
    actionError.value = ''
    await del(`/api/classrooms/${classroomCode.value}/pupils/${pupilToRemove.email}`)
    await loadPupils()
    if (selectedPupilEmail.value === pupilToRemove.email) {
      closeDetails()
    }
    pupilPendingRemoval.value = null
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to remove pupil'
  } finally {
    removingPupil.value = false
  }
}

const approvePendingPupil = async (): Promise<void> => {
  if (!selectedPupil.value) return

  try {
    actionError.value = ''
    await put(
      `/api/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}/accept`,
      {},
    )
    await loadPupils()
    closeDetails()
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to approve pupil'
  }
}

const rejectPendingPupil = async (): Promise<void> => {
  if (!selectedPupil.value) return

  const confirmed = window.confirm(
    `Er du sikker på at du vil avslå ${selectedPupil.value.legalName}?`,
  )

  if (!confirmed) return

  try {
    actionError.value = ''
    await del(`/api/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}`)
    await loadPupils()
    closeDetails()
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to reject pupil'
  }
}

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  await loadPupils()
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => {
  return confirmDiscardUsernameChanges()
})

watch(
  () => route.params.classroomCode,
  async () => {
    closeDetails()
    await loadPupils()
  },
)

watch([selectedPupilEmail, selectedStopName, activePanel], async () => {
  await loadSelectedNotebook()
})
</script>

<template>
  <main class="pupils-view">
    <p v-if="loading" class="state-message">Laster elever...</p>
    <p v-else-if="error" class="state-message error">{{ error }}</p>
    <p v-if="!loading && !error && actionError" class="state-message error">{{ actionError }}</p>

    <section class="pupils-layout">
      <TeacherPupilsLeftPanel
        v-if="!loading && !error"
        :approved-pupils="approvedPupils"
        :pending-pupils="pendingPupils"
        :selected-pupil-email="selectedPupilEmail"
        @select-pupil="selectPupil"
      />

      <TeacherPupilsRightPanel
        v-if="!loading && !error"
        :selected-pupil="selectedPupil"
        :edit-username="editUsername"
        :active-panel="activePanel"
        :stop-names="stopNames"
        :selected-stop-name="selectedStopName"
        :selected-chapter-name="selectedNotebookChapterName"
        :selected-stop-note="selectedStopNote"
        :notebook-loading="notebookLoading"
        :notebook-error="notebookError"
        @update:edit-username="editUsername = $event"
        @save-changes="saveChanges"
        @remove-pupil="removePupil"
        @open-notes="openNotes"
        @select-stop-note="selectStopNote"
        @approve-pending-pupil="approvePendingPupil"
        @reject-pending-pupil="rejectPendingPupil"
      />
    </section>

    <div
      v-if="pupilPendingRemoval"
      class="teacher-confirm-overlay"
      @click.self="closeRemovePupilPopup"
    >
      <section class="teacher-confirm-popup" role="dialog" aria-modal="true">
        <h2>Fjern elev?</h2>
        <p>
          Er du sikker på at du vil fjerne
          <strong>{{ pupilPendingRemoval.legalName }}</strong>
          fra klasserommet?
        </p>

        <div class="teacher-confirm-actions">
          <button
            type="button"
            class="teacher-confirm-button teacher-confirm-button-secondary cancel-remove-pupil"
            :disabled="removingPupil"
            @click="closeRemovePupilPopup"
          >
            Avbryt
          </button>
          <button
            type="button"
            class="teacher-confirm-button teacher-confirm-button-danger confirm-remove-pupil"
            :disabled="removingPupil"
            @click="confirmRemovePupil"
          >
            {{ removingPupil ? 'Fjerner...' : 'Bekreft' }}
          </button>
        </div>
      </section>
    </div>
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

@media (max-width: 768px) {
  .pupils-view {
    padding: 0.5rem;
  }
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

.state-message {
  margin: 0 0 1rem;
  color: var(--txt);
}

.state-message.error {
  color: #9f1d1d;
}

.teacher-confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgb(60 42 31 / 45%);
}

.teacher-confirm-popup {
  width: min(520px, calc(100vw - 32px));
  padding: 2rem;
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 24px;
  background: var(--beige-light);
  box-shadow: 0 16px 30px var(--brown-dark-20);
  text-align: center;
}

.teacher-confirm-popup h2 {
  margin: 0 0 0.75rem;
  font-size: 2rem;
  color: var(--txt);
}

.teacher-confirm-popup p {
  margin: 0;
  color: var(--txt);
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.35;
}

.teacher-confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.75rem;
}

.teacher-confirm-button {
  min-width: 140px;
  padding: 0.85rem 1.4rem;
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 18px;
  color: var(--txt);
  font-size: 1rem;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 12px var(--brown-dark-20);
}

.teacher-confirm-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.teacher-confirm-button-secondary {
  background: var(--beige-medium);
}

.teacher-confirm-button-danger {
  background: var(--red-danger);
}

.page-title {
  margin: 0 0 18px;
  font-size: 2.2rem;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .pupils-layout {
    grid-template-columns: 1fr;
  }
}
</style>
