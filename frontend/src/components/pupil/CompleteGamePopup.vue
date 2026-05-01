<script setup lang="ts">
import { watch } from 'vue'
import confetti from 'canvas-confetti'
import victorySoundFile from '@/assets/sounds/levels/victory-sound.mp3'
import defeatSoundFile from '@/assets/sounds/levels/evil-laugh.mp3'
import { useAudio } from '@/composables/useAudio'
import BaseModal from '@/components/ui/BaseModal.vue'

const props = defineProps<{
  show: boolean
  success: boolean
  image: string
  imageAlt?: string
  kicker?: string
  title: string
  message: string
  subMessage?: string
  highlightTitle?: string
  highlightText?: string
}>()

const emit = defineEmits<{
  close: []
  retry: []
  map: []
}>()

const { playSound, stopSound } = useAudio()
let victorySoundRef: HTMLAudioElement | null = null
let defeatSoundRef: HTMLAudioElement | null = null

function stopSounds() {
  stopSound(victorySoundRef)
  stopSound(defeatSoundRef)
  victorySoundRef = null
  defeatSoundRef = null
}

function closePopup() {
  stopSounds()
  emit('close')
}

function retryGame() {
  stopSounds()
  emit('retry')
}

function goToMap() {
  stopSounds()
  emit('map')
}

function fireVictoryConfetti() {
  const duration = 2000
  const end = Date.now() + duration
  const defaults = { zIndex: 3000 }

  const frame = () => {
    confetti({
      ...defaults,
      particleCount: 8,
      angle: 60,
      spread: 60,
      origin: { x: 0, y: 0.6 },
    })

    confetti({
      ...defaults,
      particleCount: 8,
      angle: 120,
      spread: 60,
      origin: { x: 1, y: 0.6 },
    })

    if (Date.now() < end) requestAnimationFrame(frame)
  }

  frame()
}

watch(
  () => props.show,
  (isOpen) => {
    if (!isOpen) {
      stopSounds()
      return
    }

    stopSounds()

    if (props.success) {
      victorySoundRef = playSound(victorySoundFile, 0.3)
      fireVictoryConfetti()
    } else {
      defeatSoundRef = playSound(defeatSoundFile, 0.3)
    }
  },
)
</script>

<template>
  <BaseModal
    v-if="show"
    panel-class=""
    :close-on-backdrop="true"
    :close-on-escape="true"
    :showCloseButton="false"
    @close="closePopup"
  >
    <div :class="['game-popup-wrapper', { 'game-popup-defeat': !success }]">
      <div class="game-top">
      <img :src="image" :alt="imageAlt ?? (success ? 'Seier' : 'Tap')" class="game-image" />
    </div>

    <div class="game-content">
      <p v-if="kicker" class="game-kicker">{{ kicker }}</p>

      <h2 class="game-title">{{ title }}</h2>

      <p class="game-message">{{ message }}</p>
      <p v-if="subMessage" class="game-submessage">{{ subMessage }}</p>

      <div v-if="success && (highlightTitle || highlightText)" class="game-highlight">
        <p v-if="highlightTitle" class="game-highlight-title">{{ highlightTitle }}</p>
        <p v-if="highlightText">{{ highlightText }}</p>
      </div>

      <div class="game-actions">
        <button v-if="!success" class="game-button retry-button" @click="retryGame">
          Prøv på nytt
        </button>

        <button class="game-button map-button" @click="goToMap">Tilbake til kartet</button>
      </div>
      </div>
    </div>
  </BaseModal>
</template>

<style scoped>
.game-popup-wrapper {
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
  padding: 0;
}

.game-popup-wrapper.game-popup-defeat {
  background: linear-gradient(
    180deg,
    rgb(255 186 135 / 96%) 0%,
    rgb(228 107 91 / 96%) 45%,
    rgb(181 67 67 / 98%) 100%
  );
}

.game-top {
  padding: 3rem 2rem 1.5rem;
}

.game-image {
  width: min(100%, 600px);
  max-height: 400px;
  object-fit: contain;
  filter: drop-shadow(0 12px 20px rgb(0 0 0 / 20%));
}

.game-content {
  padding: 0 2.5rem 2.5rem;
}

.game-kicker {
  margin: 0 0 0.5rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.85;
}

.game-title {
  margin: 0;
  font-size: clamp(2rem, 5.5vw, 4.2rem);
  line-height: 1;
}

.game-message {
  margin: 1rem auto 0;
  max-width: 700px;
}

.game-submessage {
  margin: 0.75rem auto 0;
  max-width: 700px;
}

.game-highlight {
  margin: 2rem auto 0;
  max-width: 700px;
  padding: 1.2rem 1.4rem;
  background: rgb(255 245 214 / 72%);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 8px 20px rgb(0 0 0 / 12%);
}

.game-highlight-title {
  margin: 0 0 0.4rem;
  font-weight: 800;
}

.game-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.game-button {
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
  box-shadow: 0 8px 18px rgb(0 0 0 / 18%);
  transition: all 0.2s ease;
}

.retry-button {
  background: #ffe08a;
}

.map-button {
  background: var(--green-confirm);
}

.game-button:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .complete-game-popup {
    border-width: 4px;
    border-radius: 28px;
  }

  .game-top {
    padding: 1.5rem 1rem 0.75rem;
  }

  .game-content {
    padding: 0 1rem 1.5rem;
  }

  .game-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .game-button {
    width: 100%;
    min-width: 0;
  }
}
</style>
