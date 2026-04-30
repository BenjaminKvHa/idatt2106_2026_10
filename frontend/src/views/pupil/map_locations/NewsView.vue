<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import HintCreature from '@/components/pupil/task/BlobbyQuestion.vue'
import TaskGuide from '@/components/pupil/TaskGuide.vue'
import CompleteLevelPopup from '@/components/pupil/CompleteLevelPopup.vue'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  NewsVerificationRequest,
  NewsVerificationResponse,
  ValidationResult,
} from '@/types/chapter'
import thumbsUpNews from '@/assets/icons/detectives_and_thief/ThumbsUpNews.png'
import thumbsDownNews from '@/assets/icons/detectives_and_thief/ThumbsDownNews.png'
import PupilNewsTask from '@/components/pupil/task/PupilNewsTask.vue'
import { useDifficultyProgression } from '@/composables/useDifficultyProgression'

const router = useRouter()

const NYHETSKVARTALET_CHAPTER_ID = 1

const {
  DIFFICULTIES,
  stepIndex,
  lastResult,
  runCorrect,
  runAttempts,
  runDone,
  loading,
  submitting,
  error,
  showGuidePopup,
  showCompletePopup,
  popupTitle,
  popupMessage,
  popupSubMessage,
  popupSuccess,
  currentDifficulty,
  difficultyLabel,
  progressPercent,
  isLastStep,
  recordResult,
  advanceStep,
  reset,
} = useDifficultyProgression()

const showHelpPopup = ref(false)
const showHintPopup = ref(false)

const currentTask = ref<NewsVerificationResponse | null>(null)
const imageUrls = ref<string[]>([])
const selectedFileId = ref<number | null>(null)

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
    const task = await get<NewsVerificationResponse>(
      `/api/chapters/${NYHETSKVARTALET_CHAPTER_ID}/nyhetskvartalet?difficulty=${currentDifficulty.value}`,
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
    recordResult(result.correct)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke sende svar'
    selectedFileId.value = null
  } finally {
    submitting.value = false
  }
}

async function next(): Promise<void> {
  if (!lastResult.value) return

  if (isLastStep.value) {
    showFinalResult()
    return
  }

  advanceStep()
  selectedFileId.value = null

  await loadTask()
}

function showFinalResult(): void {
  const success = runCorrect.value === DIFFICULTIES.length

  runDone.value = success
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
  reset()
  selectedFileId.value = null

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
  <div class="nyhetskvartalet-page-bg">
    <TopBar title="Nyhetskvartalet" />

    <div class="page">
      <div class="center-container">
        <template v-if="currentTask && !runDone">
          <div class="progress-container">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>

            <p class="progress-text">
              Nivå {{ stepIndex + 1 }} / {{ DIFFICULTIES.length }} — {{ difficultyLabel }}
            </p>
          </div>

          <HintCreature :chapter-id="NYHETSKVARTALET_CHAPTER_ID" />

          <PupilNewsTask
            :task-description="currentTask.taskDescription"
            :file-ids="currentTask.fileIds"
            :image-urls="imageUrls"
            :selected-file-id="selectedFileId"
            :result="lastResult"
            :submitting="submitting"
            select-button-text="Velg dette bildet"
            @select-file="choose"
          />

          <div
            v-if="lastResult"
            class="mini-feedback"
            :class="{ success: lastResult.correct, fail: !lastResult.correct }"
          >
            <span>{{ lastResult.feedback }}</span>
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
        avisforsider. I hver oppgave vil du få én ekte avisforside og én falsk. Du skal velge
        hvilken du tror er falsk. Er du klar for å løse oppgaver?
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
  </div>
</template>

<style scope>
.nyhetskvartalet-page-bg {
  min-height: 100vh;
  background:
    linear-gradient(rgb(255 255 255 / 60%), rgb(255 255 255 / 60%)),
    url('@/assets/backgrounds/nyhetskvartalet-bg.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.page {
  min-height: calc(100vh - 4.2rem);
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
  background: transparent;
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

.progress-container {
  width: min(100%, 540px);
  text-align: center;
}

.progress-bar {
  width: 100%;
  height: 14px;
  background: rgb(255 246 220 / 55%);
  border: 2px solid rgb(91 62 43 / 70%);
  border-radius: 999px;
  overflow: hidden;
  box-shadow:
    inset 0 2px 5px rgb(0 0 0 / 12%),
    0 3px 10px rgb(0 0 0 / 10%);
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #9df13c 0%, #78d927 55%, #5cc322 100%);
  border-radius: 999px;
  transition: width 0.4s ease;
  box-shadow: inset 0 1px 0 rgb(255 255 255 / 45%);
}

.progress-text {
  margin-top: 0.5rem;
  margin-bottom: 0;
  font-weight: 800;
  color: #1f160f;
  text-shadow: 0 1px 0 rgb(255 255 255 / 45%);
}

.mini-feedback {
  padding: 0.75rem 1.4rem;
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 700;
  box-shadow: 0 5px 0 #5b371f;
}

.mini-feedback.success {
  background: var(--green-confirm);
}

.mini-feedback.fail {
  background: #d95b43;
  color: white;
}

.next-level-button {
  width: 100%;
  max-width: 360px;
  padding: 0.9rem 1.8rem;
  border-radius: 24px;
  border: 2px solid var(--brown-dark);
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
  color: var(--brown-dark);
  cursor: pointer;
  font-weight: 850;
  box-shadow: 0 5px 14px rgb(0 0 0 / 14%);
  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease;
}

.next-level-button:hover {
  filter: brightness(1.03);
  transform: translateY(-1px);
  box-shadow: 0 7px 18px rgb(0 0 0 / 16%);
}

.next-level-button:active {
  transform: translateY(1px);
  box-shadow: 0 3px 9px rgb(0 0 0 / 12%);
}

.error {
  color: #c62828;
  font-weight: 600;
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

.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 28%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.hint-popup {
  width: min(680px, calc(100vw - 32px));
  background: linear-gradient(180deg, #fff0c9 0%, #f5d59a 58%, #e8b86f 100%);
  border: 7px solid var(--brown-dark);
  border-radius: 42px;
  box-shadow:
    0 12px 0 #5b371f,
    0 26px 45px rgb(0 0 0 / 28%),
    inset 0 4px 0 rgb(255 255 255 / 45%);
  padding: 32px 28px;
  text-align: center;
}

.popup-title {
  margin: 0 0 1rem;
  color: var(--brown-dark);
  font-size: clamp(1.8rem, 4vw, 2.8rem);
  text-shadow: 0 3px 0 rgb(255 255 255 / 45%);
}

.popup-actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
}

.popup-btn {
  min-width: 180px;
  padding: 16px 28px;
  border-radius: 28px;
  border: 5px solid var(--brown-dark);
  font-size: 1.15rem;
  font-weight: 900;
  color: var(--brown-dark);
  cursor: pointer;
  box-shadow: 0 8px 0 #5b371f;
  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease;
}

.popup-btn:hover {
  filter: brightness(1.04);
  transform: translateY(-2px);
}

.popup-btn:active {
  transform: translateY(5px);
  box-shadow: 0 3px 0 #5b371f;
}

.popup-btn-play {
  background: linear-gradient(180deg, #9df13c 0%, #78d927 100%);
}

.popup-btn-map {
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
}

:deep(.task-guide),
:deep(.guide-popup),
:deep(.popup-card) {
  background: linear-gradient(180deg, #fff0c9 0%, #f5d59a 58%, #e8b86f 100%) !important;
  border: 7px solid var(--brown-dark) !important;
  border-radius: 42px !important;
  box-shadow:
    0 12px 0 #5b371f,
    0 26px 45px rgb(0 0 0 / 28%),
    inset 0 4px 0 rgb(255 255 255 / 45%) !important;
}

:deep(.task-guide button),
:deep(.guide-popup button),
:deep(.popup-card button),
:deep(.select-button),
:deep(.news-task button) {
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%) !important;
  color: var(--brown-dark) !important;
  border: 5px solid var(--brown-dark) !important;
  border-radius: 28px !important;
  font-weight: 900 !important;
  box-shadow: 0 8px 0 #5b371f !important;
}

:deep(.task-guide button:hover),
:deep(.guide-popup button:hover),
:deep(.popup-card button:hover),
:deep(.select-button:hover),
:deep(.news-task button:hover) {
  filter: brightness(1.04);
  transform: translateY(-2px);
}

:deep(.task-guide button:active),
:deep(.guide-popup button:active),
:deep(.popup-card button:active),
:deep(.select-button:active),
:deep(.news-task button:active) {
  transform: translateY(5px);
  box-shadow: 0 3px 0 #5b371f !important;
}

@media (max-width: 768px) {
  .page {
    padding: 1rem;
    background-attachment: scroll;
  }

  .next-level-button {
    border-width: 4px;
    border-radius: 24px;
  }

  .hint-popup {
    padding: 24px 18px;
    border-width: 5px;
    border-radius: 30px;
  }

  .popup-btn {
    width: 100%;
    min-width: unset;
  }
}
</style>
