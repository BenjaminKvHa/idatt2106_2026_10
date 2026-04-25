<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

import TopBar from '@/components/TopBar.vue'
import TaskGuide from '@/components/TaskGuide.vue'
import CompleteLevelPopup from '@/components/CompleteLevelPopup.vue'

import thumbsUpNews from '@/assets/ThumbsUpNews.png'
import thumbsDownNews from '@/assets/ThumbsDownNews.png'

const router = useRouter()

const showHelpPopup = ref(false)
const showHintPopup = ref(false)
const showGuidePopup = ref(true)
const showCompletePopup = ref(false)

const selectedPictureId = ref<number | null>(null)
const currentLevel = ref(0)
const wrongAnswers = ref(0)

const feedbackText = ref('')
const feedbackSuccess = ref(true)

const popupTitle = ref('')
const popupMessage = ref('')
const popupSubMessage = ref('')
const popupSuccess = ref(true)

type TaskPicture = {
  id: number
  label: string
  imageUrl?: string
}

type Level = {
  text: string[]
  pictures: TaskPicture[]
  correctFakeId: number
}

const levels: [Level, ...Level[]] = [
  {
    text: [
      'To avisforsider ligger foran deg.',
      'Én er ekte og én er falsk.',
      'Kan du finne ut hvilken som er falsk?',
    ],
    correctFakeId: 2,
    pictures: [
      { id: 1, label: 'Bilde 1', imageUrl: '' },
      { id: 2, label: 'Bilde 2', imageUrl: '' },
    ],
  },
  {
    text: [
      'Se nøye på de to avisforsidene.',
      'Hvilken av dem ser minst troverdig ut?',
      'Velg den du tror er falsk.',
    ],
    correctFakeId: 1,
    pictures: [
      { id: 1, label: 'Bilde 1', imageUrl: '' },
      { id: 2, label: 'Bilde 2', imageUrl: '' },
    ],
  },
  {
    text: [
      'Dette er siste nivå.',
      'Bruk det du har lært om ekte og falske nyheter.',
      'Finn den falske avisforsiden.',
    ],
    correctFakeId: 2,
    pictures: [
      { id: 1, label: 'Bilde 1', imageUrl: '' },
      { id: 2, label: 'Bilde 2', imageUrl: '' },
    ],
  },
]

const currentLevelData = computed<Level>(() => {
  return levels[currentLevel.value] ?? levels[0]
})

const isLastLevel = computed(() => currentLevel.value === levels.length - 1)

const progressPercent = computed(() => {
  return ((currentLevel.value + 1) / levels.length) * 100
})

function chooseFakePicture(picture: TaskPicture): void {
  if (selectedPictureId.value !== null) {
    return
  }

  selectedPictureId.value = picture.id

  const answeredCorrectly = picture.id === currentLevelData.value.correctFakeId

  feedbackSuccess.value = answeredCorrectly
  feedbackText.value = answeredCorrectly
    ? 'Riktig! Du fant den falske avisforsiden.'
    : 'Feil. Dette var ikke den falske avisforsiden.'

  if (!answeredCorrectly) {
    wrongAnswers.value += 1
  }
}

function goToNextLevel(): void {
  if (isLastLevel.value) {
    showFinalResult()
    return
  }

  currentLevel.value += 1
  selectedPictureId.value = null
  feedbackText.value = ''
}

function showFinalResult(): void {
  const allCorrect = wrongAnswers.value === 0

  popupSuccess.value = allCorrect
  popupTitle.value = allCorrect ? 'Du klarte det!' : 'Du klarte det ikke'
  popupMessage.value = allCorrect
    ? 'Gratulerer! Du klarte alle nivåene i Nyhetskvartalet.'
    : 'Du må ha rett på alle oppgavene for å bestå nyhetskvartalet.'
  popupSubMessage.value = allCorrect
    ? 'Du har nå fullført stoppestedet.'
    : "Trykk 'prøv på nytt' for å prøve igjen, eller gå tilbake til kartet."

  showCompletePopup.value = true
}

function retryLevel(): void {
  currentLevel.value = 0
  wrongAnswers.value = 0
  selectedPictureId.value = null
  feedbackText.value = ''
  showCompletePopup.value = false
}

function goToMap(): void {
  router.push('/pupil/classroom/map')
}

function closeCompletePopup(): void {
  showCompletePopup.value = false
}

function openHelpPopup(): void {
  showHelpPopup.value = true
}

function closeHelpPopup(): void {
  showHelpPopup.value = false
}

function openHintPopup(): void {
  showHintPopup.value = true
}

function closeHintPopup(): void {
  showHintPopup.value = false
}
</script>

<template>
  <TopBar title="Nyhetskvartalet" @help="openHelpPopup" @hint="openHintPopup" />

  <div class="page">
    <div class="center-container">
      <div class="task-wrapper">
        <p v-for="line in currentLevelData.text" :key="line">
          {{ line }}
        </p>
      </div>

      <div class="progress-container">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>

        <p class="progress-text">Nivå {{ currentLevel + 1 }} / {{ levels.length }}</p>
      </div>

      <div
        v-if="feedbackText"
        class="mini-feedback"
        :class="{ success: feedbackSuccess, fail: !feedbackSuccess }"
      >
        {{ feedbackText }}
      </div>

      <div class="picture-wrapper">
        <div
          v-for="picture in currentLevelData.pictures"
          :key="picture.id"
          class="picture-card"
          :class="{
            correct: selectedPictureId !== null && picture.id === currentLevelData.correctFakeId,
            wrong:
              selectedPictureId !== null &&
              selectedPictureId === picture.id &&
              picture.id !== currentLevelData.correctFakeId,
          }"
        >
          <div class="picture-placeholder">
            <img
              v-if="picture.imageUrl"
              :src="picture.imageUrl"
              :alt="picture.label"
              class="picture"
            />
            <span v-else>{{ picture.label }}</span>
          </div>

          <p
            v-if="selectedPictureId !== null && picture.id === currentLevelData.correctFakeId"
            class="answer-label correct-label"
          >
            Riktig svar
          </p>

          <p
            v-else-if="
              selectedPictureId !== null &&
              selectedPictureId === picture.id &&
              picture.id !== currentLevelData.correctFakeId
            "
            class="answer-label wrong-label"
          >
            Ditt valg
          </p>

          <button
            class="fake-button"
            :disabled="selectedPictureId !== null"
            @click="chooseFakePicture(picture)"
          >
            Velg dette bildet
          </button>
        </div>
      </div>

      <button v-if="selectedPictureId !== null" class="next-level-button" @click="goToNextLevel">
        {{ isLastLevel ? 'Se resultat' : 'Gå til neste nivå' }}
      </button>
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
  />

  <div v-if="showCompletePopup && !popupSuccess" class="popup-button-layer">
    <button class="popup-btn popup-btn-play" @click="retryLevel">Prøv på nytt</button>

    <button class="popup-btn popup-btn-map" @click="goToMap">Tilbake til kartet</button>
  </div>
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
  margin-top: 1.5rem;
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
  margin-top: 1rem;
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
  margin-top: 2rem;
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
  padding: 1rem;
  border-radius: 30px;
  background: rgb(255 255 255 / 35%);
  border: 3px solid rgb(91 62 43 / 25%);
  box-shadow: 0 10px 20px rgb(0 0 0 / 10%);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.picture-card:hover {
  transform: translateY(-4px);
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
  width: min(100%, 420px);
  aspect-ratio: 420 / 320;
  background-color: var(--beige-light);
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  overflow: hidden;
}

.picture {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

.next-level-button {
  margin-top: 2rem;
  max-width: 360px;
  background: var(--green-confirm);
}

.fake-button:hover,
.next-level-button:hover {
  filter: brightness(1.1);
  transform: translateY(-3px);
  box-shadow: 0 9px 0 var(--brown-dark);
}

.fake-button:active,
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

.popup-actions,
.popup-button-layer {
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
}

.popup-actions {
  margin-top: 26px;
}

.popup-button-layer {
  position: fixed;
  left: 50%;
  bottom: 16vh;
  transform: translateX(-50%);
  z-index: 2000;
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

  .popup-button-layer {
    bottom: 8vh;
    width: calc(100vw - 32px);
  }
}
</style>
