<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import HintCreature from '@/components/pupil/task/BlobbyQuestion.vue'
import TaskGuide from '@/components/pupil/TaskGuide.vue'
import PupilImageTask from '@/components/pupil/task/PupilImageTask.vue'
import PupilNewsTask from '@/components/pupil/task/PupilNewsTask.vue'
import CompleteGamePopup from '@/components/pupil/CompleteGamePopup.vue'
import VictoryImage from '@/assets/icons/detectives_and_thief/Victory.png'
import DefeatImage from '@/assets/icons/detectives_and_thief/Defeat.png'
import DatatyvenImage from '@/assets/icons/detectives_and_thief/Datatyven.png'
import datacenterBg from '@/assets/backgrounds/datasenteret-bg.png'
import victorySoundFile from '@/assets/sounds/levels/victory-sound.mp3'
import defeatSoundFile from '@/assets/sounds/levels/evil-laugh.mp3'
import confetti from 'canvas-confetti'
import { useAudio } from '@/composables/useAudio'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  DatacenterAnswerRequest,
  DatacenterRunResponse,
  DatacenterTaskResponse,
  ImageCategory,
  ValidationResult,
} from '@/types/chapter'

type ResultType = 'victory' | 'defeat' | null

type LevelState = {
  task: DatacenterTaskResponse
  imageUrls: string[]
  selectedCategory: ImageCategory | null
  selectedJustification: string | null
  selectedFileId: number | null
  result: ValidationResult | null
}

const router = useRouter()

const DATASENTERET_CHAPTER_ID = 3

const CATEGORIES: { value: ImageCategory; label: string }[] = [
  { value: 'REAL', label: 'Ekte' },
  { value: 'AI_GENERATED', label: 'KI-generert' },
  { value: 'MANIPULATED', label: 'Manipulert' } as never,
]

const { playSound, stopSound } = useAudio()
let victorySoundRef: HTMLAudioElement | null = null
let defeatSoundRef: HTMLAudioElement | null = null

const showGuidePopup = ref(true)
const showHelpPopup = ref(false)
const showResultPopup = ref(false)
const resultType = ref<ResultType>(null)

const levels = ref<LevelState[]>([])
const currentLevelIndex = ref(0)
const loading = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

const currentLevel = computed(() => levels.value[currentLevelIndex.value] ?? null)
const isLastLevel = computed(() => currentLevelIndex.value === levels.value.length - 1)
const isCurrentLevelAnswered = computed(() => currentLevel.value?.result != null)
const correctCount = computed(() => levels.value.filter((level) => level.result?.correct).length)

const progressPercent = computed(() =>
  levels.value.length > 0 ? ((currentLevelIndex.value + 1) / levels.value.length) * 100 : 0,
)

const allAnswersCorrect = computed(
  () => levels.value.length > 0 && levels.value.every((level) => level.result?.correct === true),
)

const canSubmit = computed(() => {
  const level = currentLevel.value
  if (!level || level.result || submitting.value) return false

  if (level.task.format === 'IMAGE_VERIFICATION') {
    return level.selectedCategory !== null && level.selectedJustification !== null
  }

  return level.selectedFileId !== null
})

const pageStyle = computed(() => ({
  backgroundImage: `
    linear-gradient(rgb(255 255 255 / 45%), rgb(255 255 255 / 45%)),
    url(${datacenterBg})
  `,
}))

const resultImage = computed(() => (resultType.value === 'victory' ? VictoryImage : DefeatImage))
const resultAltText = computed(() => (resultType.value === 'victory' ? 'Seier' : 'Tap'))
const resultKicker = computed(() =>
  resultType.value === 'victory' ? 'Oppdrag fullført' : 'Oppdrag mislyktes',
)
const resultTitle = computed(() =>
  resultType.value === 'victory' ? 'Du klarte det!' : 'Du tapte!',
)
const resultMessage = computed(() =>
  resultType.value === 'victory'
    ? `Gratulerer! Du svarte riktig på alle ${levels.value.length} oppgavene og slo datatyven.`
    : `Du fikk ${correctCount.value} av ${levels.value.length} riktig. Datatyven slapp unna denne gangen.`,
)
const resultSubmessage = computed(() =>
  resultType.value === 'victory'
    ? 'Nå har du vist at du kan tenke som en ekte nett-detektiv!'
    : 'Men ikke gi opp – prøv igjen og se om du klarer å slå datatyven neste gang!',
)
const resultHighlightTitle = computed(() =>
  resultType.value === 'victory' ? '🏆 Spill fullført' : '💥 Prøv igjen',
)
const resultHighlightText = computed(() =>
  resultType.value === 'victory'
    ? 'Du har kommet helt til toppen i Datasenteret.'
    : 'Du kan gå tilbake og prøve oppgavene på nytt for å få en ny sjanse til å fullføre spillet.',
)

function revokeAllImageUrls() {
  for (const level of levels.value) {
    for (const url of level.imageUrls) URL.revokeObjectURL(url)
    level.imageUrls = []
  }
}

async function loadRun() {
  loading.value = true
  error.value = null
  revokeAllImageUrls()
  levels.value = []
  currentLevelIndex.value = 0

  try {
    const run = await get<DatacenterRunResponse>(
      `/api/chapters/${DATASENTERET_CHAPTER_ID}/datasenteret/run`,
    )

    levels.value = await Promise.all(
      run.tasks.map(async (task) => {
        const fileIds =
          task.format === 'IMAGE_VERIFICATION'
            ? task.imageTask
              ? [task.imageTask.fileId]
              : []
            : (task.newsTask?.fileIds ?? [])

        const blobs = await Promise.all(fileIds.map((id) => getBlob(`/api/objects/${id}`)))

        return {
          task,
          imageUrls: blobs.map((blob) => URL.createObjectURL(blob)),
          selectedCategory: null,
          selectedJustification: null,
          selectedFileId: null,
          result: null,
        }
      }),
    )
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke laste oppgavene'
  } finally {
    loading.value = false
  }
}

function selectCategory(value: ImageCategory) {
  const level = currentLevel.value
  if (!level || level.result) return

  level.selectedCategory = value
  level.selectedJustification = null
}

function selectJustification(value: string) {
  const level = currentLevel.value
  if (!level || level.result) return

  level.selectedJustification = value
}

function selectFileId(value: number) {
  const level = currentLevel.value
  if (!level || level.result) return

  level.selectedFileId = value
}

async function submit() {
  const level = currentLevel.value
  if (!level || !canSubmit.value) return

  submitting.value = true
  error.value = null

  try {
    const body: DatacenterAnswerRequest =
      level.task.format === 'IMAGE_VERIFICATION'
        ? {
            format: 'IMAGE_VERIFICATION',
            imageAnswer: {
              selectedCategory: level.selectedCategory!,
              selectedJustification: level.selectedJustification!,
            },
            newsAnswer: null,
          }
        : {
            format: 'NEWS_VERIFICATION',
            imageAnswer: null,
            newsAnswer: { selectedFileId: level.selectedFileId! },
          }

    level.result = await post<ValidationResult>(
      `/api/chapters/datasenteret/tasks/${level.task.taskId}/validate`,
      body,
    )
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke sende svar'
  } finally {
    submitting.value = false
  }
}

function goToNextLevel() {
  if (!isCurrentLevelAnswered.value || isLastLevel.value) return
  currentLevelIndex.value += 1
}

function goToPreviousLevel() {
  if (currentLevelIndex.value === 0) return
  currentLevelIndex.value -= 1
}

function finishGame() {
  if (!isCurrentLevelAnswered.value) return
  openResultPopup(allAnswersCorrect.value ? 'victory' : 'defeat')
}

function stopResultSounds() {
  stopSound(victorySoundRef)
  stopSound(defeatSoundRef)
  victorySoundRef = null
  defeatSoundRef = null
}

function fireVictoryConfetti() {
  const duration = 2000
  const end = Date.now() + duration
  const defaults = { zIndex: 3000 }

  const frame = () => {
    confetti({
      ...defaults,
      particleCount: 8,
      angle: 60,
      spread: 60,
      origin: { x: 0, y: 0.6 },
    })

    confetti({
      ...defaults,
      particleCount: 8,
      angle: 120,
      spread: 60,
      origin: { x: 1, y: 0.6 },
    })

    if (Date.now() < end) requestAnimationFrame(frame)
  }

  frame()
}

watch(showResultPopup, (isOpen) => {
  if (!isOpen) {
    stopResultSounds()
    return
  }

  stopResultSounds()

  if (resultType.value === 'victory') {
    victorySoundRef = playSound(victorySoundFile, 0.3)
    fireVictoryConfetti()
  } else if (resultType.value === 'defeat') {
    defeatSoundRef = playSound(defeatSoundFile, 0.3)
  }
})

function openResultPopup(type: ResultType) {
  resultType.value = type
  showResultPopup.value = true
}

function closeResultPopup() {
  showResultPopup.value = false
  resultType.value = null
}

function goBackToMap() {
  closeResultPopup()
  void router.push('/pupil/classroom/map')
}

async function resetGame() {
  closeResultPopup()
  showGuidePopup.value = true
  await loadRun()
}

onMounted(loadRun)
onBeforeUnmount(stopResultSounds)
onBeforeUnmount(revokeAllImageUrls)
</script>

<template>
  <div class="datacenter-page-bg" :style="pageStyle">
    <TopBar title="Datasenteret" />

    <div class="pupil-task-page">
      <div class="pupil-center-container datacenter-container">
        <p v-if="loading">Laster oppgaver…</p>
        <p v-else-if="error" class="pupil-error">{{ error }}</p>

        <template v-else-if="currentLevel">
          <HintCreature :chapter-id="DATASENTERET_CHAPTER_ID" />

          <div class="progress-container">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>

            <p class="progress-text">Nivå {{ currentLevelIndex + 1 }}/{{ levels.length }}</p>
          </div>

          <div class="pupil-panel pupil-task-text-box">
            <p>
              {{
                currentLevel.task.format === 'IMAGE_VERIFICATION'
                  ? currentLevel.task.imageTask?.taskDescription
                  : currentLevel.task.newsTask?.taskDescription
              }}
            </p>
          </div>

          <div class="pupil-panel pupil-task-card task-card">
            <PupilImageTask
              v-if="currentLevel.task.format === 'IMAGE_VERIFICATION'"
              :image-url="currentLevel.imageUrls[0]"
              :categories="CATEGORIES"
              :justifications="currentLevel.task.imageTask?.justifications ?? []"
              :selected-category="currentLevel.selectedCategory"
              :selected-justification="currentLevel.selectedJustification"
              :result="currentLevel.result"
              :submitting="submitting"
              @select-category="selectCategory"
              @select-justification="selectJustification"
            />

            <PupilNewsTask
              v-else
              :file-ids="currentLevel.task.newsTask?.fileIds ?? []"
              :image-urls="currentLevel.imageUrls"
              :alt-texts="currentLevel.task.newsTask?.altTexts"
              :selected-file-id="currentLevel.selectedFileId"
              :result="currentLevel.result"
              :submitting="submitting"
              select-button-text="Dette er den falske"
              @select-file="selectFileId"
            />

            <button
              v-if="currentLevel.task.format === 'IMAGE_VERIFICATION'"
              class="submit-button pupil-orange-button"
              :disabled="!canSubmit"
              @click="submit"
            >
              Svar
            </button>

            <button
              v-else-if="
                currentLevel.task.format === 'NEWS_VERIFICATION' &&
                currentLevel.selectedFileId !== null &&
                !currentLevel.result
              "
              class="submit-button pupil-orange-button"
              :disabled="!canSubmit"
              @click="submit"
            >
              Bekreft svar
            </button>

            <div
              v-if="currentLevel.result"
              class="pupil-feedback-box"
              :class="
                currentLevel.result.correct ? 'pupil-feedback-correct' : 'pupil-feedback-wrong'
              "
            >
              <p class="pupil-feedback-title">
                {{ currentLevel.result.correct ? 'Riktig!' : 'Feil!' }}
              </p>
              <p>{{ currentLevel.result.feedback }}</p>
            </div>
          </div>

          <div class="navigation-bar">
            <button
              class="nav-button nav-button-secondary"
              :disabled="currentLevelIndex === 0"
              @click="goToPreviousLevel"
            >
              Forrige
            </button>

            <button
              v-if="!isLastLevel"
              class="nav-button nav-button-primary"
              :disabled="!isCurrentLevelAnswered"
              @click="goToNextLevel"
            >
              Neste
            </button>

            <button
              v-else
              class="nav-button nav-button-primary"
              :disabled="!isCurrentLevelAnswered"
              @click="finishGame"
            >
              Fullfør
            </button>
          </div>
        </template>

        <TaskGuide v-if="showGuidePopup" @close="showGuidePopup = false">
          <h2>Velkommen til datasenteret!</h2>
          <p>Klarer du å svare riktig på alle oppgavene og slå datatyven?</p>
          <img :src="DatatyvenImage" alt="Datatyven" class="guide-image" />
        </TaskGuide>

        <TaskGuide v-if="showHelpPopup" @close="showHelpPopup = false">
          <h2>Trenger du hjelp?</h2>
          <p>Inne på denne siden skal du løse oppgaver for å bekjempe datatyven.</p>
          <ul>
            <li>Det er én oppgave på hvert nivå.</li>
            <li>Du kan bare svare én gang på hver oppgave.</li>
            <li>Du må svare riktig på alle nivåene for å vinne.</li>
          </ul>
        </TaskGuide>

        <CompleteGamePopup
          :show="showResultPopup"
          :success="resultType === 'victory'"
          :image="resultImage"
          :image-alt="resultAltText"
          :kicker="resultKicker"
          :title="resultTitle"
          :message="resultMessage"
          :sub-message="resultSubmessage"
          :highlight-title="resultHighlightTitle"
          :highlight-text="resultHighlightText"
          @close="closeResultPopup"
          @retry="resetGame"
          @map="goBackToMap"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
h2,
p {
  margin: 0;
}

.datacenter-container {
  position: relative;
  max-width: 1000px;
  gap: 0.8rem;
}

.datacenter-container :deep(.hint-creature-wrapper) {
  position: absolute;
  width: 120px;
  height: 120px;
  left: -6.5rem;
  top: 5rem;
  z-index: 10;
}

.datacenter-container :deep(.hint-creature-button) {
  position: static;
  width: 120px;
  height: 120px;
}

.datacenter-container :deep(.hint-creature-button img) {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.progress-container {
  position: relative;
  z-index: 3;
  width: min(100%, 540px);
  max-width: 540px;
  margin-bottom: 0.4rem;
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
  font-weight: 800;
  color: #1f160f;
  text-shadow: 0 1px 0 rgb(255 255 255 / 45%);
}

.pupil-task-text-box,
.task-card {
  position: relative;
  z-index: 2;
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.submit-button {
  width: 100%;
  padding: 0.9rem 1.8rem;
  border-radius: 24px;
  border: 2px solid var(--brown-dark);
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
  color: var(--brown-dark);
  font-weight: 850;
  cursor: pointer;
  box-shadow: 0 5px 14px rgb(0 0 0 / 14%);
  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease;
}

.submit-button:hover:not(:disabled) {
  background: var(--brown-dark);
  color: #fff6dc;
  filter: none;
  transform: translateY(-1px);
  box-shadow: 0 7px 18px rgb(0 0 0 / 16%);
}

.submit-button:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 3px 9px rgb(0 0 0 / 12%);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  transform: none;
  filter: none;
  box-shadow: 0 5px 14px rgb(0 0 0 / 10%);
}

.navigation-bar {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.nav-button {
  min-width: 200px;
  min-height: 58px;
  padding: 0.85rem 1.8rem;
  border-radius: 24px;
  border: 2px solid var(--brown-dark);
  font-weight: 850;
  color: var(--brown-dark);
  cursor: pointer;
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
  box-shadow: 0 5px 14px rgb(0 0 0 / 14%);
  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease,
    opacity 0.15s ease;
}

.nav-button:hover:not(:disabled) {
  filter: brightness(1.03);
  transform: translateY(-1px);
  box-shadow: 0 7px 18px rgb(0 0 0 / 16%);
}

.nav-button:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 3px 9px rgb(0 0 0 / 12%);
}

.nav-button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
  transform: none;
  filter: none;
  box-shadow: 0 5px 14px rgb(0 0 0 / 10%);
}

.guide-image {
  width: min(260px, 70%);
  margin: 1rem auto 0;
  display: block;
  object-fit: contain;
}

.datacenter-page-bg {
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.pupil-task-page {
  min-height: calc(100vh - 3.4rem);
}

:deep(.complete-game-popup),
:deep(.complete-level-popup),
:deep(.popup-card),
:deep(.result-popup),
:deep(.complete-popup) {
  display: flex;
  flex-direction: column;
  align-items: center;
  overflow: hidden;
}

:deep(.complete-game-popup img),
:deep(.complete-level-popup img),
:deep(.popup-card img),
:deep(.result-popup img),
:deep(.complete-popup img) {
  width: auto !important;
  height: auto !important;
  max-width: 260px !important;
  max-height: 260px !important;
  object-fit: contain !important;
  display: block;
  margin: 0 auto;
}

:deep(.complete-game-popup *),
:deep(.complete-level-popup *),
:deep(.popup-card *),
:deep(.result-popup *),
:deep(.complete-popup *) {
  max-width: 100%;
}

:deep(.pupil-feedback-box) {
  background: #f6d8c9 !important;
  opacity: 1 !important;
}

:deep(.pupil-feedback-correct) {
  background: #dff6c9 !important;
  opacity: 1 !important;
}

:deep(.pupil-feedback-wrong) {
  background: #f6c9c0 !important;
  opacity: 1 !important;
}

@media (max-width: 1150px) {
  .datacenter-container :deep(.hint-creature-wrapper) {
    left: -3.5rem;
    top: 5.2rem;
  }
}

@media (max-width: 1000px) {
  .datacenter-container {
    padding-top: 7rem;
  }

  .datacenter-container :deep(.hint-creature-wrapper) {
    left: 50%;
    top: -0.5rem;
    transform: translateX(-50%);
  }
}

@media (max-width: 768px) {
  .datacenter-container {
    padding-top: 5.5rem;
  }

  .datacenter-container :deep(.hint-creature-wrapper) {
    width: 85px;
    height: 85px;
    top: -0.8rem;
  }

  .datacenter-container :deep(.hint-creature-button) {
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

  .navigation-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .nav-button {
    width: 100%;
    min-width: 0;
  }
}

:deep(.pupil-feedback-box) {
  background: #f6d8c9 !important;
  opacity: 1 !important;
  margin-top: 1rem;
  padding: 1rem;
}

:deep(.pupil-feedback-correct) {
  background: #dff6c9 !important;
}

:deep(.pupil-feedback-wrong) {
  background: #f6c9c0 !important;
}
</style>
