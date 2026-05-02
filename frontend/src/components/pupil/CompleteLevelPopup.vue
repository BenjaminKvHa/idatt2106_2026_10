<script setup lang="ts">
import { computed, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import confetti from 'canvas-confetti'
import completedSoundFile from '@/assets/sounds/levels/completedSound.mp3'
import tryAgainSoundFile from '@/assets/sounds/levels/tryAgainSound.mp3'
import { useAudio } from '@/composables/useAudio'
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue'
import type { PupilMedal } from '@/types/chapter'
import BaseModal from '@/components/ui/BaseModal.vue'


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
  completedChapterMedal?: PupilMedal | null
  completedChapterMedalNewlyAwarded?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'retry'): void
  (e: 'map'): void
}>()

let completedSoundRef: HTMLAudioElement | null = null
let tryAgainSoundRef: HTMLAudioElement | null = null

const currentImage = computed(() => (props.success ? props.successImage : props.failImage))
const medalImageUrl = computed(() => {
  const fileObjectId = props.completedChapterMedal?.imageFileObjectId
  return fileObjectId === null || fileObjectId === undefined ? null : `/api/objects/${fileObjectId}`
})

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
      const defaults = { zIndex: 2000 }

      const frame = () => {
        confetti({ ...defaults, particleCount: 8, angle: 60, spread: 60, origin: { x: 0, y: 0.6 } })
        confetti({ ...defaults, particleCount: 8, angle: 120, spread: 60, origin: { x: 1, y: 0.6 } })
        if (Date.now() < end) requestAnimationFrame(frame)
      }

      frame()
    } else {
      tryAgainSoundRef = playSound(tryAgainSoundFile, 0.5)
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
    <div :class="['complete-popup-wrapper', { 'complete-popup-fail': success === false }]">
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

      <div
        v-if="success && completedChapterMedal"
        class="medal-reward"
        :class="{ 'medal-reward-new': completedChapterMedalNewlyAwarded }"
      >
        <p class="medal-kicker">
          {{ completedChapterMedalNewlyAwarded ? 'Ny medalje låst opp' : 'Medaljen din' }}
        </p>

        <div class="medal-content">
          <div class="medal-frame">
            <AuthenticatedImage
              v-if="medalImageUrl"
              :src="medalImageUrl"
              :alt="completedChapterMedal.name"
              class="medal-image"
            />
            <span v-else class="medal-placeholder">?</span>
          </div>

          <div class="medal-copy">
            <h3>{{ completedChapterMedal.name }}</h3>
            <p>
              {{
                completedChapterMedalNewlyAwarded
                  ? 'Du fikk denne medaljen for å fullføre kapittelet for første gang.'
                  : 'Du har allerede låst opp denne medaljen, og den er fortsatt din.'
              }}
            </p>
          </div>
        </div>
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
  </BaseModal>
</template>

<style scoped>
.complete-popup-wrapper {
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
}

.complete-popup-wrapper.complete-popup-fail {
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

.medal-reward {
  width: min(100%, 600px);
  margin: 1.6rem auto 0;
  padding: 1rem 1.1rem;
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  background: rgb(255 246 220 / 86%);
  box-shadow:
    0 7px 0 #5b371f,
    inset 0 3px 0 rgb(255 255 255 / 48%);
}

.medal-reward-new {
  background: linear-gradient(180deg, #fff8df 0%, #ffe4a7 100%);
}

.medal-kicker {
  margin: 0 0 0.65rem;
  color: var(--brown-dark);
  font-size: 0.95rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.medal-content {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 1rem;
  align-items: center;
  text-align: left;
}

.medal-frame {
  width: clamp(5.6rem, 15vw, 7.6rem);
  height: clamp(5.6rem, 15vw, 7.6rem);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgb(91 55 31 / 60%);
  border-radius: 24px;
  background: rgb(255 255 255 / 45%);
  box-shadow: inset 0 2px 0 rgb(255 255 255 / 55%);
}

.medal-image {
  width: 88%;
  height: 88%;
  object-fit: contain;
  filter: drop-shadow(0 5px 5px rgb(0 0 0 / 18%));
}

.medal-placeholder {
  color: var(--brown-dark);
  font-size: 2rem;
  font-weight: 900;
}

.medal-copy h3 {
  margin: 0;
  color: var(--brown-dark);
  font-size: clamp(1.2rem, 2.2vw, 1.6rem);
}

.medal-copy p {
  margin: 0.3rem 0 0;
  color: #2b1a10;
  font-weight: 700;
  line-height: 1.35;
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
  .complete-popup-wrapper {
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

  .medal-content {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }
}
</style>