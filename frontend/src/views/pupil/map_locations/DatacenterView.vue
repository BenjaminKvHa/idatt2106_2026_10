<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import VictoryImage from '@/assets/icons/detectives_and_thief/Victory.png'
import DefeatImage from '@/assets/icons/detectives_and_thief/Defeat.png'
import DatatyvenImage from '@/assets/icons/detectives_and_thief/Datatyven.png'
import victorySoundFile from '@/assets/sounds/levels/victory-sound.mp3'
import defeatSoundFile from '@/assets/sounds/levels/evil-laugh.mp3'
import confetti from 'canvas-confetti'
import DatacenterIntroPopup from '@/components/pupil/datacenter/DatacenterIntroPopup.vue'
import DatacenterResultPopup from '@/components/pupil/datacenter/DatacenterResultPopup.vue'
import DatacenterImageTask from '@/components/pupil/task/PupilImageTask.vue'
import DatacenterNewsTask from '@/components/pupil/task/PupilNewsTask.vue'
import DatacenterNavigation from '@/components/pupil/datacenter/DatacenterNavigation.vue'
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
  { value: 'MANIPULATED', label: 'Manipulert' },
]

const victorySound = new Audio(victorySoundFile)
victorySound.volume = 0.3
const defeatSound = new Audio(defeatSoundFile)
defeatSound.volume = 0.3

const showHelpPopup = ref(false)
const showHintPopup = ref(false)
const showGuidePopup = ref(true)
const showResultPopup = ref(false)
const resultType = ref<ResultType>(null)

const levels = ref<LevelState[]>([])
const currentLevelIndex = ref(0)
const loading = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

const currentLevel = computed<LevelState | null>(
  () => levels.value[currentLevelIndex.value] ?? null,
)

const isLastLevel = computed(
  () => currentLevelIndex.value === levels.value.length - 1,
)

const isCurrentLevelAnswered = computed(
  () => currentLevel.value?.result != null,
)

const correctCount = computed(
  () => levels.value.filter((l) => l.result?.correct).length,
)

const allAnswersCorrect = computed(
  () =>
    levels.value.length > 0 &&
    levels.value.every((l) => l.result?.correct === true),
)

const canSubmit = computed(() => {
  const level = currentLevel.value
  if (!level || level.result || submitting.value) return false
  if (level.task.format === 'IMAGE_VERIFICATION') {
    return (
      level.selectedCategory !== null && level.selectedJustification !== null
    )
  }
  return level.selectedFileId !== null
})

const resultImage = computed(() =>
  resultType.value === 'victory' ? VictoryImage : DefeatImage,
)
const resultAltText = computed(() =>
  resultType.value === 'victory' ? 'Seier' : 'Tap',
)
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

    const built: LevelState[] = []
    for (const task of run.tasks) {
      const fileIds =
        task.format === 'IMAGE_VERIFICATION'
          ? task.imageTask
            ? [task.imageTask.fileId]
            : []
          : (task.newsTask?.fileIds ?? [])

      const blobs = await Promise.all(
        fileIds.map((id) => getBlob(`/api/objects/${id}`)),
      )
      const urls = blobs.map((b) => URL.createObjectURL(b))

      built.push({
        task,
        imageUrls: urls,
        selectedCategory: null,
        selectedJustification: null,
        selectedFileId: null,
        result: null,
      })
    }
    levels.value = built
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
    let body: DatacenterAnswerRequest
    if (level.task.format === 'IMAGE_VERIFICATION') {
      body = {
        format: 'IMAGE_VERIFICATION',
        imageAnswer: {
          selectedCategory: level.selectedCategory!,
          selectedJustification: level.selectedJustification!,
        },
        newsAnswer: null,
      }
    } else {
      body = {
        format: 'NEWS_VERIFICATION',
        imageAnswer: null,
        newsAnswer: { selectedFileId: level.selectedFileId! },
      }
    }

    const result = await post<ValidationResult>(
      `/api/chapters/datasenteret/tasks/${level.task.taskId}/validate`,
      body,
    )
    level.result = result

    if (!result.correct) {
      openResultPopup('defeat')
    }
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
  victorySound.pause()
  defeatSound.pause()
  victorySound.currentTime = 0
  defeatSound.currentTime = 0
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
    victorySound.play()
    fireVictoryConfetti()
  } else if (resultType.value === 'defeat') {
    defeatSound.play()
  }
})

function openResultPopup(type: ResultType) {
  resultType.value = type
  showResultPopup.value = true
}

function closeResultPopup() {
  showResultPopup.value = false
  resultType.value = null
  stopResultSounds()
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
onBeforeUnmount(revokeAllImageUrls)
</script>

<template>
  <TopBar
    title="Datasenteret"
    @help="showHelpPopup = true"
    @hint="showHintPopup = true"
  />

  <div class="page pupil-task-page">
    <div class="center-container pupil-task-center">
      <p v-if="loading">Laster oppgaver…</p>
      <p v-else-if="error" class="error pupil-task-error">{{ error }}</p>

      <template v-else-if="currentLevel">
        <div class="progress-box">
          <p>Nivå {{ currentLevelIndex + 1 }}/{{ levels.length }}</p>
        </div>

        <div class="task-wrapper pupil-task-wrapper">
          <p>
            {{
              currentLevel.task.format === 'IMAGE_VERIFICATION'
                ? currentLevel.task.imageTask?.taskDescription
                : currentLevel.task.newsTask?.taskDescription
            }}
          </p>
        </div>

        <div class="task-card pupil-task-card">
          <DatacenterImageTask
            v-if="currentLevel.task.format === 'IMAGE_VERIFICATION'"
            :level="currentLevel"
            :categories="CATEGORIES"
            :can-submit="canSubmit"
            @select-category="selectCategory"
            @select-justification="selectJustification"
            @submit="submit"
          />

          <DatacenterNewsTask
            v-else
            :level="currentLevel"
            :submitting="submitting"
            :can-submit="canSubmit"
            @select-file-id="selectFileId"
            @submit="submit"
          />

          <div
            v-if="currentLevel.result"
            class="feedback-box pupil-feedback-box"
            :class="currentLevel.result.correct ? 'correct' : 'wrong'"
          >
            <p class="feedback-title">
              {{ currentLevel.result.correct ? 'Riktig!' : 'Feil!' }}
            </p>
            <p>{{ currentLevel.result.feedback }}</p>
          </div>
        </div>

        <DatacenterNavigation
          :is-first="currentLevelIndex === 0"
          :is-last="isLastLevel"
          :is-answered="isCurrentLevelAnswered"
          @previous="goToPreviousLevel"
          @next="goToNextLevel"
          @finish="finishGame"
        />
      </template>

      <DatacenterIntroPopup
        v-if="showGuidePopup"
        :image="DatatyvenImage"
        @close="showGuidePopup = false"
      />

      <DatacenterResultPopup
        v-if="showResultPopup"
        :image="resultImage"
        :alt-text="resultAltText"
        :kicker="resultKicker"
        :title="resultTitle"
        :message="resultMessage"
        :submessage="resultSubmessage"
        :highlight-title="resultHighlightTitle"
        :highlight-text="resultHighlightText"
        :result-type="resultType"
        @close="closeResultPopup"
        @retry="resetGame"
        @map="goBackToMap"
      />
    </div>
  </div>
</template>

<style scoped>
p {
  margin: 0;
}

.page {
  padding-bottom: 3rem;
}

.progress-box {
  width: min(100%, 700px);
  padding: 1rem 1.4rem;
  background: var(--beige-light);
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 700;
}

.task-card {
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.feedback-title {
  margin-bottom: 0.35rem;
  font-weight: 800;
}

.correct {
  background: #a5d6a7 !important;
  border-color: #2e7d32 !important;
}

.wrong {
  background: #ef9a9a !important;
  border-color: #c62828 !important;
}

@media (max-width: 768px) {
  .progress-box,
  .task-wrapper,
  .task-card {
    border-width: 3px;
    border-radius: 24px;
  }

  .task-wrapper,
  .task-card {
    padding: 1rem;
  }
}
</style>
