<template>
  <div class="page">
    <TopBar
      title="Fotografen"
      help="Du skal trykke på om du tror bildet er ekte, KI eller manipulert. Så velger du begrunnelse under."
      hint="Manipulert vil si at et ekte bilde er endret på. KI har ofte mer urealistiske trekk."
    />

    <div class="content">
      <div v-if="runDone" class="task-container">
        <div class="task-box end-screen">
          <h2>Ferdig!</h2>
          <p>Du fikk {{ runCorrect }} av {{ runAttempts }} riktig.</p>
          <button class="primary-btn" @click="finish">Til kartet</button>
        </div>
      </div>

      <div v-else-if="currentTask" class="task-container">
        <h1 class="level">Nivå {{ stepIndex + 1 }} — {{ difficultyLabel }}</h1>
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
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import TaskGuide from '@/components/pupil/TaskGuide.vue'
import CompleteLevelPopup from '@/components/pupil/CompleteLevelPopup.vue'
import thumbsUpPhoto from '@/assets/icons/detectives_and_thief/ThumbsUpPhotographer.png'
import thumbsDownPhoto from '@/assets/icons/detectives_and_thief/ThumbsDownPhotographer.png'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  ImageCategory,
  ImageVerificationRequest,
  ImageVerificationResponse,
  TaskDifficulty,
  ValidationResult,
} from '@/types/chapter'
import PupilImageTask from '@/components/pupil/task/PupilImageTask.vue'

const router = useRouter()
const showGuidePopup = ref(true)

const showCompletePopup = ref(false)
const popupTitle = ref('')
const popupMessage = ref('')
const popupSubMessage = ref('')
const popupSuccess = ref(true)

const FOTOGRAFEN_CHAPTER_ID = 2
const DIFFICULTIES: TaskDifficulty[] = ['EASY', 'MEDIUM', 'HARD']

const CATEGORIES: { value: ImageCategory; label: string }[] = [
  { value: 'REAL', label: 'Ekte' },
  { value: 'AI_GENERATED', label: 'KI-generert' },
  { value: 'MANIPULATED', label: 'Manipulert' },
]

const stepIndex = ref(0)
const currentTask = ref<ImageVerificationResponse | null>(null)
const imageUrl = ref('')
const selectedCategory = ref<ImageCategory | null>(null)
const selectedJustification = ref<string | null>(null)
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
    const difficulty = DIFFICULTIES[stepIndex.value]!

    const task = await get<ImageVerificationResponse>(
      `/api/chapters/${FOTOGRAFEN_CHAPTER_ID}/fotografen?difficulty=${difficulty}`,
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
    runAttempts.value++

    if (result.correct) {
      runCorrect.value++
    }
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

  if (lastResult.value.runCompleted || stepIndex.value === DIFFICULTIES.length - 1) {
    runDone.value = true
    openResultPopup()
    return
  }

  stepIndex.value++
  selectedCategory.value = null
  selectedJustification.value = null
  lastResult.value = null

  await loadTask()
}

function closeCompletePopup() {
  showCompletePopup.value = false
}

function finish() {
  revokeImageUrl()
  router.push('/pupil/classroom/map')
}

onMounted(loadTask)
onBeforeUnmount(revokeImageUrl)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    linear-gradient(rgb(255 255 255 / 45%), rgb(255 255 255 / 45%)),
    url('@/assets/backgrounds/fotografen-bg.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.level {
  text-align: center;
  margin-bottom: 18px;
}

.task-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  position: relative;
}

.task-box {
  width: 100%;
  max-width: 760px;
  background: rgba(255, 255, 255, 0.65);
  border: 3px solid #5c4033;
  border-radius: 24px;
  padding: 28px 28px 36px;
  box-shadow: 0 6px 0 rgba(0, 0, 0, 0.15);
  position: relative;
}

.answer-feedback {
  margin-top: 20px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.85);
  border: 2px solid #5c4033;
  width: 100%;
  max-width: 500px;
}

.bottom-actions {
  margin-top: 34px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.bottom-actions button,
.primary-btn {
  background: #f4a259;
  border: 2px solid #000;
  border-radius: 20px;
  padding: 10px 18px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.bottom-actions button:hover,
.primary-btn:hover {
  background: #e38b3d;
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

</style>
