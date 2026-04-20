<template>
  <div class="page">
    <TopBar title="Fotografen" />

    <div class="content">
      <h1 class="level">Nivå {{ currentLevel + 1 }}</h1>

      <div class="task-box">
        <p>Velg om bildet er ekte, KI-generert eller manipulert. Velg deretter en begrunnelse.</p>
      </div>

      <div v-if="currentLevelData" class="images-box">
        <div v-for="(image, index) in currentLevelData.images" :key="index" class="image-card">
          <!-- Midlertidig plassholder -->
          <div class="image-placeholder">Bilde {{ index + 1 }}</div>

          <div class="choices">
            <button
              @click="selectAnswer(index, 'ekte')"
              :disabled="currentLevelData.completed"
              :class="[
                { selected: image.selectedAnswer === 'ekte' },
                currentLevelData.completed && image.correctAnswer === 'ekte' ? 'correct' : '',
                currentLevelData.completed &&
                image.selectedAnswer === 'ekte' &&
                image.correctAnswer !== 'ekte'
                  ? 'wrong'
                  : '',
              ]"
            >
              Ekte
            </button>

            <button
              @click="selectAnswer(index, 'ki')"
              :disabled="currentLevelData.completed"
              :class="[
                { selected: image.selectedAnswer === 'ki' },
                currentLevelData.completed && image.correctAnswer === 'ki' ? 'correct' : '',
                currentLevelData.completed &&
                image.selectedAnswer === 'ki' &&
                image.correctAnswer !== 'ki'
                  ? 'wrong'
                  : '',
              ]"
            >
              KI-generert
            </button>

            <button
              @click="selectAnswer(index, 'manipulert')"
              :disabled="currentLevelData.completed"
              :class="[
                { selected: image.selectedAnswer === 'manipulert' },
                currentLevelData.completed && image.correctAnswer === 'manipulert' ? 'correct' : '',
                currentLevelData.completed &&
                image.selectedAnswer === 'manipulert' &&
                image.correctAnswer !== 'manipulert'
                  ? 'wrong'
                  : '',
              ]"
            >
              Manipulert
            </button>
          </div>

          <div v-if="image.selected" class="reason-box">
            <p>Hvorfor?</p>

            <button
              v-for="reason in image.reasons"
              :key="reason.text"
              @click="selectReason(index, reason)"
              :disabled="currentLevelData.completed"
              :class="{ selected: image.selectedReason?.text === reason.text }"
            >
              {{ reason.text }}
            </button>
          </div>

          <div v-if="currentLevelData.completed" class="answer-feedback">
            <p><strong>Riktig svar:</strong> {{ getCorrectAnswerText(image) }}</p>
            <p><strong>Riktig begrunnelse:</strong> {{ getCorrectReasonText(image) }}</p>
          </div>
        </div>
      </div>

      <div v-if="currentLevelData" class="bottom-actions">
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import TopBar from '@/components/TopBar.vue'

const router = useRouter()

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
  images: ImageTask[]
  completed: boolean
  isCorrect: boolean
}

const currentLevel = ref(0)

const levels = ref<Level[]>([
  {
    completed: false,
    isCorrect: false,
    images: [
      {
        correctAnswer: 'ekte',
        selected: false,
        selectedAnswer: null,
        reasons: [
          { text: 'Ser naturlig ut', correct: true },
          { text: 'Rare skygger og detaljer', correct: false },
        ],
        selectedReason: null,
      },
      {
        correctAnswer: 'ki',
        selected: false,
        selectedAnswer: null,
        reasons: [
          { text: 'Unaturlige detaljer i bildet', correct: true },
          { text: 'Bildet har klare farger', correct: false },
        ],
        selectedReason: null,
      },
    ],
  },
  {
    completed: false,
    isCorrect: false,
    images: [
      {
        correctAnswer: 'manipulert',
        selected: false,
        selectedAnswer: null,
        reasons: [
          { text: 'Noe i bildet er endret eller lagt til', correct: true },
          { text: 'Bildet er litt uklart', correct: false },
        ],
        selectedReason: null,
      },
      {
        correctAnswer: 'ekte',
        selected: false,
        selectedAnswer: null,
        reasons: [
          { text: 'Lys, skygger og proporsjoner ser naturlige ut', correct: true },
          { text: 'Bildet ser profesjonelt ut', correct: false },
        ],
        selectedReason: null,
      },
    ],
  },
  {
    completed: false,
    isCorrect: false,
    images: [
      {
        correctAnswer: 'ki',
        selected: false,
        selectedAnswer: null,
        reasons: [
          { text: 'Detaljene ser unaturlige ut', correct: true },
          { text: 'Bildet har høy kvalitet', correct: false },
        ],
        selectedReason: null,
      },
      {
        correctAnswer: 'manipulert',
        selected: false,
        selectedAnswer: null,
        reasons: [
          { text: 'Bildet har blitt redigert i etterkant', correct: true },
          { text: 'Fargene er sterke', correct: false },
        ],
        selectedReason: null,
      },
    ],
  },
])

const currentLevelData = computed(() => {
  return levels.value[currentLevel.value] ?? null
})

const earnedStars = computed(() => {
  return levels.value.filter((level) => level.isCorrect).length
})

function selectAnswer(index: number, answer: AnswerOption) {
  const level = currentLevelData.value
  if (!level || level.completed) return

  const image = level.images[index]
  if (!image) return

  image.selected = true
  image.selectedAnswer = answer
  image.selectedReason = null
}

function selectReason(index: number, reason: Reason) {
  const level = currentLevelData.value
  if (!level || level.completed) return

  const image = level.images[index]
  if (!image || !image.selectedAnswer) return

  image.selectedReason = reason
}

function canSubmitLevel() {
  const level = currentLevelData.value
  if (!level) return false

  return level.images.every(
    (image) => image.selectedAnswer !== null && image.selectedReason !== null,
  )
}

function submitLevel() {
  const level = currentLevelData.value
  if (!level || level.completed) return

  const allCorrect = level.images.every(
    (image) =>
      image.selectedAnswer === image.correctAnswer && image.selectedReason?.correct === true,
  )

  level.completed = true
  level.isCorrect = allCorrect
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

/* Kun innhold sentreres */
.content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.title {
  text-align: center;
}

.level {
  text-align: center;
  margin-bottom: 10px;
}

.task-box {
  background: rgba(255, 255, 255, 0.8); /* hvit gjennomsiktig */
  padding: 15px 20px;
  border-radius: 12px;

  display: inline-block; /* gjør at den kun blir like bred som teksten */
  margin: 0 auto 20px auto; /* sentrerer boksen */
  text-align: center;

  max-width: 90%; /* hindrer at den blir for bred på små skjermer */
}

.images-box {
  display: flex;
  gap: 20px;
  justify-content: center;
  flex-wrap: wrap;
}

.image-card {
  background: white;
  padding: 10px;
  border-radius: 12px;
}

.image-placeholder {
  width: 250px;
  height: 150px;
  background: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  margin-bottom: 10px;
}

.choices button {
  margin: 5px;
}

.reason-box {
  margin-top: 10px;
}

.navigation {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
}

button {
  background: #f4a259;
  border: 2px solid #000;
  border-radius: 20px;
  padding: 8px 15px;
  cursor: pointer;
}

button:hover {
  background: #e38b3d;
}

.choices button {
  margin: 5px;
  padding: 10px 16px;
  border-radius: 20px;
  border: 2px solid #333;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

/* Hover */
.choices button:hover {
  background: #f4a259;
  transform: scale(1.05);
}

/* VALGT */
.choices button.selected {
  background: #f4a259;
  border-color: #000;
  transform: scale(1.1);
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.2);
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

/* RIKTIG */
.correct {
  background: #a5d6a7 !important;
  border-color: #2e7d32 !important;
}

/* FEIL */
.wrong {
  background: #ef9a9a !important;
  border-color: #c62828 !important;
}

/* Disabled */
button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
