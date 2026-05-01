<script setup lang="ts">
import { computed, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import confetti from 'canvas-confetti'
import completedSoundFile from '@/assets/sounds/levels/completedSound.mp3'
import tryAgainSoundFile from '@/assets/sounds/levels/tryAgainSound.mp3'
import { useAudio } from '@/composables/useAudio'

const router = useRouter()
const { playSound, stopSound } = useAudio()

const props = defineProps<{
  show: boolean
  title?: string
  message?: string
  subMessage?: string
  success?: boolean
  successImage?: string
  failImage?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'retry'): void
  (e: 'map'): void
}>()

let completedSoundRef: HTMLAudioElement | null = null
let tryAgainSoundRef: HTMLAudioElement | null = null

const currentImage = computed(() => (props.success ? props.successImage : props.failImage))

function stopAllSounds() {
  stopSound(completedSoundRef)
  stopSound(tryAgainSoundRef)
  completedSoundRef = null
  tryAgainSoundRef = null
}

function closePopup() {
  stopAllSounds()
  emit('close')
}

function goToMap() {
  stopAllSounds()

  if (props.success) {
    router.push('/pupil/classroom/map')
    return
  }

  emit('map')
}

function retry() {
  stopAllSounds()
  emit('retry')
}

onUnmounted(stopAllSounds)

watch(
  () => props.show,
  (isOpen) => {
    if (!isOpen) {
      stopAllSounds()
      return
    }

    stopAllSounds()

    if (props.success) {
      completedSoundRef = playSound(completedSoundFile, 0.5)

      const duration = 2000
      const end = Date.now() + duration

      const defaults = {
        zIndex: 2000,
      }

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

        if (Date.now() < end) {
          requestAnimationFrame(frame)
        }
      }

      frame()
    } else {
      tryAgainSoundRef = playSound(tryAgainSoundFile, 0.5)
    }
  },
)
</script>

<template>
  <div v-if="show" class="popup-overlay" @click="closePopup">
    <div class="complete-popup" :class="{ 'complete-popup-fail': success === false }" @click.stop>
      <div v-if="currentImage" class="popup-image-wrapper">
        <img :src="currentImage" :alt="success ? 'Riktig svar' : 'Feil svar'" class="popup-image" />
      </div>

      <h2 class="popup-title">
        {{ title || (success ? 'Du klarte det!' : 'Du klarte det ikke') }}
      </h2>

      <div class="popup-text">
        <p>
          {{
            message ||
            (success
              ? 'Gratulerer! Du har fullført nivået.'
              : 'Å nei! Du løste ikke oppgaven riktig denne gangen.')
          }}
        </p>
        <p>
          {{
            subMessage ||
            (success
              ? 'Nå har du vist at du kan tenke som en ekte nett-detektiv!'
              : 'Prøv igjen for å klare nivået.')
          }}
        </p>
      </div>

      <div class="popup-actions">
        <template v-if="success">
          <button class="popup-btn popup-btn-success" @click="goToMap">Tilbake til kartet</button>
        </template>

        <template v-else>
          <button class="popup-btn popup-btn-play" @click="retry">Prøv på nytt</button>

          <button class="popup-btn popup-btn-map" @click="goToMap">Tilbake til kartet</button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 28%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(3px);
}

.complete-popup {
  width: min(820px, calc(100vw - 32px));
  background: linear-gradient(180deg, #fff0c9 0%, #f5d59a 55%, #e8b86f 100%);
  border: 7px solid var(--brown-dark);
  border-radius: 42px;
  box-shadow:
    0 12px 0 #5b371f,
    0 26px 45px rgb(0 0 0 / 28%),
    inset 0 4px 0 rgb(255 255 255 / 45%);
  padding: 34px 28px 40px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.complete-popup-fail {
  background: linear-gradient(180deg, #ffe1c9 0%, #f0a078 55%, #d96a55 100%);
}

.popup-image-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}

.popup-image {
  max-width: 220px;
  width: 100%;
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 10px 16px rgb(0 0 0 / 20%));
}

.popup-title {
  font-size: clamp(2rem, 4vw, 3.2rem);
  margin: 0 0 1.2rem;
  color: var(--brown-dark);
  text-shadow: 0 3px 0 rgb(255 255 255 / 45%);
}

.popup-text {
  font-size: 1.15rem;
  margin-bottom: 1.5rem;
  color: #2b1a10;
  font-weight: 600;
}

.popup-text p {
  margin: 0 0 0.75rem;
}

.popup-actions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
}

.popup-btn {
  min-width: 190px;
  padding: 18px 30px;
  border-radius: 28px;
  border: 5px solid var(--brown-dark);
  font-size: 1.25rem;
  font-weight: 900;
  color: var(--brown-dark);
  cursor: pointer;
  box-shadow: 0 8px 0 #5b371f;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    filter 0.15s ease;
}

.popup-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.04);
}

.popup-btn:active {
  transform: translateY(5px);
  box-shadow: 0 3px 0 #5b371f;
}

.popup-btn-success,
.popup-btn-play {
  background: linear-gradient(180deg, #9df13c 0%, #78d927 100%);
}

.popup-btn-map {
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
}

@media (max-width: 768px) {
  .complete-popup {
    padding: 24px 18px;
    border-width: 5px;
    border-radius: 30px;
  }

  .popup-image {
    max-width: 180px;
  }

  .popup-title {
    font-size: 1.7rem;
  }

  .popup-actions {
    gap: 14px;
  }

  .popup-btn {
    width: 100%;
    min-width: unset;
    font-size: 1.1rem;
  }
}
</style>
