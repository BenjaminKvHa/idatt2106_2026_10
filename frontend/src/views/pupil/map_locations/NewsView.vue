<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import TaskGuide from '@/components/TaskGuide.vue'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  NewsVerificationRequest,
  NewsVerificationResponse,
  TaskDifficulty,
  ValidationResult,
} from '@/types/chapter'

const router = useRouter()

const NYHETSKVARTALET_CHAPTER_ID = 1
const DIFFICULTIES: TaskDifficulty[] = ['EASY', 'MEDIUM', 'HARD']

const showHelpPopup = ref(false)
const showHintPopup = ref(false)
const showGuidePopup = ref(true)

const stepIndex = ref(0)
const currentTask = ref<NewsVerificationResponse | null>(null)
const imageUrls = ref<string[]>([])
const selectedFileId = ref<number | null>(null)
const lastResult = ref<ValidationResult | null>(null)
const runCorrect = ref(0)
const runAttempts = ref(0)
const runDone = ref(false)
const loading = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

const difficultyLabel = computed(() => {
  const labels: Record<TaskDifficulty, string> = {
    EASY: 'Lett',
    MEDIUM: 'Middels',
    HARD: 'Vanskelig',
  }
  return labels[DIFFICULTIES[stepIndex.value]!]
})

function revokeImageUrls() {
  for (const url of imageUrls.value) URL.revokeObjectURL(url)
  imageUrls.value = []
}

async function loadTask() {
  loading.value = true
  error.value = null
  revokeImageUrls()
  currentTask.value = null
  try {
    const difficulty = DIFFICULTIES[stepIndex.value]!
    const task = await get<NewsVerificationResponse>(
      `/api/chapters/${NYHETSKVARTALET_CHAPTER_ID}/nyhetskvartalet?difficulty=${difficulty}`,
    )
    currentTask.value = task
    const blobs = await Promise.all(
      task.fileIds.map((id) => getBlob(`/api/objects/${id}`)),
    )
    imageUrls.value = blobs.map((b) => URL.createObjectURL(b))
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke laste oppgave'
  } finally {
    loading.value = false
  }
}

async function choose(fileId: number) {
  if (!currentTask.value || lastResult.value || submitting.value) return
  submitting.value = true
  selectedFileId.value = fileId
  error.value = null
  try {
    const body: NewsVerificationRequest = { selectedFileId: fileId }
    const result = await post<ValidationResult>(
      `/api/chapters/nyhetskvartalet/tasks/${currentTask.value.taskId}/validate`,
      body,
    )
    lastResult.value = result
    runAttempts.value++
    if (result.correct) runCorrect.value++
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke sende svar'
    selectedFileId.value = null
  } finally {
    submitting.value = false
  }
}

function cardClass(fileId: number) {
  if (selectedFileId.value !== fileId) return ''
  if (!lastResult.value) return 'selected'
  return lastResult.value.correct ? 'selected correct' : 'selected wrong'
}

async function next() {
  if (!lastResult.value) return
  if (lastResult.value.runCompleted) {
    runDone.value = true
    return
  }
  stepIndex.value++
  selectedFileId.value = null
  lastResult.value = null
  await loadTask()
}

function finish() {
  revokeImageUrls()
  router.push('/pupil/classroom/map')
}

onMounted(loadTask)
onBeforeUnmount(revokeImageUrls)
</script>

<template>
  <TopBar
    title="Nyhetskvartalet"
    @help="showHelpPopup = true"
    @hint="showHintPopup = true"
  />

  <div class="page">
    <div class="center-container">
      <div v-if="runDone" class="end-screen">
        <h2>Ferdig!</h2>
        <p>Du fikk {{ runCorrect }} av {{ runAttempts }} riktig.</p>
        <button class="primary-btn" @click="finish">Til kartet</button>
      </div>

      <template v-else-if="currentTask">
        <h2>Nivå {{ stepIndex + 1 }} — {{ difficultyLabel }}</h2>
        <div class="task-wrapper">
          <p>{{ currentTask.taskDescription }}</p>
        </div>

        <div class="picture-wrapper">
          <div
            v-for="(fileId, idx) in currentTask.fileIds"
            :key="fileId"
            class="picture-card"
            :class="cardClass(fileId)"
          >
            <div class="picture-placeholder">
              <img
                v-if="imageUrls[idx]"
                :src="imageUrls[idx]"
                :alt="`Bilde ${Number(idx) + 1}`"
                class="picture"
              />
              <span v-else>Laster…</span>
            </div>

            <button
              class="fake-button"
              :disabled="!!lastResult || submitting"
              @click="choose(fileId)"
            >
              Dette er den falske
            </button>
          </div>
        </div>

        <div v-if="lastResult" class="answer-feedback">
          <p>
            <strong>{{ lastResult.correct ? 'Riktig!' : 'Feil.' }}</strong>
          </p>
          <p>{{ lastResult.feedback }}</p>
          <button class="primary-btn" @click="next">
            {{ stepIndex === DIFFICULTIES.length - 1 ? 'Se resultat' : 'Neste nivå →' }}
          </button>
        </div>
      </template>

      <p v-else-if="loading">Laster oppgave…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
    </div>
  </div>

  <TaskGuide v-if="showGuidePopup" @close="showGuidePopup = false">
    <h2>Velkommen til nyhetskvartalet!</h2>
    <p>I nyhetskvartalet vil du få oppgaver som handler om å skille ekte avisforsider og uekte avisforsider.
      I hver oppgave vil du få én ekte avisforside, og en falsk, og du skal velge hvilken en av dem du tror er falsk.
      Er du klar for å løse oppgaver?</p>
  </TaskGuide>

  <div v-if="showHelpPopup" class="popup-overlay" @click="showHelpPopup = false">
    <div class="hint-popup" @click.stop>
      <h2 class="popup-title">Trenger du hjelp?</h2>
      <div class="hint-text">
        <p>I Nyhetskvartalet skal du undersøke informasjon og finne ut hva som er ekte og falskt.</p>
        <ul>
          <li>Les oppgaven nøye før du svarer</li>
          <li>Sammenlign bildene og se etter rare detaljer</li>
          <li>Velg det alternativet du mener er falskt</li>
        </ul>
      </div>
      <div class="popup-actions">
        <button class="popup-btn popup-btn-play" @click="showHelpPopup = false">Ok</button>
      </div>
    </div>
  </div>

  <div v-if="showHintPopup" class="popup-overlay" @click="showHintPopup = false">
    <div class="hint-popup" @click.stop>
      <h2 class="popup-title">Tips 💡</h2>
      <div class="hint-text">
        <p>Tenk som en detektiv.</p>
        <ul>
          <li>Se etter detaljer som ikke passer inn</li>
          <li>Vær ekstra obs på tekst, bilder og overskrifter</li>
          <li>Det som virker litt rart, er ofte verdt å undersøke nærmere</li>
        </ul>
      </div>
      <div class="popup-actions">
        <button class="popup-btn popup-btn-play" @click="showHintPopup = false">Ok</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 2.5rem);
}

.page {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
}

.center-container {
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 1100px;
  gap: 1.5rem;
}

.picture-wrapper {
  display: flex;
  gap: 2rem;
  margin-top: 1rem;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}

.picture-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  flex: 1 1 300px;
  max-width: 420px;
  width: 100%;
  padding: 0.5rem;
  border-radius: 16px;
  transition: all 0.2s ease;
}

.picture-card.selected {
  box-shadow: 0 0 0 4px rgba(0, 0, 0, 0.2);
}

.picture-card.correct {
  background: #a5d6a7;
  box-shadow: 0 0 0 4px #2e7d32;
}

.picture-card.wrong {
  background: #ef9a9a;
  box-shadow: 0 0 0 4px #c62828;
}

.picture-placeholder {
  width: min(100%, 420px);
  aspect-ratio: 420 / 320;
  background-color: var(--beige-light);
  border: 2px solid var(--brown-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(1rem, 2vw, 2rem);
  font-weight: 700;
  overflow: hidden;
}

.picture {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fake-button {
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--orange-medium);
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, filter 0.2s ease;
}

.fake-button:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: scale(1.02);
}

.fake-button:active:not(:disabled) {
  transform: scale(0.97);
}

.fake-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.task-wrapper {
  margin: 0;
  width: min(100%, 800px);
  font-size: clamp(1rem, 2.5vw, 2rem);
  padding: 1rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
}

.task-wrapper p {
  margin: 0;
}

.answer-feedback {
  width: min(100%, 800px);
  padding: 1.25rem 1.5rem;
  background: rgba(255, 255, 255, 0.85);
  border: 3px solid var(--brown-dark);
  border-radius: 24px;
  text-align: center;
}

.answer-feedback p {
  margin: 0 0 0.75rem;
  font-size: 1.1rem;
}

.primary-btn {
  padding: 0.75rem 1.5rem;
  border-radius: 24px;
  border: 3px solid var(--brown-dark);
  background: var(--orange-medium);
  font-size: 1.1rem;
  font-weight: 700;
  cursor: pointer;
}

.primary-btn:hover {
  filter: brightness(1.08);
}

.end-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.error {
  color: #c62828;
  font-weight: 600;
}

@media (max-width: 768px) {
  .page {
    padding: 1rem;
  }

  .picture-wrapper {
    gap: 1rem;
  }

  .task-wrapper {
    padding: 1rem;
    border-width: 3px;
    border-radius: 24px;
  }

  .fake-button {
    border-width: 3px;
    border-radius: 24px;
  }
}

.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 18%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.hint-popup {
  width: min(560px, calc(100vw - 32px));
  background: var(--orange-medium-80);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 28px 24px 20px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.popup-title {
  margin: 0 0 10px;
  font-size: 2rem;
  line-height: 1.15;
  font-weight: 800;
  color: var(--color-text);
}

.hint-text {
  margin-top: 10px;
  font-size: 1.1rem;
  line-height: 1.35;
  font-weight: 700;
  color: var(--color-text);
  text-align: left;
}

.hint-text p {
  margin: 0 0 8px;
  font-size: 1.2rem;
}

.hint-text ul {
  margin: 0;
  padding-left: 24px;
}

.hint-text li {
  margin-bottom: 6px;
}

.popup-actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
}

.popup-btn {
  min-width: 130px;
  padding: 14px 24px;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text);
  cursor: pointer;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
}

.popup-btn-play {
  background: var(--green-confirm);
}
</style>
