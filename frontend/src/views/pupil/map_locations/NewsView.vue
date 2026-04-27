<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import TaskGuide from '@/components/TaskGuide.vue'
import CompleteLevelPopup from '@/components/CompleteLevelPopup.vue'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  NewsVerificationRequest,
  NewsVerificationResponse,
  TaskDifficulty,
  ValidationResult,
} from '@/types/chapter'
import thumbsUpNews from '@/assets/ThumbsUpNews.png'
import thumbsDownNews from '@/assets/ThumbsDownNews.png'

const router = useRouter()

const NYHETSKVARTALET_CHAPTER_ID = 1
const DIFFICULTIES: TaskDifficulty[] = ['EASY', 'MEDIUM', 'HARD']

const showHelpPopup = ref(false)
const showHintPopup = ref(false)
const showGuidePopup = ref(true)
const showCompletePopup = ref(false)

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

const popupTitle = ref('')
const popupMessage = ref('')
const popupSubMessage = ref('')
const popupSuccess = ref(false)

const difficultyLabel = computed(() => {
  const labels: Record<TaskDifficulty, string> = {
    EASY: 'Lett',
    MEDIUM: 'Middels',
    HARD: 'Vanskelig',
  }

  return labels[DIFFICULTIES[stepIndex.value]!]
})

const progressPercent = computed(() => {
  return ((stepIndex.value + 1) / DIFFICULTIES.length) * 100
})

function closeHelpPopup(): void {
  showHelpPopup.value = false
}

function closeHintPopup(): void {
  showHintPopup.value = false
}

function revokeImageUrls(): void {
  for (const url of imageUrls.value) URL.revokeObjectURL(url)
  imageUrls.value = []
}

async function loadTask(): Promise<void> {
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

    const blobs = await Promise.all(task.fileIds.map((id) => getBlob(`/api/objects/${id}`)))

    imageUrls.value = blobs.map((blob) => URL.createObjectURL(blob))
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke laste oppgave'
  } finally {
    loading.value = false
  }
}

async function choose(fileId: number): Promise<void> {
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
    runAttempts.value += 1

    if (result.correct) {
      runCorrect.value += 1
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke sende svar'
    selectedFileId.value = null
  } finally {
    submitting.value = false
  }
}

function cardClass(fileId: number): string {
  if (selectedFileId.value !== fileId) return ''
  if (!lastResult.value) return 'selected'

  return lastResult.value.correct ? 'selected correct' : 'selected wrong'
}

async function next(): Promise<void> {
  if (!lastResult.value) return

  if (lastResult.value.runCompleted || stepIndex.value === DIFFICULTIES.length - 1) {
    showFinalResult()
    return
  }

  stepIndex.value += 1
  selectedFileId.value = null
  lastResult.value = null

  await loadTask()
}

function showFinalResult(): void {
  runDone.value = true

  const success = runCorrect.value === runAttempts.value

  popupSuccess.value = success
  popupTitle.value = success ? 'Du klarte det!' : 'Du klarte det ikke'
  popupMessage.value = success
    ? 'Gratulerer! Du klarte alle nivåene i Nyhetskvartalet.'
    : 'Du må ha rett på alle oppgavene for å bestå nyhetskvartalet.'
  popupSubMessage.value = success
    ? 'Du har nå fullført stoppestedet.'
    : "Trykk 'Prøv på nytt' for å prøve igjen, eller gå tilbake til kartet."

  showCompletePopup.value = true
}

async function retryLevel(): Promise<void> {
  stepIndex.value = 0
  selectedFileId.value = null
  lastResult.value = null
  runCorrect.value = 0
  runAttempts.value = 0
  runDone.value = false
  showCompletePopup.value = false

  await loadTask()
}

function goToMap(): void {
  revokeImageUrls()
  router.push('/pupil/classroom/map')
}

function closeCompletePopup(): void {
  showCompletePopup.value = false

  if (popupSuccess.value) {
    goToMap()
  }
}

onMounted(loadTask)
onBeforeUnmount(revokeImageUrls)
</script>

<template>
  <TopBar
    title="Nyhetskvartalet"
    help="På dette stoppestedet skal du prøve å se hvilken av nyhetsartiklene som er falsk. Det kan være noe i teksten, feil bilde, eller rar URL."
    hint="Se nøye på de ulike artiklene, det kan være noe smått som skiller de."
  />

  <div class="page">
    <div class="center-container">
      <template v-if="currentTask && !runDone">
        <div class="task-wrapper">
          <p>{{ currentTask.taskDescription }}</p>
        </div>

        <div class="progress-container">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>

          <p class="progress-text">
            Nivå {{ stepIndex + 1 }} / {{ DIFFICULTIES.length }} — {{ difficultyLabel }}
          </p>
        </div>

        <div
          v-if="lastResult"
          class="mini-feedback"
          :class="{ success: lastResult.correct, fail: !lastResult.correct }"
        >
          <strong>{{ lastResult.correct ? 'Riktig!' : 'Feil.' }}</strong>
          <span v-if="lastResult.feedback"> {{ lastResult.feedback }}</span>
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

            <p
              v-if="lastResult && selectedFileId === fileId"
              class="answer-label"
              :class="lastResult.correct ? 'correct-label' : 'wrong-label'"
            >
              {{ lastResult.correct ? 'Riktig svar' : 'Ditt valg' }}
            </p>

            <button
              class="fake-button"
              :disabled="!!lastResult || submitting"
              @click="choose(fileId)"
            >
              Velg dette bildet
            </button>
          </div>
        </div>

        <button v-if="lastResult" class="next-level-button" @click="next">
          {{ stepIndex === DIFFICULTIES.length - 1 ? 'Se resultat' : 'Gå til neste nivå' }}
        </button>
      </template>

      <p v-else-if="loading">Laster oppgave…</p>
      <p v-else-if="error" class="error">{{ error }}</p>
    </div>
  </div>

  <TaskGuide v-if="showGuidePopup" @close="showGuidePopup = false">
    <h2>Velkommen til nyhetskvartalet!</h2>
    <p>
      I nyhetskvartalet vil du få oppgaver som handler om å skille ekte avisforsider og uekte
      avisforsider. I hver oppgave vil du få én ekte avisforside og én falsk. Du skal velge hvilken
      du tror er falsk. Er du klar for å løse oppgaver?
    </p>
  </TaskGuide>

  <div v-if="showHelpPopup" class="popup-overlay" @click="closeHelpPopup">
    <div class="hint-popup" @click.stop>
      <h2 class="popup-title">Trenger du hjelp?</h2>

      <div class="hint-text">
        <p>
          I Nyhetskvartalet skal du undersøke informasjon og finne ut hva som er ekte og falskt.
        </p>
        <ul>
          <li>Les oppgaven nøye før du svarer</li>
          <li>Sammenlign bildene og se etter rare detaljer</li>
          <li>Velg det alternativet du mener er falskt</li>
        </ul>
      </div>

      <div class="popup-actions">
        <button class="popup-btn popup-btn-play" @click="closeHelpPopup">Ok</button>
      </div>
    </div>
  </div>

  <div v-if="showHintPopup" class="popup-overlay" @click="closeHintPopup">
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
        <button class="popup-btn popup-btn-play" @click="closeHintPopup">Ok</button>
      </div>
    </div>
  </div>

  <CompleteLevelPopup
    :show="showCompletePopup"
    :title="popupTitle"
    :message="popupMessage"
    :sub-message="popupSubMessage"
    :success="popupSuccess"
    :success-image="thumbsUpNews"
    :fail-image="thumbsDownNews"
    @close="closeCompletePopup"
    @retry="retryLevel"
    @map="goToMap"
  />
</template>

<style scoped>
.page {
  min-height: calc(100vh - 80px);
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
  background:
    linear-gradient(rgb(255 255 255 / 60%), rgb(255 255 255 / 60%)),
    url('@/assets/backgrounds/nyhetskvartalet-bg.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 1100px;
  gap: 1.5rem;
}

.task-wrapper {
  position: relative;
  width: min(100%, 800px);
  padding: 1.4rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
  box-shadow: 0 8px 0 var(--brown-dark);
}

.task-wrapper p {
  margin: 0;
}

.progress-container {
  width: min(100%, 500px);
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 18px;
  background: rgb(0 0 0 / 15%);
  border: 3px solid var(--brown-dark);
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--green-confirm);
  transition: width 0.4s ease;
}

.progress-text {
  margin-top: 0.4rem;
  margin-bottom: 0;
  font-weight: 700;
}

.mini-feedback {
  padding: 0.75rem 1.4rem;
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 700;
  box-shadow: 0 5px 0 var(--brown-dark);
}

.mini-feedback.success {
  background: var(--green-confirm);
}

.mini-feedback.fail {
  background: #d95b43;
  color: white;
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
  max-width: 520px;
  width: 100%;
  padding: 0;
  border-radius: 30px;
  background: rgb(255 255 255 / 35%);
  border: 3px solid rgb(91 62 43 / 25%);
  box-shadow: 0 10px 20px rgb(0 0 0 / 10%);
  overflow: hidden;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.picture-card:hover {
  transform: translateY(-4px);
}

.picture-card.selected {
  box-shadow: 0 0 0 5px var(--orange-medium);
}

.picture-card.correct {
  box-shadow: 0 0 0 6px var(--green-confirm);
  background: rgb(120 190 120 / 22%);
}

.picture-card.wrong {
  box-shadow: 0 0 0 6px #d95b43;
  background: rgb(217 91 67 / 18%);
}

.picture-placeholder {
  width: 100%;
  min-height: 360px;
  background-color: var(--beige-light);
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 0;
  box-sizing: border-box;
}

.picture {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.answer-label {
  margin: 0;
  padding: 0.45rem 1.2rem;
  border: 3px solid var(--brown-dark);
  border-radius: 999px;
  font-weight: 700;
}

.correct-label {
  background: var(--green-confirm);
}

.wrong-label {
  background: #d95b43;
  color: white;
}

.fake-button,
.next-level-button {
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--orange-medium);
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 6px 0 var(--brown-dark);
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    box-shadow 0.2s ease;
}

.fake-button {
  width: calc(100% - 2rem);
  max-width: 420px;
  margin-bottom: 1rem;
}

.next-level-button {
  max-width: 360px;
  background: var(--green-confirm);
}

.fake-button:hover:not(:disabled),
.next-level-button:hover {
  filter: brightness(1.1);
  transform: translateY(-3px);
  box-shadow: 0 9px 0 var(--brown-dark);
}

.fake-button:active:not(:disabled),
.next-level-button:active {
  transform: translateY(3px);
  box-shadow: 0 3px 0 var(--brown-dark);
}

.fake-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
  transform: none;
  filter: none;
  box-shadow: 0 6px 0 var(--brown-dark);
}

.error {
  color: #c62828;
  font-weight: 600;
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
}

.hint-text {
  margin-top: 10px;
  text-align: left;
}

.hint-text p {
  margin: 0 0 8px;
}

.hint-text ul {
  margin: 0;
  padding-left: 24px;
}

.hint-text li {
  margin-bottom: 6px;
}

.popup-actions {
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
  margin-top: 26px;
}

.popup-btn {
  min-width: 150px;
  padding: 14px 24px;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
  transition:
    transform 0.2s ease,
    filter 0.2s ease;
}

.popup-btn:hover {
  transform: scale(1.04);
  filter: brightness(1.08);
}

.popup-btn:active {
  transform: scale(0.97);
}

.popup-btn-play {
  background: var(--green-confirm);
}

.popup-btn-map {
  background: var(--orange-medium);
}

@media (max-width: 768px) {
  .page {
    padding: 1rem;
    background-attachment: scroll;
  }

  .picture-wrapper {
    gap: 1rem;
  }

  .task-wrapper {
    padding: 1rem;
    border-width: 3px;
    border-radius: 24px;
  }

  .fake-button,
  .next-level-button {
    border-width: 3px;
    border-radius: 24px;
  }
}
</style>
