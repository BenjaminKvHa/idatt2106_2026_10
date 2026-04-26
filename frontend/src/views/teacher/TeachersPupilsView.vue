<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TeacherPupilsLeftPanel from '@/components/teacher/pupils/TeacherPupilsLeftPanel.vue'
import TeacherPupilsRightPanel from '@/components/teacher/pupils/TeacherPupilsRightPanel.vue'
import type { ActivePanel, Pupil, StopName } from '@/components/teacher/pupils/types'
import { del, get, put } from '@/utils/apiUtils'

type AcceptedPupilResponse = {
  accountEmail: string
  legalName: string
  username: string
  avatarObjectKey: string | null
}

type PendingPupilResponse = {
  accountEmail: string
  legalName: string
}

type ChapterNotebookResponse = {
  chapterId: number
  tips: string[]
  ownNotes: string
}

const MOCK_PUPIL_DATA: Record<string, { totalXp: number; mysterySuggestion: string }> = {
  'demo.alice@example.com': {
    totalXp: 1240,
    mysterySuggestion: 'Lag et mysterium om falske nyheter på TikTok.',
  },
  'demo.bob@example.com': {
    totalXp: 860,
    mysterySuggestion: 'Et mysterium om en rar e-post fra banken.',
  },
  'test.elev@example.com': {
    totalXp: 540,
    mysterySuggestion: 'Noen har laget et KI-bilde av skolen.',
  },
}

const CHAPTER_ID_BY_STOP_NAME: Record<StopName, number> = {
  Nyhetskvartalet: 1,
  Fotografen: 2,
  Postkontoret: 3,
  Datasenteret: 4,
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

const stopNames: StopName[] = ['Nyhetskvartalet', 'Fotografen', 'Postkontoret', 'Datasenteret']

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

const editUsername = ref('')

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
  notebookError.value = ''
  selectedNotebookNote.value = ''
}

const closeDetails = (): void => {
  selectedPupilEmail.value = ''
  activePanel.value = 'NONE'
  selectedStopName.value = ''
  notebookError.value = ''
  selectedNotebookNote.value = ''
}

const openNotes = (): void => {
  activePanel.value = activePanel.value === 'NOTES' ? 'NONE' : 'NOTES'
  selectedStopName.value = ''
  notebookError.value = ''
  selectedNotebookNote.value = ''
}

const openMysterySuggestion = (): void => {
  activePanel.value = activePanel.value === 'MYSTERY' ? 'NONE' : 'MYSTERY'
  selectedStopName.value = ''
}

const selectStopNote = (stopName: StopName): void => {
  selectedStopName.value = stopName
}

function getMockData(email: string) {
  return MOCK_PUPIL_DATA[email] ?? { totalXp: 0, mysterySuggestion: '' }
}

function mapAcceptedPupil(response: AcceptedPupilResponse): Pupil {
  const mockData = getMockData(response.accountEmail)

  return {
    email: response.accountEmail,
    legalName: response.legalName,
    username: response.username,
    classroomCode: classroomCode.value,
    status: 'APPROVED',
    totalXp: mockData.totalXp,
    notes: [],
    mysterySuggestion: mockData.mysterySuggestion,
    avatarUrl: response.avatarObjectKey ? `/api/${response.avatarObjectKey}` : null,
  }
}

function mapPendingPupil(response: PendingPupilResponse): Pupil {
  return {
    email: response.accountEmail,
    legalName: response.legalName,
    username: '',
    classroomCode: classroomCode.value,
    status: 'PENDING',
    totalXp: 0,
    notes: [],
    mysterySuggestion: '',
    avatarUrl: null,
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
    activePanel.value !== 'NOTES'
    || !selectedPupil.value
    || selectedPupil.value.status !== 'APPROVED'
    || !selectedStopName.value
    || !classroomCode.value
  ) {
    notebookLoading.value = false
    notebookError.value = ''
    selectedNotebookNote.value = ''
    return
  }

  notebookLoading.value = true
  notebookError.value = ''
  selectedNotebookNote.value = ''

  try {
    const chapterId = CHAPTER_ID_BY_STOP_NAME[selectedStopName.value]
    const response = await get<ChapterNotebookResponse>(
      `/api/teacher/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}/chapters/${chapterId}/notebook`,
    )
    selectedNotebookNote.value = response.ownNotes
  } catch (fetchError) {
    notebookError.value = fetchError instanceof Error ? fetchError.message : 'Failed to load notebook'
  } finally {
    notebookLoading.value = false
  }
}

const saveChanges = (): void => {
  if (!selectedPupil.value) return

  const pupil = approvedPupils.value.find((item) => item.email === selectedPupil.value?.email)
  if (!pupil) return

  pupil.username = editUsername.value.trim()

  closeDetails()
}

const removePupil = async (): Promise<void> => {
  if (!selectedPupil.value) return

  const confirmed = window.confirm(
    `Er du sikker på at du vil fjerne ${selectedPupil.value.legalName} fra klasserommet?`,
  )

  if (!confirmed) return

  try {
    actionError.value = ''
    await del(`/api/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}`)
    await loadPupils()
    closeDetails()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to remove pupil'
  }
}

const approvePendingPupil = async (): Promise<void> => {
  if (!selectedPupil.value) return

  try {
    actionError.value = ''
    await put(`/api/classrooms/${classroomCode.value}/pupils/${selectedPupil.value.email}/accept`, {})
    await loadPupils()
    closeDetails()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to approve pupil'
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
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to reject pupil'
  }
}

const resetTestData = (): void => {
  void loadPupils()
  closeDetails()
}

onMounted(async () => {
  await loadPupils()
})

watch(
  () => route.params.classroomCode,
  async () => {
    closeDetails()
    await loadPupils()
  },
)

watch(
  [selectedPupilEmail, selectedStopName, activePanel],
  async () => {
    await loadSelectedNotebook()
  },
)

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
        @reset-data="resetTestData"
      />

      <TeacherPupilsRightPanel
        v-if="!loading && !error"
        :selected-pupil="selectedPupil"
        :edit-username="editUsername"
        :active-panel="activePanel"
        :stop-names="stopNames"
        :selected-stop-name="selectedStopName"
        :selected-stop-note="selectedStopNote"
        :notebook-loading="notebookLoading"
        :notebook-error="notebookError"
        @update:edit-username="editUsername = $event"
        @save-changes="saveChanges"
        @open-mystery="openMysterySuggestion"
        @remove-pupil="removePupil"
        @open-notes="openNotes"
        @select-stop-note="selectStopNote"
        @approve-pending-pupil="approvePendingPupil"
        @reject-pending-pupil="rejectPendingPupil"
      />
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

.state-message {
  margin: 0 0 1rem;
  color: var(--txt);
}

.state-message.error {
  color: #9f1d1d;
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
