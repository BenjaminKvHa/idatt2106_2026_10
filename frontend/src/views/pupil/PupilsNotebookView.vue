<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { ChevronLeftIcon, ChevronRightIcon } from '@lucide/vue'
import bookImage from '@/assets/book.png'
import woodBackground from '@/assets/backgrounds/woodBackground.png'
import TopBar from '@/components/pupil/TopBar.vue'
import { get, put } from '@/utils/apiUtils'
import ConfirmModal from '@/components/ui/ConfirmModal.vue'
import pageFlipSound from '@/assets/sounds/shared/page-flip-sound.mp3'
import { useAudio } from '@/composables/useAudio'

const PAGE_FLIP_SOUND_RELATIVE_VOLUME = 1.4

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

const { playSound, stopSound } = useAudio()
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
const currentPageNumber = computed(() =>
  chapters.value.length === 0 ? 0 : currentChapterIndex.value + 1,
)
const totalPages = computed(() => chapters.value.length)
const hasUnsavedOwnNotes = computed(() => {
  if (!notebook.value) return false
  return ownNotesDraft.value !== notebook.value.ownNotes
})

const mockChapters: Chapter[] = [
  { chapterId: 1, name: 'Nyhetskvartalet' },
  { chapterId: 2, name: 'Fotografen' },
  { chapterId: 3, name: 'Datasenteret' },
]

let soundRef: HTMLAudioElement | null = null

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
    const chapterNotebook = await get<NotebookResponse>(
      getNotebookEndpoint(currentChapter.value.chapterId),
    )
    notebook.value = chapterNotebook
    ownNotesDraft.value = chapterNotebook.ownNotes
  } catch (fetchError) {
    loadError.value = fetchError instanceof Error ? fetchError.message : 'Failed to load notebook'
  } finally {
    loadingNotebook.value = false
  }
}

const confirmModalShow = ref(false)
const confirmModalMessage = ref('')
let confirmModalResolve: ((v: boolean) => void) | null = null

function showConfirmModal(message: string): Promise<boolean> {
  return new Promise((resolve) => {
    confirmModalMessage.value = message
    confirmModalShow.value = true
    confirmModalResolve = resolve
  })
}

function onConfirmModalConfirm() {
  confirmModalResolve?.(true)
  confirmModalResolve = null
  confirmModalShow.value = false
}

function onConfirmModalCancel() {
  confirmModalResolve?.(false)
  confirmModalResolve = null
  confirmModalShow.value = false
}

async function shouldStayOnCurrentChapter(): Promise<boolean> {
  if (!hasUnsavedOwnNotes.value) {
    return false
  }

  const proceed = await showConfirmModal(
    'Du har ulagrede notater. Vil du fortsette uten å lagre?',
  )
  return !proceed
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

    const updatedNotebook = await put<NotebookResponse>(
      getNotebookEndpoint(currentChapter.value.chapterId),
      payload,
    )
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
  if (await shouldStayOnCurrentChapter()) return

  soundRef = playSound(pageFlipSound, PAGE_FLIP_SOUND_RELATIVE_VOLUME, 0.15)

  currentChapterIndex.value -= 1
  await loadNotebookForCurrentChapter()
}

async function goToNextChapter() {
  if (!hasNextChapter.value) return
  if (await shouldStayOnCurrentChapter()) return

  soundRef = playSound(pageFlipSound, PAGE_FLIP_SOUND_RELATIVE_VOLUME, 0.15)

  currentChapterIndex.value += 1
  await loadNotebookForCurrentChapter()
}

onMounted(async () => {
  await loadChapters()
})

onBeforeUnmount(() => {
  if (soundRef) {
    stopSound(soundRef)
    soundRef = null
  }
})
</script>

<template>
  <div
    class="notebook-page-bg pupil-scene-screen"
    :style="{ backgroundImage: `url(${woodBackground})` }"
  >
    <TopBar title="Notatbok" />

    <main class="notebook-page pupil-scene-page">
      <section class="book-stage" aria-labelledby="chapter-title">
        <img class="book-image" :src="bookImage" alt="" aria-hidden="true" />

        <div class="book-content">
          <p v-if="loadingChapters || loadingNotebook" class="state-message pupil-state-message">
            Loading...
          </p>
          <p v-else-if="loadError" class="state-message pupil-state-message pupil-error">
            {{ loadError }}
          </p>

          <template v-else-if="notebook">
            <section
              class="book-page book-page-left"
              :class="{ 'book-page--has-previous': hasPreviousChapter }"
            >
              <button
                v-if="hasPreviousChapter"
                type="button"
                class="nav-button nav-button-back pupil-round-icon-button"
                aria-label="Forrige kapittel"
                @click="goToPreviousChapter"
                data-no-click-sound
              >
                <ChevronLeftIcon aria-hidden="true" />
                <span class="sr-only">Backward</span>
              </button>
              <span v-else class="nav-placeholder" aria-hidden="true"></span>

              <header class="chapter-header">
                <h1 id="chapter-title">{{ currentChapter?.name ?? 'Notatbok' }}</h1>
                <p class="page-number">Page {{ currentPageNumber }} / {{ totalPages }}</p>
              </header>

              <div class="page-section pupil-thin-scroll">
                <h2>Tips</h2>
                <ul class="tips-list">
                  <li v-for="tip in notebook.tips" :key="tip">{{ tip }}</li>
                </ul>
                <p v-if="notebook.tips.length === 0" class="empty-text">Ingen tips enda.</p>
              </div>
            </section>

            <section
              class="book-page book-page-right"
              :class="{ 'book-page--has-next': hasNextChapter }"
            >
              <button
                v-if="hasNextChapter"
                type="button"
                class="nav-button nav-button-forward pupil-round-icon-button"
                aria-label="Neste kapittel"
                @click="goToNextChapter"
                data-no-click-sound
              >
                <span class="sr-only">Forward</span>
                <ChevronRightIcon aria-hidden="true" />
              </button>
              <span v-else class="nav-placeholder" aria-hidden="true"></span>

              <div class="page-section notes-section pupil-thin-scroll">
                <h2>Egne notater</h2>
                <textarea
                  v-model="ownNotesDraft"
                  class="notes-input"
                  rows="8"
                  placeholder="Skriv dine egne notater her"
                ></textarea>

                <div class="notes-actions">
                  <button
                    type="button"
                    class="save-button"
                    :disabled="savingNotebook || !hasUnsavedOwnNotes"
                    @click="saveOwnNotes"
                  >
                    {{ savingNotebook ? 'Lagrer...' : 'Lagre' }}
                  </button>
                  <span v-if="saveStatus" class="save-status">{{ saveStatus }}</span>
                  <span v-if="saveError" class="save-error">{{ saveError }}</span>
                </div>
              </div>
            </section>
          </template>
        </div>
      </section>
    </main>
  </div>

  <ConfirmModal
    v-if="confirmModalShow"
    :message="confirmModalMessage"
    confirm-label="Fortsett uten å lagre"
    cancel-label="Avbryt"
    @confirm="onConfirmModalConfirm"
    @cancel="onConfirmModalCancel"
  />
</template>

<style scoped>
.notebook-page-bg {
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.notebook-page {
  background: transparent;
  padding: clamp(0.35rem, 1vw, 0.85rem);
}

.book-stage {
  position: relative;
  width: min(99vw, calc((100dvh - 5.4rem) * 1.5), 1700px);
  aspect-ratio: 3 / 2;
}

.book-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: brightness(0.9) saturate(0.96);
  pointer-events: none;
  user-select: none;
}

.book-content {
  position: absolute;
  inset: 14% 14.5% 12% 14.5%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  column-gap: 9%;
  min-height: 0;
}

.book-page {
  position: relative;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: clamp(3.4rem, 4.6vw, 4.7rem) clamp(0.55rem, 1.25vw, 1.15rem)
    clamp(0.8rem, 1.45vw, 1.35rem);
}

.book-page-right {
  padding-top: clamp(3.4rem, 4.6vw, 4.7rem);
  padding-bottom: clamp(0.8rem, 1.45vw, 1.35rem);
}

.chapter-header {
  padding-right: clamp(0.75rem, 1.6vw, 1.5rem);
}

.chapter-header h1 {
  margin: 0;
  color: var(--brown-dark);
  font-size: clamp(1.55rem, 3.2vw, 3.25rem);
  line-height: 1;
}

.page-section {
  flex: 1 1 auto;
  min-height: 0;
  margin-top: clamp(0.85rem, 1.35vw, 1.35rem);
  overflow-y: auto;
  padding-right: 0.25rem;
}

.page-section h2 {
  margin: 0 0 0.6rem;
  color: var(--brown-dark);
  font-size: clamp(1.1rem, 1.8vw, 1.65rem);
}

.tips-list {
  margin: 0;
  padding-left: 1.2rem;
  color: var(--brown-dark);
  font-size: clamp(0.85rem, 1.2vw, 1rem);
  font-weight: 600;
  line-height: 1.45;
}

.tips-list li + li {
  margin-top: 0.55rem;
}

.empty-text {
  margin: 0;
  color: var(--brown-dark-80);
  font-weight: 600;
}

.nav-button {
  position: absolute;
  top: clamp(0.25rem, 0.75vw, 0.75rem);
}

.nav-button-back {
  left: clamp(0.35rem, 0.9vw, 0.95rem);
}

.nav-button-forward {
  right: clamp(0.35rem, 0.9vw, 0.95rem);
}

.nav-placeholder {
  position: absolute;
  top: clamp(0.15rem, 0.55vw, 0.55rem);
  width: clamp(2.15rem, 3.7vw, 3rem);
  height: clamp(2.15rem, 3.7vw, 3rem);
}

.page-number {
  margin: 0.35rem 0 0;
  color: var(--brown-dark-80);
  font-weight: 800;
  line-height: 1.1;
}

.state-message {
  grid-column: 1 / -1;
  align-self: center;
  justify-self: center;
  width: min(28rem, 100%);
  margin: 0;
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  background: rgba(255, 248, 235, 0.92);
  color: var(--brown-dark);
  font-weight: 800;
  padding: 1rem;
}

.notes-section {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-top: 0;
}

.notes-input {
  width: 100%;
  flex: 1 1 auto;
  min-height: 0;
  resize: none;
  border: none;
  border-radius: 0;
  padding: 0;
  background: transparent;
  color: var(--brown-dark);
  font: inherit;
  font-size: clamp(0.85rem, 1.15vw, 1rem);
  line-height: 1.4;
  box-sizing: border-box;
}

.notes-input:focus {
  outline: none;
}

.notes-actions {
  flex: 0 0 auto;
  margin-top: auto;
  padding-top: clamp(0.65rem, 1vw, 1rem);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: clamp(0.55rem, 1vw, 0.9rem);
}

.save-button {
  padding: clamp(0.45rem, 0.8vw, 0.65rem) clamp(0.9rem, 1.5vw, 1.35rem);
  border-radius: 999px;
  border: var(--border-thin) solid var(--brown-dark);
  background: rgba(255, 248, 235, 0.9);
  color: var(--brown-dark);
  font-size: clamp(0.9rem, 1vw, 1.1rem);
  font-weight: 800;
  cursor: pointer;
}

.save-button:hover:not(:disabled) {
  background: var(--brown-dark);
  color: var(--beige-light);
}

.save-button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.save-status {
  font-size: 0.95rem;
  color: var(--brown-dark);
  font-weight: 700;
}

.save-error {
  font-size: 0.95rem;
  color: var(--red-danger);
  font-weight: 700;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 900px) {
  .book-stage {
    width: min(99vw, calc((100dvh - 5.2rem) * 1.5));
  }

  .book-content {
    inset: 14.5% 14.5% 12.5% 14.5%;
    column-gap: 8.5%;
  }

  .book-page {
    padding-top: clamp(2.6rem, 4.7vw, 3.5rem);
    padding-inline: clamp(0.4rem, 1.15vw, 0.85rem);
  }
}

@media (max-width: 640px) {
  .notebook-page {
    padding: 0.5rem;
  }

  .book-stage {
    width: min(64rem, 195vw);
    flex: 0 0 auto;
  }

  .book-content {
    inset: 15.5% 14.5% 13% 14.5%;
    column-gap: 8%;
  }

  .book-page {
    padding-top: clamp(2.1rem, 7vw, 2.7rem);
  }

  .chapter-header h1 {
    font-size: clamp(1.05rem, 5vw, 1.55rem);
  }

  .page-section h2 {
    font-size: clamp(0.9rem, 4vw, 1.15rem);
  }

  .tips-list,
  .notes-input {
    font-size: clamp(0.68rem, 3.2vw, 0.82rem);
  }

  .notes-actions {
    gap: 0.35rem;
    padding-top: 0.4rem;
  }

  .save-button {
    padding: 0.35rem 0.65rem;
  }

  .save-status,
  .save-error {
    font-size: 0.75rem;
  }
}
</style>