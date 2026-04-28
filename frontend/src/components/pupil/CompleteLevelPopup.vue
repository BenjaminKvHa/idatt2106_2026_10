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
.popup-btn-success {
  background: var(--green-confirm);
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

.complete-popup {
  width: min(800px, calc(100vw - 32px));
  background: var(--orange-medium);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 32px 24px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.complete-popup-fail {
  background: #ff4d4d;
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
}

.popup-title {
  font-size: 2rem;
  margin: 0 0 1.2rem;
}

.popup-text {
  font-size: 1rem;
  margin-bottom: 1.5rem;
}

.popup-text p {
  margin: 0 0 0.75rem;
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
  .complete-popup {
    padding: 24px 18px;
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
