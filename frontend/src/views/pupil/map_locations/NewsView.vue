<script setup lang="ts">
import TopBar from "@/components/TopBar.vue";
import { ref } from "vue";

const showHelpPopup = ref<boolean>(false);
const showHintPopup = ref<boolean>(false);

type TaskPicture = {
  id: number;
  label: string;
  imageUrl?: string;
};

// Temporary placeholder-data, change later when connected to task bank
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

function chooseFakePicture(picture: TaskPicture) {
  console.log(`Valgte ${picture.label} som falsk`);
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
</script>

<template>
  <TopBar
    title="Nyhetskvartalet"
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
    </div>
  </div>

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
          Tenk som en detektiv.
        </p>
        <ul>
          <li>Se etter detaljer som ikke passer inn</li>
          <li>Vær ekstra obs på tekst, bilder og overskrifter</li>
          <li>Det som virker litt rart, er ofte verdt å undersøke nærmere</li>
        </ul>
      </div>

      <div class="popup-actions">
        <button class="popup-btn popup-btn-play" @click="closeHintPopup">
          Ok
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 2.5rem);
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
  font-size: clamp(1rem, 2vw, 2rem);
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
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, filter 0.2s ease;
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
  font-size: clamp(1rem, 2.5vw, 2rem);
  padding: 1rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
}

.task-wrapper p {
  margin: 0;
}

/**Scaling for smaller screens*/
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
  font-size: 2rem;
  line-height: 1.15;
  font-weight: 800;
  color: var(--color-text);
}

.hint-text {
  margin-top: 10px;
  font-size: 1.1rem;
  line-height: 1.35;
  font-weight: 700;
  color: var(--color-text);
  text-align: left;
}

.hint-text p {
  margin: 0 0 8px;
  font-size: 1.2rem;
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
  min-width: 130px;
  padding: 14px 24px;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text);
  cursor: pointer;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
}

.popup-btn-play {
  background: var(--green-confirm);
}
</style>
