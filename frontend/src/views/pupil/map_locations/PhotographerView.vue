<template>
  <div class="page">
    <TopBar title="Fotografen" />

    <div class="content">
      <h1 class="level">Nivå {{ currentLevel + 1 }}</h1>

      <div v-if="currentLevelData" class="task-container">
        <div class="task-box">
          <p class="task-text">
            Velg om bildet er ekte, KI-generert eller manipulert. Velg deretter en begrunnelse.
          </p>

          <div class="image-card">
            <div class="image-placeholder">Bilde {{ currentLevel + 1 }}</div>

            <div class="choices">
              <button
                @click="selectAnswer('ekte')"
                :disabled="currentLevelData.completed"
                :class="[
                  { selected: currentLevelData.image.selectedAnswer === 'ekte' },
                  currentLevelData.completed && currentLevelData.image.correctAnswer === 'ekte'
                    ? 'correct'
                    : '',
                  currentLevelData.completed &&
                  currentLevelData.image.selectedAnswer === 'ekte' &&
                  currentLevelData.image.correctAnswer !== 'ekte'
                    ? 'wrong'
                    : '',
                ]"
              >
                Ekte
              </button>

              <button
                @click="selectAnswer('ki')"
                :disabled="currentLevelData.completed"
                :class="[
                  { selected: currentLevelData.image.selectedAnswer === 'ki' },
                  currentLevelData.completed && currentLevelData.image.correctAnswer === 'ki'
                    ? 'correct'
                    : '',
                  currentLevelData.completed &&
                  currentLevelData.image.selectedAnswer === 'ki' &&
                  currentLevelData.image.correctAnswer !== 'ki'
                    ? 'wrong'
                    : '',
                ]"
              >
                KI-generert
              </button>

              <button
                @click="selectAnswer('manipulert')"
                :disabled="currentLevelData.completed"
                :class="[
                  { selected: currentLevelData.image.selectedAnswer === 'manipulert' },
                  currentLevelData.completed &&
                  currentLevelData.image.correctAnswer === 'manipulert'
                    ? 'correct'
                    : '',
                  currentLevelData.completed &&
                  currentLevelData.image.selectedAnswer === 'manipulert' &&
                  currentLevelData.image.correctAnswer !== 'manipulert'
                    ? 'wrong'
                    : '',
                ]"
              >
                Manipulert
              </button>
            </div>

            <div v-if="currentLevelData.image.selected" class="reason-box">
              <p>Hvorfor?</p>

              <button
                v-for="reason in currentLevelData.image.reasons"
                :key="reason.text"
                @click="selectReason(reason)"
                :disabled="currentLevelData.completed"
                :class="{ selected: currentLevelData.image.selectedReason?.text === reason.text }"
              >
                {{ reason.text }}
              </button>
            </div>

            <div v-if="currentLevelData.completed" class="answer-feedback">
              <p>
                <strong>Riktig svar:</strong> {{ getCorrectAnswerText(currentLevelData.image) }}
              </p>
              <p>
                <strong>Riktig begrunnelse:</strong>
                {{ getCorrectReasonText(currentLevelData.image) }}
              </p>
            </div>
          </div>
        </div>

        <div class="bottom-actions">
          <button @click="prevLevel" :disabled="currentLevel === 0">← Forrige nivå</button>

          <button
            v-if="!currentLevelData.completed"
            @click="submitLevel"
            :disabled="!canSubmitLevel()"
          >
            Svar
          </button>

          <button v-else @click="nextLevel">
            {{ currentLevel === levels.length - 1 ? 'Til kartet' : 'Neste nivå →' }}
          </button>
        </div>
      </div>

      <TaskGuide v-if="showGuidePopup" @close="showGuidePopup = false">
        <h2>Velkommen til fotografen!</h2>
        <p>
          I fotografen vil du få se et bilde, og oppgaven din er å finne ut om bildet er ekte,
          KI-generert eller manipulert. Etter du har bestemt det, skal du si hvorfor du mener bildet
          er en av disse. Er du klar for å løse oppgaver?
        </p>
      </TaskGuide>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import TaskGuide from '@/components/TaskGuide.vue'

const router = useRouter()
const showGuidePopup = ref(true)

type AnswerOption = 'ekte' | 'ki' | 'manipulert'

type Reason = {
  text: string
  correct: boolean
}

type ImageTask = {
  correctAnswer: AnswerOption
  selected: boolean
  selectedAnswer: AnswerOption | null
  reasons: Reason[]
  selectedReason: Reason | null
}

type Level = {
  image: ImageTask
  completed: boolean
  isCorrect: boolean
}

const currentLevel = ref(0)

const levels = ref<Level[]>([
  {
    completed: false,
    isCorrect: false,
    image: {
      correctAnswer: 'ekte',
      selected: false,
      selectedAnswer: null,
      reasons: [
        { text: 'Ser naturlig ut', correct: true },
        { text: 'Rare skygger og detaljer', correct: false },
      ],
      selectedReason: null,
    },
  },
  {
    completed: false,
    isCorrect: false,
    image: {
      correctAnswer: 'manipulert',
      selected: false,
      selectedAnswer: null,
      reasons: [
        { text: 'Noe i bildet er endret eller lagt til', correct: true },
        { text: 'Bildet er litt uklart', correct: false },
      ],
      selectedReason: null,
    },
  },
  {
    completed: false,
    isCorrect: false,
    image: {
      correctAnswer: 'ki',
      selected: false,
      selectedAnswer: null,
      reasons: [
        { text: 'Detaljene ser unaturlige ut', correct: true },
        { text: 'Bildet har høy kvalitet', correct: false },
      ],
      selectedReason: null,
    },
  },
])

const currentLevelData = computed(() => {
  return levels.value[currentLevel.value] ?? null
})

const earnedStars = computed(() => {
  return levels.value.filter((level) => level.isCorrect).length
})

function selectAnswer(answer: AnswerOption) {
  const level = currentLevelData.value
  if (!level || level.completed) return

  level.image.selected = true
  level.image.selectedAnswer = answer
  level.image.selectedReason = null
}

function selectReason(reason: Reason) {
  const level = currentLevelData.value
  if (!level || level.completed) return
  if (!level.image.selectedAnswer) return

  level.image.selectedReason = reason
}

function canSubmitLevel() {
  const level = currentLevelData.value
  if (!level) return false

  return level.image.selectedAnswer !== null && level.image.selectedReason !== null
}

function submitLevel() {
  const level = currentLevelData.value
  if (!level || level.completed) return

  const isCorrect =
    level.image.selectedAnswer === level.image.correctAnswer &&
    level.image.selectedReason?.correct === true

  level.completed = true
  level.isCorrect = isCorrect
}

function nextLevel() {
  const level = currentLevelData.value
  if (!level || !level.completed) return

  if (currentLevel.value < levels.value.length - 1) {
    currentLevel.value++
    return
  }

  localStorage.setItem('fotografenStars', JSON.stringify(earnedStars.value))
  router.push('/pupil/classroom/map')
}

function prevLevel() {
  if (currentLevel.value > 0) {
    currentLevel.value--
  }
}

function getCorrectAnswerText(image: ImageTask) {
  return image.correctAnswer === 'ki'
    ? 'KI-generert'
    : image.correctAnswer.charAt(0).toUpperCase() + image.correctAnswer.slice(1)
}

function getCorrectReasonText(image: ImageTask) {
  const correctReason = image.reasons.find((reason) => reason.correct)
  return correctReason?.text ?? ''
}
</script>

<style scoped>
.page {
  background: #e6c8a8;
  min-height: 100vh;
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

.task-text {
  text-align: center;
  margin: 0 0 24px 0;
  font-size: 20px;
}

.image-card {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.image-placeholder {
  width: 460px;
  height: 280px;
  background: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  margin-bottom: 20px;
  border: 2px solid #999;
  font-size: 20px;
  font-weight: 600;
}

.choices {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.choices button {
  padding: 10px 16px;
  border-radius: 20px;
  border: 2px solid #333;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.choices button:hover {
  background: #f4a259;
  transform: scale(1.05);
}

.choices button.selected {
  background: #f4a259;
  border-color: #000;
  transform: scale(1.08);
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.2);
}

.reason-box {
  margin-top: 10px;
  text-align: center;
}

.reason-box p {
  margin-bottom: 10px;
  font-weight: 600;
}

.reason-box button {
  margin: 5px;
  padding: 8px 14px;
  border-radius: 16px;
  border: 2px solid #555;
  background: #eee;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reason-box button:hover {
  background: #ddd;
}

.reason-box button.selected {
  background: #90caf9;
  border-color: #1976d2;
  transform: scale(1.05);
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

.bottom-actions button {
  background: #f4a259;
  border: 2px solid #000;
  border-radius: 20px;
  padding: 10px 18px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.bottom-actions button:hover {
  background: #e38b3d;
}

.correct {
  background: #a5d6a7 !important;
  border-color: #2e7d32 !important;
}

.wrong {
  background: #ef9a9a !important;
  border-color: #c62828 !important;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
