<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import TopBar from '@/components/pupil/TopBar.vue'
import { get, put } from '@/utils/apiUtils'

type Chapter = {
  chapterId: number
  name: string
}

type NotebookResponse = {
  chapterId: number
  tips: string[]
  ownNotes: string
}

function getNotebookEndpoint(chapterId: number): string {
  return `/api/pupil/chapters/${chapterId}/notebook`
}

const chapters = ref<Chapter[]>([])
const currentChapterIndex = ref(0)
const notebook = ref<NotebookResponse | null>(null)
const ownNotesDraft = ref('')
const loadingChapters = ref(true)
const loadingNotebook = ref(false)
const savingNotebook = ref(false)
const loadError = ref<string | null>(null)
const saveError = ref<string | null>(null)
const saveStatus = ref<string | null>(null)

const currentChapter = computed(() => chapters.value[currentChapterIndex.value] ?? null)
const hasPreviousChapter = computed(() => currentChapterIndex.value > 0)
const hasNextChapter = computed(() => currentChapterIndex.value < chapters.value.length - 1)
const currentPageNumber = computed(() => (chapters.value.length === 0 ? 0 : currentChapterIndex.value + 1))
const totalPages = computed(() => chapters.value.length)
const hasUnsavedOwnNotes = computed(() => {
  if (!notebook.value) return false
  return ownNotesDraft.value !== notebook.value.ownNotes
})

const mockChapters: Chapter[] = [
  { chapterId: 1, name: 'Nyhetskvartalet' },
  { chapterId: 2, name: 'Fotografen' },
  { chapterId: 3, name: 'Postkontoret' },
  { chapterId: 4, name: 'Passordbanken' },
]

async function mockFetchChapters(): Promise<Chapter[]> {
  await new Promise((resolve) => setTimeout(resolve, 250))
  return [...mockChapters]
}

async function loadChapters() {
  loadingChapters.value = true
  loadError.value = null

  try {
    const fetchedChapters = await mockFetchChapters()
    chapters.value = fetchedChapters
    currentChapterIndex.value = 0
    await loadNotebookForCurrentChapter()
  } catch (fetchError) {
    loadError.value = fetchError instanceof Error ? fetchError.message : 'Failed to load chapters'
  } finally {
    loadingChapters.value = false
  }
}

async function loadNotebookForCurrentChapter() {
  if (!currentChapter.value) {
    notebook.value = null
    ownNotesDraft.value = ''
    return
  }

  loadingNotebook.value = true
  loadError.value = null
  saveStatus.value = null

  try {
    const chapterNotebook = await get<NotebookResponse>(getNotebookEndpoint(currentChapter.value.chapterId))
    notebook.value = chapterNotebook
    ownNotesDraft.value = chapterNotebook.ownNotes
  } catch (fetchError) {
    loadError.value = fetchError instanceof Error ? fetchError.message : 'Failed to load notebook'
  } finally {
    loadingNotebook.value = false
  }
}

function shouldStayOnCurrentChapter(): boolean {
  if (!hasUnsavedOwnNotes.value) {
    return false
  }

  return !window.confirm('You have unsaved notes. If you continue, your unsaved changes will be lost.')
}

async function saveOwnNotes() {
  if (!currentChapter.value || !notebook.value || savingNotebook.value) return

  savingNotebook.value = true
  saveError.value = null
  saveStatus.value = null

  try {
    const payload = {
      ownNotes: ownNotesDraft.value,
    }

    const updatedNotebook = await put<NotebookResponse>(getNotebookEndpoint(currentChapter.value.chapterId), payload)
    notebook.value = updatedNotebook
    ownNotesDraft.value = updatedNotebook.ownNotes
    saveStatus.value = 'Notes saved'
  } catch (err) {
    saveError.value = err instanceof Error ? err.message : 'Failed to save notes'
  } finally {
    savingNotebook.value = false
  }
}

async function goToPreviousChapter() {
  if (!hasPreviousChapter.value) return
  if (shouldStayOnCurrentChapter()) return

  currentChapterIndex.value -= 1
  await loadNotebookForCurrentChapter()
}

async function goToNextChapter() {
  if (!hasNextChapter.value) return
  if (shouldStayOnCurrentChapter()) return

  currentChapterIndex.value += 1
  await loadNotebookForCurrentChapter()
}

onMounted(async () => {
  await loadChapters()
})
</script>

<template>
  <TopBar />

  <main class="page pupil-page-center notebook-page">
    <section class="notebook-card pupil-card">
      <header class="chapter-header">
        <button
          v-if="hasPreviousChapter"
          type="button"
          class="pupil-button pupil-button-secondary nav-button"
          @click="goToPreviousChapter"
        >
          Backward
        </button>
        <span v-else class="nav-placeholder" aria-hidden="true"></span>

        <h1>{{ currentChapter?.name ?? 'Notebook' }}</h1>

        <button
          v-if="hasNextChapter"
          type="button"
          class="pupil-button pupil-button-secondary nav-button"
          @click="goToNextChapter"
        >
          Forward
        </button>
        <span v-else class="nav-placeholder" aria-hidden="true"></span>
      </header>

      <p class="page-number">Page {{ currentPageNumber }} / {{ totalPages }}</p>

      <p v-if="loadingChapters || loadingNotebook" class="pupil-state-message">Loading...</p>
      <p v-else-if="loadError" class="pupil-state-message pupil-error">{{ loadError }}</p>

      <div v-else-if="notebook" class="content-grid">
        <section class="panel pupil-panel">
          <h2>System Tips</h2>
          <ul>
            <li v-for="tip in notebook.tips" :key="tip">{{ tip }}</li>
          </ul>
        </section>

        <section class="panel pupil-panel">
          <h2>My Notes</h2>
          <textarea
            v-model="ownNotesDraft"
            class="pupil-textarea"
            rows="8"
            placeholder="Write your notes here"
          ></textarea>

          <div class="notes-actions">
            <button
              type="button"
              class="pupil-button pupil-button-secondary"
              :disabled="savingNotebook || !hasUnsavedOwnNotes"
              @click="saveOwnNotes"
            >
              {{ savingNotebook ? 'Saving...' : 'Save notes' }}
            </button>
            <span v-if="saveStatus" class="save-status">{{ saveStatus }}</span>
            <span v-if="saveError" class="save-error">{{ saveError }}</span>
          </div>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.notebook-page {
  margin-top: 2rem;
}

.notebook-card {
  width: min(56rem, 100%);
  padding: 1rem;
}

.chapter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.chapter-header h1 {
  margin: 0;
  text-align: center;
}

.nav-placeholder {
  width: 5.75rem;
  height: 2.1rem;
}

.page-number {
  margin: 0.5rem 0 0;
  text-align: center;
}

.content-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.panel h2 {
  margin-top: 0;
}

.panel ul {
  margin: 0;
  padding-left: 1rem;
}

.notes-actions {
  margin-top: 0.6rem;
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.save-status,
.save-error {
  font-size: 0.95rem;
}

.save-error {
  color: var(--red-danger);
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .chapter-header {
    flex-wrap: wrap;
    justify-content: center;
  }
}
</style>
