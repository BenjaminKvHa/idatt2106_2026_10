<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import TaskGuide from '@/components/pupil/TaskGuide.vue'
import PupilImageTask from '@/components/pupil/task/PupilImageTask.vue'
import PupilNewsTask from '@/components/pupil/task/PupilNewsTask.vue'
import CompleteGamePopup from '@/components/pupil/CompleteGamePopup.vue'
import VictoryImage from '@/assets/icons/detectives_and_thief/Victory.png'
import DefeatImage from '@/assets/icons/detectives_and_thief/Defeat.png'
import DatatyvenImage from '@/assets/icons/detectives_and_thief/Datatyven.png'
import datacenterBg from '@/assets/backgrounds/datasenteret-bg.png'
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

const showGuidePopup = ref(true)
const showHelpPopup = ref(false)
const showHintPopup = ref(false)
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
  background: `
    linear-gradient(rgb(255 255 255 / 45%), rgb(255 255 255 / 45%)),
    url(${datacenterBg})
  `,
  backgroundSize: 'cover',
  backgroundPosition: 'center',
  backgroundAttachment: 'fixed',
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
  resultType.value = allAnswersCorrect.value ? 'victory' : 'defeat'
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
onBeforeUnmount(revokeAllImageUrls)
</script>

<template>
  <TopBar title="Datasenteret" @help="showHelpPopup = true" @hint="showHintPopup = true" />

  <div class="pupil-task-page" :style="pageStyle">
    <div class="pupil-center-container datacenter-container">
      <p v-if="loading">Laster oppgaver…</p>
      <p v-else-if="error" class="pupil-error">{{ error }}</p>

      <template v-else-if="currentLevel">
        <div class="progress-box pupil-panel">
          <p>Nivå {{ currentLevelIndex + 1 }}/{{ levels.length }}</p>
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

      <TaskGuide v-if="showHintPopup" @close="showHintPopup = false">
        <h2>Tips 💡</h2>
        <p>Les oppgaven nøye før du svarer.</p>
        <ul>
          <li>Se etter små detaljer som kan avsløre riktig svar.</li>
          <li>Ta deg god tid – du kan ikke svare på nytt.</li>
          <li>Bruk det du har lært tidligere i spillet.</li>
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
</template>

<style scoped>
h2,
p {
  margin: 0;
}

.datacenter-container {
  max-width: 1000px;
}

.task-card {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.progress-box {
  width: min(100%, 700px);
  padding: 1rem 1.4rem;
  font-weight: 700;
}

.submit-button {
  width: 100%;
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
  padding: 10px 18px;
  border-radius: 20px;
  border: 2px solid var(--color-text);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-button-primary {
  background: var(--orange-medium);
}

.nav-button-secondary {
  background: var(--yellow-award-60);
}

.nav-button:hover:not(:disabled) {
  filter: brightness(1.08);
}

.nav-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.guide-image {
  width: min(260px, 70%);
  margin: 1rem auto 0;
  display: block;
  object-fit: contain;
}

@media (max-width: 768px) {
  .navigation-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .nav-button {
    width: 100%;
    min-width: 0;
  }
}
</style>
