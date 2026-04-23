<script setup lang="ts">
import { watch } from "vue"
import confetti from "canvas-confetti"

const props = defineProps<{
  show: boolean
  title?: string
  message?: string
  subMessage?: string
  success?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

function closePopup() {
  emit('close')
}

watch(() => props.show, (isOpen) => {
  if (isOpen) {
    const duration = 2000
    const end = Date.now() + duration

    const defaults = {
      zIndex: 2000
    }

    const frame = () => {
      confetti({
        ...defaults,
        particleCount: 8,
        angle: 60,
        spread: 60,
        origin: { x: 0, y: 0.6 }
      })

      confetti({
        ...defaults,
        particleCount: 8,
        angle: 120,
        spread: 60,
        origin: { x: 1, y: 0.6 }
      })

      if (Date.now() < end) {
        requestAnimationFrame(frame)
      }
    }

    frame()
  }
})
</script>

<template>
  <div v-if="show" class="popup-overlay" @click="closePopup">
    <div class="complete-popup" @click.stop>
      <h2 class="popup-title">{{ title || 'Du klarte det!' }}</h2>

      <div class="popup-text">
        <p>{{ message || 'Gratulerer! Du har fullført nivået.' }}</p>
        <p>{{ subMessage || 'Nå har du vist at du kan tenke som en ekte nett-detektiv!' }}</p>
      </div>

      <div class="popup-actions">
        <button class="popup-btn popup-btn-play" @click="closePopup">
          Ok
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 18%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.complete-popup {
  width: min(800px, calc(100vw - 32px));
  background: var(--orange-medium-80);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 32px 24px;
  text-align: center;
  backdrop-filter: blur(2px);
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
  font-size: 1.3rem;
  font-weight: 800;
  color: var(--color-text);
  cursor: pointer;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
}

.popup-btn-play {
  background: var(--green-confirm);
}

.popup-title{
  font-size: 2rem;
}

.popup-text{
  font-size: 1rem;
}
</style>
