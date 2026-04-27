<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import confetti from 'canvas-confetti'
import completedSoundFile from '@/assets/completedSound.mp3'
import tryAgainSoundFile from '@/assets/tryAgainSound.mp3'
import { useAudio } from '@/composables/useAudio'

const router = useRouter()
const { playSound } = useAudio()

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
}>()

let completedSoundRef: HTMLAudioElement | null = null
let tryAgainSoundRef: HTMLAudioElement | null = null

const currentImage = computed(() => (props.success ? props.successImage : props.failImage))

function stopAllSounds() {
  if (completedSoundRef) {
    completedSoundRef.pause()
    completedSoundRef.currentTime = 0
  }
  if (tryAgainSoundRef) {
    tryAgainSoundRef.pause()
    tryAgainSoundRef.currentTime = 0
  }
}

function closePopup() {
  stopAllSounds()
  emit('close')
}

function goToMap() {
  stopAllSounds()
  router.push('/pupil/classroom/map')
}

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

      <div v-if="success" class="popup-actions">
        <button class="popup-btn popup-btn-map" @click="goToMap">Tilbake til kartet</button>
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

.popup-btn-map {
  background: var(--green-confirm);
}

.popup-title {
  font-size: 2rem;
}

.popup-text {
  font-size: 1rem;
}
</style>
