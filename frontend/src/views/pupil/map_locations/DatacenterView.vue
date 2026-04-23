<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { useRouter } from "vue-router";
import TopBar from "@/components/TopBar.vue";
import TaskGuide from "@/components/TaskGuide.vue";
import VictoryImage from "@/assets/Victory.png";
import DefeatImage from "@/assets/Defeat.png";
import victorySoundFile from "@/assets/victory-sound.mp3";
import defeatSoundFile from "@/assets/evil-laugh.mp3";
import confetti from "canvas-confetti";

type TaskPicture = {
  id: number;
  label: string;
  imageUrl?: string;
};

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

const pictures: TaskPicture[] = [
  {
    id: 1,
    label: "Bilde 1",
    imageUrl: "",
  },
  {
    id: 2,
    label: "Bilde 2",
    imageUrl: "",
  },
];

const resultImage = computed(() =>
  resultType.value === "victory" ? VictoryImage : DefeatImage,
);

const resultAltText = computed(() =>
  resultType.value === "victory" ? "Seier" : "Tap",
);

const resultKicker = computed(() =>
  resultType.value === "victory" ? "Oppdrag fullført" : "Oppdrag mislyktes",
);

const resultTitle = computed(() =>
  resultType.value === "victory" ? "Du klarte det!" : "Du tapte!",
);

const resultMessage = computed(() =>
  resultType.value === "victory"
    ? "Gratulerer! Du har fullført alle nivåene i spillet og slått datatyven."
    : "Å nei! Du fikk ikke til alle oppgavene og datatyven slapp unna denne gangen.",
);

const resultSubmessage = computed(() =>
  resultType.value === "victory"
    ? "Nå har du vist at du kan tenke som en ekte nett-detektiv!"
    : "Men ikke gi opp – prøv igjen og se om du klarer å slå datatyven neste gang!",
);

const resultHighlightTitle = computed(() =>
  resultType.value === "victory" ? "🏆 Spill fullført" : "💥 Prøv igjen",
);

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

function chooseFakePicture(picture: TaskPicture) {
  console.log(`Valgte ${picture.label} som falsk`);

  const allLevelsCompleted = true;

  if (allLevelsCompleted) {
    openResultPopup("victory");
  } else {
    openResultPopup("defeat");
  }
}

function openHelpPopup(): void {
  showHelpPopup.value = true;
}

function closeHelpPopup(): void {
  showHelpPopup.value = false;
}

function openHintPopup(): void {
  showHintPopup.value = true;
}

function closeHintPopup(): void {
  showHintPopup.value = false;
}

function openResultPopup(type: ResultType): void {
  resultType.value = type;
  showResultPopup.value = true;
}

function closeResultPopup(): void {
  showResultPopup.value = false;
  resultType.value = null;
  stopResultSounds();
}

function goBackToMap(): void {
  closeResultPopup();
  router.push("/pupil/classroom/map");
}
</script>

<template>
  <TopBar
    title="Datasenteret"
    @help="openHelpPopup"
    @hint="openHintPopup"
  />
  <div class="page">
    <div class="center-container">
      <h2>Aktivitet 1</h2>
      <div class="task-wrapper">
        <p>To avisforsider ligger foran deg.</p>
        <p>Én er ekte og én er falsk.</p>
        <p>Kan du finne ut hvilken som er falsk?</p>
      </div>

      <div class="picture-wrapper">
        <div
          v-for="picture in pictures"
          :key="picture.id"
          class="picture-card"
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
          <button
            class="fake-button"
            @click="chooseFakePicture(picture)"
          >
            Dette er den falske
          </button>
        </div>
      </div>

      <TaskGuide v-if="showGuidePopup" @close="showGuidePopup = false">
        <h2>Velkommen til datasenteret!</h2>
        <p>
          I datasenteret vil du få en blanding av oppgaver fra alle de tidligere
          nivåene. Dette er det siste nivået du må gjennomføre for å bekjempe
          datatyven og gjennomføre spillet. Er du klar for å løse oppgaver, og
          slå datatyven en gang for alle?
        </p>
      </TaskGuide>

      <div v-if="showHelpPopup" class="popup-overlay" @click="closeHelpPopup">
        <div class="hint-popup" @click.stop>
          <h2 class="popup-title">Trenger du hjelp?</h2>

          <div class="hint-text">
            <p>
              Inne på denne siden skal du løse oppgaver for å bekjempe
              datatyven.
            </p>
            <ul>
              <li>Begynn med oppgave 1</li>
              <li>Fullfør alle oppgavene korrekt og slå bossen.</li>
              <li>Trykk på tilbake for å gå tilbake til hovedskjermen.</li>
            </ul>
          </div>

          <div class="popup-actions">
            <button class="popup-btn popup-btn-play" @click="closeHelpPopup">
              Ok
            </button>
          </div>
        </div>
      </div>

      <div v-if="showHintPopup" class="popup-overlay" @click="closeHintPopup">
        <div class="hint-popup" @click.stop>
          <h2 class="popup-title">Tips 💡</h2>

          <div class="hint-text">
            <p>
              Les oppgavene nøye før du svarer.
            </p>
            <ul>
              <li>Se etter små detaljer som kan avsløre riktig svar</li>
              <li>Ta deg god tid – det er lov å tenke seg om</li>
              <li>Hvis du er usikker, prøv deg frem!</li>
            </ul>
          </div>

          <div class="popup-actions">
            <button class="popup-btn popup-btn-play" @click="closeHintPopup">
              Ok
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="showResultPopup"
        class="victory-overlay"
        @click="closeResultPopup"
      >
        <div class="victory-popup" @click.stop>
          <div class="victory-top">
            <img
              :src="resultImage"
              :alt="resultAltText"
              class="victory-image"
            />
          </div>

          <div class="victory-content">
            <p class="victory-kicker">{{ resultKicker }}</p>
            <h2 class="victory-title">{{ resultTitle }}</h2>
            <p class="victory-message">
              {{ resultMessage }}
            </p>
            <p class="victory-submessage">
              {{ resultSubmessage }}
            </p>

            <div class="victory-highlight">
              <p class="victory-highlight-title">{{ resultHighlightTitle }}</p>
              <p>
                {{ resultHighlightText }}
              </p>
            </div>

            <div class="victory-actions">
              <button class="victory-close-button" @click="goBackToMap">
                Tilbake til kartet
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0;
}

.page {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
}

.center-container {
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 1100px;
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
}

.picture-placeholder {
  width: min(100%, 420px);
  aspect-ratio: 420 / 320;
  background-color: var(--beige-light);
  border: 2px solid var(--brown-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.picture {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fake-button {
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--orange-medium);
  font-weight: 700;
  cursor: pointer;
  transition:
    background-color 0.2s ease,
    transform 0.2s ease,
    filter 0.2s ease;
}

.fake-button:hover {
  filter: brightness(1.1);
  transform: scale(1.02);
}

.fake-button:active {
  transform: scale(0.97);
}

.task-wrapper {
  margin: 0;
  width: min(100%, 800px);
  padding: 1rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
}

.task-wrapper p {
  margin: 0;
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

/* Result popup */

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
  background:
    linear-gradient(
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
  background: var(--green-confirm);
}

.victory-close-button:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
}

.victory-close-button:active {
  transform: translateY(0);
}

@media (max-width: 768px) {
  .page {
    padding: 1rem;
  }

  .picture-wrapper {
    gap: 1rem;
  }

  .task-wrapper {
    padding: 1rem;
    border-width: 3px;
    border-radius: 24px;
  }

  .fake-button {
    border-width: 3px;
    border-radius: 24px;
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

  .victory-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .victory-close-button {
    width: 100%;
    min-width: 0;
  }
}
</style>
