<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRouter } from "vue-router";
import TopBar from "@/components/TopBar.vue";
import DatatyvenImage from "@/assets/Datatyven.png";
import DefeatImage from "@/assets/Defeat.png";
import VictoryImage from '@/assets/Victory.png'
import victorySoundFile from "@/assets/victory-sound.mp3";
import defeatSoundFile from "@/assets/evil-laugh.mp3";
import confetti from "canvas-confetti";

type ResultType = "victory" | "defeat" | null;

const router = useRouter();

const victorySound = new Audio(victorySoundFile);
victorySound.volume = 0.3;

const defeatSound = new Audio(defeatSoundFile);
defeatSound.volume = 0.3;

const showHelpPopup = ref<boolean>(false);
const showHintPopup = ref<boolean>(false);
const showGuidePopup = ref(true);
const showResultPopup = ref(false);
const resultType = ref<ResultType>(null);


type TaskOption = {
  id: number
  text: string
}

type Task = {
  id: number
  title: string
  description: string
  options: TaskOption[]
  correctOptionId: number
  selectedOptionId: number | null
  isAnswered: boolean
  isCorrect: boolean | null
}

type Level = {
  id: number
  intro: string
  task: Task
}

const currentLevelIndex = ref<number>(0)

const levels = ref<Level[]>([

  {
    id: 1,
    intro: 'Her møter du den første utfordringen fra datatyven.',
    task: {
      id: 1,
      title: 'Oppgave 1',
      description: 'Legg inn oppgaven for nivå 1 her.',
      options: [
        { id: 1, text: 'Svaralternativ A' },
        { id: 2, text: 'Svaralternativ B' },
        { id: 3, text: 'Svaralternativ C' },
      ],
      correctOptionId: 2,
      selectedOptionId: null,
      isAnswered: false,
      isCorrect: null,
    },
  },
  {
    id: 2,
    intro: 'Datatyven prøver å lure deg. Klarer du neste oppgave?',
    task: {
      id: 2,
      title: 'Oppgave 2',
      description: 'Legg inn oppgaven for nivå 2 her.',
      options: [
        { id: 1, text: 'Svaralternativ A' },
        { id: 2, text: 'Svaralternativ B' },
        { id: 3, text: 'Svaralternativ C' },
      ],
      correctOptionId: 1,
      selectedOptionId: null,
      isAnswered: false,
      isCorrect: null,
    },
  },
  {
    id: 3,
    intro: 'Nå blir det vanskeligere. Tenk nøye før du svarer.',
    task: {
      id: 3,
      title: 'Oppgave 3',
      description: 'Legg inn oppgaven for nivå 3 her.',
      options: [
        { id: 1, text: 'Svaralternativ A' },
        { id: 2, text: 'Svaralternativ B' },
        { id: 3, text: 'Svaralternativ C' },
      ],
      correctOptionId: 3,
      selectedOptionId: null,
      isAnswered: false,
      isCorrect: null,
    },
  },
  {
    id: 4,
    intro: 'Du nærmer deg slutten. Nå gjelder det å holde fokus.',
    task: {
      id: 4,
      title: 'Oppgave 4',
      description: 'Legg inn oppgaven for nivå 4 her.',
      options: [
        { id: 1, text: 'Svaralternativ A' },
        { id: 2, text: 'Svaralternativ B' },
        { id: 3, text: 'Svaralternativ C' },
      ],
      correctOptionId: 2,
      selectedOptionId: null,
      isAnswered: false,
      isCorrect: null,
    },
  },
  {
    id: 5,
    intro: 'Siste nivå! Nå avgjøres det om du slår datatyven.',
    task: {
      id: 5,
      title: 'Oppgave 5',
      description: 'Legg inn oppgaven for nivå 5 her.',
      options: [
        { id: 1, text: 'Svaralternativ A' },
        { id: 2, text: 'Svaralternativ B' },
        { id: 3, text: 'Svaralternativ C' },
      ],
      correctOptionId: 1,
      selectedOptionId: null,
      isAnswered: false,
      isCorrect: null,
    },
  },
])

const currentLevel = computed(() => levels.value[currentLevelIndex.value] as Level)

const isCurrentLevelComplete = computed<boolean>(() => {
  return currentLevel.value.task.isAnswered
})

const isLastLevel = computed<boolean>(() => {
  return currentLevelIndex.value === levels.value.length - 1
})

const allAnswersCorrect = computed<boolean>(() => {
  return levels.value.every((level) => level.task.isCorrect === true)
})

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
    ? 'Gratulerer! Du svarte riktig på alle nivåene og slo datatyven.'
    : 'Å nei! Du svarte feil på én eller flere oppgaver, og datatyven slapp unna denne gangen.',
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
  resultType.value === "victory"
    ? "Du har kommet helt til toppen i Datasenteret."
    : "Du kan gå tilbake og prøve oppgavene på nytt for å få en ny sjanse til å fullføre spillet.",
);

function stopResultSounds(): void {
  victorySound.pause();
  defeatSound.pause();

  victorySound.currentTime = 0;
  defeatSound.currentTime = 0;
}

function fireVictoryConfetti(): void {
  const duration = 2000;
  const end = Date.now() + duration;

  const defaults = {
    zIndex: 3000,
  };

  const frame = () => {
    confetti({
      ...defaults,
      particleCount: 8,
      angle: 60,
      spread: 60,
      origin: { x: 0, y: 0.6 },
    });

    confetti({
      ...defaults,
      particleCount: 8,
      angle: 120,
      spread: 60,
      origin: { x: 1, y: 0.6 },
    });

    if (Date.now() < end) {
      requestAnimationFrame(frame);
    }
  };

  frame();
}

watch(showResultPopup, (isOpen) => {
  if (!isOpen) {
    stopResultSounds();
    return;
  }

  stopResultSounds();

  if (resultType.value === "victory") {
    victorySound.play();
    fireVictoryConfetti();
  } else if (resultType.value === "defeat") {
    defeatSound.play();
  }
});

function selectOption(optionId: number): void {
  const task = currentLevel.value.task

  if (task.isAnswered) return

  task.selectedOptionId = optionId
}

function submitTask(): void {
  const task = currentLevel.value.task

  if (task.isAnswered || task.selectedOptionId === null) return

  task.isAnswered = true
  task.isCorrect = task.selectedOptionId === task.correctOptionId
}

function goToNextLevel(): void {
  if (!isCurrentLevelComplete.value || isLastLevel.value) return

  currentLevelIndex.value += 1
}

function goToPreviousLevel(): void {
  if (currentLevelIndex.value === 0) return


  currentLevelIndex.value -= 1
}

function finishGame(): void {
  if (!isCurrentLevelComplete.value) return

  if (allAnswersCorrect.value) {
    openResultPopup('victory')
  } else {
    openResultPopup('defeat')
  }
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

function openResultPopup(type: ResultType): void {
  resultType.value = type
  showResultPopup.value = true
}

function closeResultPopup(): void {
  showResultPopup.value = false
  resultType.value = null

  victorySound.pause()
  defeatSound.pause()

  victorySound.currentTime = 0
  defeatSound.currentTime = 0
}

function goBackToMap(): void {
  closeResultPopup()
  void router.push('/pupil/classroom/map')
}

function resetGame(): void {
  levels.value.forEach((level) => {
    level.task.selectedOptionId = null
    level.task.isAnswered = false
    level.task.isCorrect = null
  })

  currentLevelIndex.value = 0
  closeResultPopup()
  showGuidePopup.value = true
}
</script>

<template>
  <TopBar title="Datasenteret" @help="openHelpPopup" @hint="openHintPopup" />

  <div class="page">
    <div class="center-container">
      <div class="progress-box">
        <p>Nivå {{ currentLevel.id }}/{{ levels.length }}</p>
      </div>

      <div class="task-wrapper">
        <p>{{ currentLevel.intro }}</p>
      </div>

      <div
        class="task-card"
        :class="{
          correct: currentLevel.task.isAnswered && currentLevel.task.isCorrect,
          wrong: currentLevel.task.isAnswered && currentLevel.task.isCorrect === false,
        }"
      >
        <h2>{{ currentLevel.task.title }}</h2>

        <p class="task-description">
          {{ currentLevel.task.description }}
        </p>

        <div class="options-list">
          <button
            v-for="option in currentLevel.task.options"
            :key="option.id"
            class="option-button"
            :class="{
              selected: currentLevel.task.selectedOptionId === option.id,
            }"
            :disabled="currentLevel.task.isAnswered"
            @click="selectOption(option.id)"
          >
            {{ option.text }}
          </button>
        </div>

        <button
          class="submit-button"
          :disabled="currentLevel.task.selectedOptionId === null || currentLevel.task.isAnswered"
          @click="submitTask"
        >
          Svar
        </button>

        <div
          v-if="currentLevel.task.isAnswered"
          class="feedback-box"
          :class="currentLevel.task.isCorrect ? 'feedback-correct' : 'feedback-wrong'"
        >
          <p class="feedback-title">
            {{ currentLevel.task.isCorrect ? 'Riktig!' : 'Feil!' }}
          </p>
          <p>
            {{
              currentLevel.task.isCorrect
                ? 'Bra jobbet! Du løste denne oppgaven.'
                : 'Dette svaret var ikke riktig.'
            }}
          </p>
        </div>
      </div>

      <div class="navigation-bar">
        <button
          class="nav-button secondary"
          :disabled="currentLevelIndex === 0"
          @click="goToPreviousLevel"
        >
          Forrige
        </button>

        <button
          v-if="!isLastLevel"
          class="nav-button primary"
          :disabled="!isCurrentLevelComplete"
          @click="goToNextLevel"
        >
          Neste
        </button>

        <button
          v-else
          class="nav-button primary"
          :disabled="!isCurrentLevelComplete"
          @click="finishGame"
        >
          Fullfør
        </button>
      </div>

      <div v-if="showGuidePopup" class="intro-overlay">
        <div class="intro-popup">
          <h2 class="intro-title">Velkommen til datasenteret!</h2>

          <p class="intro-text">Klarer du å svare riktig på alle oppgavene og slå datatyven?</p>

          <img :src="DatatyvenImage" alt="Datatyven" class="intro-image" />

          <button class="intro-button" @click="showGuidePopup = false">Jeg er klar!</button>
        </div>
      </div>

      <div v-if="showHelpPopup" class="popup-overlay" @click="closeHelpPopup">
        <div class="hint-popup" @click.stop>
          <h2 class="popup-title">Trenger du hjelp?</h2>

          <div class="hint-text">
            <p>Inne på denne siden skal du løse oppgaver for å bekjempe datatyven.</p>
            <ul>
              <li>Det er én oppgave på hvert nivå</li>
              <li>Du kan bare svare én gang på hver oppgave</li>
              <li>Du må svare riktig på alle nivåene for å vinne</li>
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
            <p>Les oppgaven nøye før du svarer.</p>
            <ul>
              <li>Se etter små detaljer som kan avsløre riktig svar</li>
              <li>Ta deg god tid – du kan ikke svare på nytt</li>
              <li>Bruk det du har lært tidligere i spillet</li>
            </ul>
          </div>

          <div class="popup-actions">
            <button class="popup-btn popup-btn-play" @click="closeHintPopup">Ok</button>
          </div>
        </div>
      </div>

      <div v-if="showResultPopup" class="victory-overlay" @click="closeResultPopup">
        <div class="victory-popup" @click.stop>
          <div class="victory-top">
            <img :src="resultImage" :alt="resultAltText" class="victory-image" />
          </div>

          <div class="victory-content">
            <p class="victory-kicker">{{ resultKicker }}</p>
            <h2 class="victory-title">{{ resultTitle }}</h2>
            <p class="victory-message">{{ resultMessage }}</p>
            <p class="victory-submessage">{{ resultSubmessage }}</p>

            <div v-if="resultType === 'victory'" class="victory-highlight">
              <p class="victory-highlight-title">{{ resultHighlightTitle }}</p>
              <p>{{ resultHighlightText }}</p>
            </div>

            <div class="victory-actions">
              <button v-if="resultType === 'defeat'" class="retry-button" @click="resetGame">
                Prøv på nytt
              </button>

              <button class="victory-close-button" @click="goBackToMap">Tilbake til kartet</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2,
p {
  margin: 0;
}

.page {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem 3rem;
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 1000px;
  text-align: center;
}

.progress-box {
  width: min(100%, 700px);
  margin-bottom: 1.25rem;
  padding: 1rem 1.4rem;
  background: var(--beige-light);
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 700;
}

.task-wrapper {
  margin-bottom: 1.5rem;
  width: min(100%, 700px);
  padding: 1rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
}

.task-card {
  width: min(100%, 700px);
  background: #fff7e9;
  border: 5px solid var(--brown-dark);
  border-radius: 32px;
  padding: 1.75rem;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-sizing: border-box;
}

.task-card.correct {
  box-shadow: inset 0 0 0 3px rgb(63 156 89 / 25%);
}

.task-card.wrong {
  box-shadow: inset 0 0 0 3px rgb(194 71 71 / 25%);
}

.task-description {
  line-height: 1.5;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.option-button {
  width: 100%;
  padding: 0.95rem 1rem;
  border-radius: 20px;
  border: 4px solid var(--brown-dark);
  background: var(--beige-light);
  font-weight: 700;
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    background-color 0.2s ease;
}

.option-button:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.03);
}

.option-button.selected {
  background: #ffd68a;
}

.option-button:disabled {
  cursor: not-allowed;
  opacity: 0.88;
}

.submit-button {
  width: 100%;
  padding: 1rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: var(--orange-medium);
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    filter 0.2s ease;
}

.submit-button:hover:not(:disabled) {
  transform: scale(1.01);
  filter: brightness(1.05);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.feedback-box {
  padding: 1rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
}

.feedback-correct {
  background: #dff4d2;
}

.feedback-wrong {
  background: #ffd4cc;
}

.feedback-title {
  margin-bottom: 0.35rem;
  font-weight: 800;
}

.navigation-bar {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.nav-button {
  min-width: 200px;
  min-height: 68px;
  padding: 1rem 1.5rem;
  border-radius: 24px;
  border: 4px solid var(--brown-dark);
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    filter 0.2s ease;
}

.nav-button.primary {
  background: var(--green-confirm);
}

.nav-button.secondary {
  background: #ffe08a;
}

.nav-button:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.05);
}

.nav-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.intro-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 35%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3000;
}

.intro-popup {
  width: min(700px, 90vw);
  background: linear-gradient(180deg, #f5d7a8 0%, #e9b878 100%);
  border: 6px solid var(--brown-dark);
  border-radius: 32px;
  padding: 2rem 1.5rem 2rem;
  text-align: center;
  box-shadow: 0 25px 60px rgb(0 0 0 / 35%);
}

.intro-title {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.intro-text {
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
  font-weight: 600;
}

.intro-image {
  width: min(260px, 70%);
  margin: 0 auto 1.5rem;
  display: block;
  object-fit: contain;
}

.intro-button {
  min-width: 220px;
  padding: 1rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: #7ed321;
  font-weight: 800;
  font-size: 1.2rem;
  cursor: pointer;
  box-shadow: 0 6px 14px rgb(0 0 0 / 25%);
  transition: transform 0.2s ease;
}

.intro-button:hover {
  transform: scale(1.05);
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
  width: min(800px, calc(100vw - 32px));
  background: var(--orange-medium-80);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 40px 32px 28px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.popup-title {
  margin: 0 0 16px;
}

.hint-text {
  margin-top: 16px;
  text-align: center;
}

.hint-text p {
  margin: 0 0 14px;
}

.hint-text ul {
  margin: 0;
  padding-left: 24px;
}

.hint-text li {
  margin-bottom: 6px;
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
  padding: 18px 28px;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
}

.popup-btn-play {
  background: var(--green-confirm);
}

.victory-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 32%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.victory-popup {
  width: min(980px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  background: linear-gradient(
    180deg,
    rgb(255 214 120 / 95%) 0%,
    rgb(245 170 92 / 96%) 18%,
    rgb(232 155 86 / 98%) 100%
  );
  border: 6px solid var(--brown-dark);
  border-radius: 40px;
  box-shadow:
    0 22px 60px rgb(0 0 0 / 30%),
    inset 0 3px 0 rgb(255 255 255 / 28%);
  text-align: center;
  position: relative;
}

.victory-top {
  padding: 3rem 2rem 1.5rem;
}

.victory-image {
  width: min(100%, 600px);
  max-height: 400px;
  object-fit: contain;
  filter: drop-shadow(0 12px 20px rgb(0 0 0 / 20%));
}

.victory-content {
  padding: 0 2.5rem 2.5rem;
}

.victory-kicker {
  margin: 0 0 0.5rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.85;
}

.victory-title {
  margin: 0;
  font-size: clamp(2rem, 5.5vw, 4.2rem);
  line-height: 1;
}

.victory-message {
  margin: 1rem auto 0;
  max-width: 700px;
}

.victory-submessage {
  margin: 0.75rem auto 0;
  max-width: 700px;
}

.victory-highlight {
  margin: 2rem auto 0;
  max-width: 700px;
  padding: 1.2rem 1.4rem;
  background: rgb(255 245 214 / 72%);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 8px 20px rgb(0 0 0 / 12%);
}

.victory-highlight-title {
  margin: 0 0 0.4rem;
  font-weight: 800;
}

.victory-highlight p:last-child {
  margin-bottom: 0;
}

.victory-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.retry-button,
.leaderboard-button,
.victory-close-button {
  min-width: 240px;
  min-height: 72px;
  padding: 1rem 1.4rem;
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  box-shadow: 0 8px 18px rgb(0 0 0 / 18%);
  transition:
    transform 0.2s ease,
    filter 0.2s ease;
}

.retry-button {
  background: #ffe08a;
}

.leaderboard-button {
  background: #ffe08a;
}

.victory-close-button {
  background: var(--green-confirm);
}

.retry-button:hover,
.leaderboard-button:hover,

.victory-close-button:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
}


.retry-button:active,
.leaderboard-button:active,

.victory-close-button:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .page {
    padding: 1rem;
  }

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

  .option-button,
  .submit-button,
  .nav-button,
  .intro-button,
  .retry-button,
  .leaderboard-button,
  .victory-close-button {
    border-width: 3px;
  }

  .navigation-bar,
  .victory-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .nav-button,
  .leaderboard-button,
  .victory-close-button,
  .retry-button,
  .intro-button {
    width: 100%;
    min-width: 0;
  }

  .intro-popup {
    padding: 1.5rem 1rem;
    border-width: 4px;
    border-radius: 26px;
  }

  .victory-popup {
    border-width: 4px;
    border-radius: 28px;
  }

  .victory-top {
    padding: 1.5rem 1rem 0.75rem;
  }

  .victory-content {
    padding: 0 1rem 1.5rem;
  }

  .victory-close-button {
    width: 100%;
    min-width: 0;
  }

}
</style>
