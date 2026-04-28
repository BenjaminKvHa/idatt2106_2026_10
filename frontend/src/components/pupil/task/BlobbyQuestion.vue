<script setup lang="ts">
import { ref } from 'vue'
import hintCreature from '@/assets/creatureSittingQuestion.png'

defineProps<{
  hint: string
}>()

const showHintPopup = ref(false)
</script>

<template>
  <div class="hint-wrapper">
    <!-- Rosa figur -->
    <div class="hint-creature">
      <button class="hint-creature-button" @click="showHintPopup = true">
        <img :src="hintCreature" alt="Få hint" />
      </button>
    </div>

    <!-- Innhold -->
    <div class="hint-content">
      <slot />
    </div>
  </div>

  <!-- Popup -->
  <div v-if="showHintPopup" class="hint-overlay" @click="showHintPopup = false">
    <div class="hint-popup" @click.stop>
      <h2>Tips 💡</h2>
      <p>{{ hint }}</p>

      <button class="hint-close-button" @click="showHintPopup = false">Ok</button>
    </div>
  </div>
</template>

<style scoped>
/* Wrapper som sentrerer innhold */
.hint-wrapper {
  position: relative;
  width: min(100%, 760px);
  margin: 0 auto;
}

/* Innhold (task-box) */
.hint-content {
  width: 100%;
}

/* Rosa figur */
.hint-creature {
  position: absolute;
  top: -80px;
  left: 0;
  transform: translateX(-140%);
  z-index: 20;
}

.hint-creature-button {
  width: 130px;
  height: 130px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.hint-creature-button img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
  filter: drop-shadow(0 8px 10px rgb(0 0 0 / 25%));
  transition: transform 0.2s ease;
}

.hint-creature-button:hover img {
  transform: scale(1.08);
}

/* Popup */
.hint-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgb(0 0 0 / 35%);
  display: flex;
  justify-content: center;
  align-items: center;
}

.hint-popup {
  width: min(620px, calc(100vw - 32px));
  background: var(--beige-light);
  border: 5px solid var(--brown-dark);
  border-radius: 32px;
  padding: 2rem;
  text-align: center;
  box-shadow: 0 14px 30px rgb(0 0 0 / 25%);
}

.hint-close-button {
  margin-top: 1.5rem;
  padding: 1rem 1.5rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: var(--green-confirm);
  font-weight: 800;
  cursor: pointer;
}

/* Mobil */
@media (max-width: 768px) {
  .hint-creature {
    top: -50px;
    transform: translateX(-60%);
  }

  .hint-creature-button {
    width: 90px;
    height: 90px;
  }
}
</style>
