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
    confetti({ ...defaults, particleCount: 8, angle: 60, spread: 60, origin: { x: 0, y: 0.6 } })
    confetti({ ...defaults, particleCount: 8, angle: 120, spread: 60, origin: { x: 1, y: 0.6 } })
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
    :show-close-button="false"
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
  background: linear-gradient(180deg, #fff0c9 0%, #f5d59a 50%, #e8b86f 100%);
  border: 7px solid var(--brown-dark);
  border-radius: 44px;
  box-shadow:
    0 14px 0 #5b371f,
    0 28px 60px rgb(0 0 0 / 32%),
    inset 0 4px 0 rgb(255 255 255 / 45%);
  text-align: center;
}

.game-popup-wrapper.game-popup-defeat {
  background: linear-gradient(180deg, #ffe1c9 0%, #f0a078 55%, #d96a55 100%);
}

.game-top {
  padding: 3rem 2rem 1.5rem;
}

.game-image {
  width: min(100%, 600px);
  max-height: 400px;
  object-fit: contain;
  filter: drop-shadow(0 12px 20px rgb(0 0 0 / 22%));
}

.game-content {
  padding: 0 2.5rem 2.7rem;
}

.game-kicker {
  margin: 0 0 0.5rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--brown-dark);
  opacity: 0.9;
}

.game-title {
  margin: 0;
  font-size: clamp(2rem, 5.5vw, 4.2rem);
  line-height: 1;
  color: var(--brown-dark);
  text-shadow: 0 3px 0 rgb(255 255 255 / 45%);
}

.game-message,
.game-submessage {
  max-width: 700px;
  color: #2b1a10;
  font-weight: 600;
}

.game-message {
  margin: 1rem auto 0;
}

.game-submessage {
  margin: 0.75rem auto 0;
}

.game-highlight {
  margin: 2rem auto 0;
  max-width: 700px;
  padding: 1.2rem 1.4rem;
  background: rgb(255 246 220 / 82%);
  border: 5px solid var(--brown-dark);
  border-radius: 30px;
  box-shadow:
    0 8px 0 #5b371f,
    inset 0 3px 0 rgb(255 255 255 / 45%);
}

.game-highlight-title {
  margin: 0 0 0.4rem;
  font-weight: 900;
  color: var(--brown-dark);
}

.game-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: center;
  gap: 1.2rem;
  flex-wrap: wrap;
}

.game-button {
  min-width: 240px;
  min-height: 72px;
  padding: 1rem 1.4rem;
  border: 5px solid var(--brown-dark);
  border-radius: 28px;
  font-weight: 900;
  color: var(--brown-dark);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 0 #5b371f;
  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease;
}

.retry-button {
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
}

.map-button {
  background: linear-gradient(180deg, #9df13c 0%, #78d927 100%);
}

.game-button:hover {
  filter: brightness(1.04);
  transform: translateY(-2px);
}

.game-button:active {
  transform: translateY(5px);
  box-shadow: 0 3px 0 #5b371f;
}

@media (max-width: 768px) {
  .game-popup-wrapper {
    border-width: 5px;
    border-radius: 30px;
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