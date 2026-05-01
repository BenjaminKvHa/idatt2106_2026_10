<template>
  <div class="fotografen-page">
    <TopBar title="Fotografen" />

    <div class="content">
      <div v-if="runDone" class="task-container">
        <div class="task-box end-screen">
          <h2>Ferdig!</h2>
          <p>Du fikk {{ runCorrect }} av {{ runAttempts }} riktig.</p>
          <button class="primary-btn" @click="finish">Til kartet</button>
        </div>
      </div>

      <div v-else-if="currentTask" class="task-container">
        <div class="progress-container">
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: ((stepIndex + 1) / DIFFICULTIES.length) * 100 + '%' }"
            ></div>
          </div>

          <h1 class="level">Nivå {{ stepIndex + 1 }} — {{ difficultyLabel }}</h1>
        </div>

        <HintCreature :chapter-id="FOTOGRAFEN_CHAPTER_ID" />

        <div class="task-box">
          <PupilImageTask
            :task-description="currentTask.taskDescription"
            :image-url="imageUrl"
            :categories="CATEGORIES"
            :justifications="currentTask.justifications"
            :selected-category="selectedCategory"
            :selected-justification="selectedJustification"
            :result="lastResult"
            :submitting="submitting"
            @select-category="selectCategory"
            @select-justification="selectJustification"
          />

          <div v-if="lastResult" class="answer-feedback">
            <p>
              <strong>{{ lastResult.correct ? 'Riktig!' : 'Feil.' }}</strong>
            </p>
            <p>{{ lastResult.feedback }}</p>
          </div>
        </div>

        <div class="bottom-actions">
          <button v-if="!lastResult" @click="submit" :disabled="!canSubmit || submitting">
            Svar
          </button>
          <button v-else @click="next">
            {{ stepIndex === DIFFICULTIES.length - 1 ? 'Se resultat' : 'Neste nivå →' }}
          </button>
        </div>
      </div>

      <div v-else-if="loading" class="task-container">
        <p>Laster oppgave…</p>
      </div>

      <div v-else-if="error" class="task-container">
        <p class="error">{{ error }}</p>
      </div>

      <TaskGuide v-if="showGuidePopup" @close="showGuidePopup = false">
        <h2>Velkommen til fotografen!</h2>
        <p>
          I fotografen vil du få se et bilde, og oppgaven din er å finne ut om bildet er ekte,
          KI-generert eller manipulert. Etter du har bestemt det, skal du si hvorfor du mener bildet
          er en av disse. Er du klar for å løse oppgaver?
        </p>
      </TaskGuide>

      <CompleteLevelPopup
        :show="showCompletePopup"
        :title="popupTitle"
        :message="popupMessage"
        :sub-message="popupSubMessage"
        :success="popupSuccess"
        :success-image="thumbsUpPhoto"
        :fail-image="thumbsDownPhoto"
        @close="closeCompletePopup"
        @retry="retryLevel"
        @map="goToMap"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import HintCreature from '@/components/pupil/task/BlobbyQuestion.vue'
import TaskGuide from '@/components/pupil/TaskGuide.vue'
import CompleteLevelPopup from '@/components/pupil/CompleteLevelPopup.vue'
import thumbsUpPhoto from '@/assets/icons/detectives_and_thief/ThumbsUpPhotographer.png'
import thumbsDownPhoto from '@/assets/icons/detectives_and_thief/ThumbsDownPhotographer.png'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  ImageCategory,
  ImageVerificationRequest,
  ImageVerificationResponse,
  ValidationResult,
} from '@/types/chapter'
import PupilImageTask from '@/components/pupil/task/PupilImageTask.vue'
import { useDifficultyProgression } from '@/composables/useDifficultyProgression'

const router = useRouter()

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
  isLastStep,
  recordResult,
  advanceStep,
} = useDifficultyProgression()

const FOTOGRAFEN_CHAPTER_ID = 2

const CATEGORIES: { value: ImageCategory; label: string }[] = [
  { value: 'REAL', label: 'Ekte' },
  { value: 'AI_GENERATED', label: 'KI-generert' },
  { value: 'MANIPULATED', label: 'Manipulert' },
]

const currentTask = ref<ImageVerificationResponse | null>(null)
const imageUrl = ref('')
const selectedCategory = ref<ImageCategory | null>(null)
const selectedJustification = ref<string | null>(null)

const canSubmit = computed(
  () => selectedCategory.value !== null && selectedJustification.value !== null,
)

function revokeImageUrl() {
  if (imageUrl.value) {
    URL.revokeObjectURL(imageUrl.value)
    imageUrl.value = ''
  }
}

async function loadTask() {
  loading.value = true
  error.value = null
  revokeImageUrl()
  currentTask.value = null

  try {
    const task = await get<ImageVerificationResponse>(
      `/api/chapters/${FOTOGRAFEN_CHAPTER_ID}/fotografen?difficulty=${currentDifficulty.value}`,
    )

    currentTask.value = task

    const blob = await getBlob(`/api/objects/${task.fileId}`)
    imageUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke laste oppgave'
  } finally {
    loading.value = false
  }
}

function selectCategory(value: ImageCategory) {
  if (lastResult.value) return
  selectedCategory.value = value
  selectedJustification.value = null
}

function selectJustification(value: string) {
  if (lastResult.value) return
  selectedJustification.value = value
}

async function submit() {
  if (!currentTask.value || !canSubmit.value) return

  submitting.value = true
  error.value = null

  try {
    const body: ImageVerificationRequest = {
      selectedCategory: selectedCategory.value!,
      selectedJustification: selectedJustification.value!,
    }

    const result = await post<ValidationResult>(
      `/api/chapters/fotografen/tasks/${currentTask.value.taskId}/validate`,
      body,
    )

    lastResult.value = result
    recordResult(result.correct)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke sende svar'
  } finally {
    submitting.value = false
  }
}

function openResultPopup() {
  const success = runCorrect.value === runAttempts.value

  popupSuccess.value = success

  if (success) {
    popupTitle.value = 'Du klarte det!'
    popupMessage.value = `Gratulerer! Du fikk ${runCorrect.value} av ${runAttempts.value} riktig.`
    popupSubMessage.value = 'Du har nå fullført Fotografen og låst opp neste stoppested.'
  } else {
    popupTitle.value = 'Du klarte det ikke'
    popupMessage.value = `Du fikk ${runCorrect.value} av ${runAttempts.value} riktig.`
    popupSubMessage.value = 'Prøv igjen for å fullføre Fotografen.'
  }

  showCompletePopup.value = true
}

async function next() {
  if (!lastResult.value) return

  if (lastResult.value.runCompleted || isLastStep.value) {
    runDone.value = true
    openResultPopup()
    return
  }

  advanceStep()
  selectedCategory.value = null
  selectedJustification.value = null

  await loadTask()
}

function closeCompletePopup() {
  showCompletePopup.value = false

  if (popupSuccess.value) {
    goToMap()
  }
}

async function retryLevel(): Promise<void> {
  stepIndex.value = 0
  selectedCategory.value = null
  selectedJustification.value = null
  lastResult.value = null
  runCorrect.value = 0
  runAttempts.value = 0
  runDone.value = false
  showCompletePopup.value = false

  await loadTask()
}

function goToMap() {
  revokeImageUrl()
  router.push('/pupil/classroom/map')
}

function finish() {
  goToMap()
}

onMounted(loadTask)
onBeforeUnmount(revokeImageUrl)
</script>

<style scoped>
.fotografen-page {
  min-height: 100vh;
  width: 100%;
  background:
    linear-gradient(rgb(255 255 255 / 45%), rgb(255 255 255 / 45%)),
    url('@/assets/backgrounds/fotografen-bg.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.content {
  width: 100%;
  min-height: calc(100vh - var(--topbar-height));
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.task-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  position: relative;
  overflow: visible !important;
}

.task-container :deep(.hint-creature-wrapper) {
  position: absolute;
  width: 120px;
  height: 120px;
  left: calc(50% - 535px);
  top: 5.6rem;
  z-index: 10;
}

.task-container :deep(.hint-creature-button) {
  position: static;
  width: 120px;
  height: 120px;
}

.task-container :deep(.hint-creature-button img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.progress-container {
  position: relative;
  z-index: 3;
  width: min(100%, 540px);
  max-width: 540px;
  text-align: center;
  margin-bottom: 1rem;
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

.level {
  text-align: center;
  margin: 0.5rem 0 0;
  color: #1f160f;
  font-size: clamp(1.5rem, 3vw, 2.4rem);
  text-shadow: 0 1px 0 rgb(255 255 255 / 45%);
}

.task-box {
  width: 100%;
  max-width: 760px;
  background: rgb(255 243 214 / 78%);
  border: 3px solid var(--brown-dark);
  border-radius: 28px;
  padding: 24px;
  box-shadow:
    0 6px 18px rgb(0 0 0 / 13%),
    inset 0 2px 0 rgb(255 255 255 / 45%);
  position: relative;
  overflow: visible !important;
  box-sizing: border-box;
  z-index: 2;
}

.answer-feedback {
  margin-top: 20px;
  padding: 16px;
  border-radius: 18px;
  background: rgb(255 255 255 / 82%);
  border: 2px solid var(--brown-dark);
  width: 100%;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
  text-align: center;
  box-sizing: border-box;
}

.bottom-actions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.bottom-actions button,
.primary-btn {
  padding: 0.85rem 1.8rem;
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

.bottom-actions button:hover:not(:disabled),
.primary-btn:hover {
  filter: brightness(1.03);
  transform: translateY(-1px);
}

.bottom-actions button:active:not(:disabled),
.primary-btn:active {
  transform: translateY(1px);
  box-shadow: 0 3px 9px rgb(0 0 0 / 12%);
}

.bottom-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.end-screen {
  text-align: center;
}

.end-screen .primary-btn {
  margin-top: 20px;
  padding: 10px 24px;
}

.error {
  color: #c62828;
  font-weight: 600;
}

.hint-wrapper {
  width: 100%;
  max-width: 760px;
  margin: 0 auto;
}

@media (max-width: 1150px) {
  .task-container :deep(.hint-creature-wrapper) {
    left: calc(50% - 455px);
    top: 5.4rem;
  }
}

@media (max-width: 1000px) {
  .task-container {
    padding-top: 7rem;
  }

  .content {
    padding-top: calc(var(--topbar-height));
  }

  .task-container :deep(.hint-creature-wrapper) {
    left: 50%;
    top: -2.2rem;
    transform: translateX(-50%);
  }
}

@media (max-width: 768px) {
  .content {
    padding: 14px;
    padding-top: calc(var(--topbar-height));
  }

  .task-container {
    padding-top: 5.5rem;
  }

  .task-container :deep(.hint-creature-wrapper) {
    width: 85px;
    height: 85px;
    top: -1.8rem;
  }

  .task-container :deep(.hint-creature-button) {
    width: 85px;
    height: 85px;
  }

  .progress-container {
    width: min(100%, 420px);
    max-width: 420px;
  }

  .progress-bar {
    height: 12px;
  }

  .task-box {
    padding: 18px;
    border-width: 2px;
    border-radius: 24px;
  }

  .bottom-actions {
    width: 100%;
  }

  .bottom-actions button,
  .primary-btn {
    width: 100%;
  }
}
</style>
