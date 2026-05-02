<script setup lang="ts">
import { computed, watch } from 'vue'
import confetti from 'canvas-confetti'
import victorySoundFile from '@/assets/sounds/levels/victory-sound.mp3'
import defeatSoundFile from '@/assets/sounds/levels/evil-laugh.mp3'
import { useAudio } from '@/composables/useAudio'
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue'
import type { PupilMedal } from '@/types/chapter'
import trophyImage from '@/assets/trophy.png'

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
  completedChapterMedal?: PupilMedal | null
  completedChapterMedalNewlyAwarded?: boolean
  grandCelebration?: boolean
}>()

const emit = defineEmits<{
  close: []
  retry: []
  map: []
}>()

const { playSound, stopSound } = useAudio()
let victorySoundRef: HTMLAudioElement | null = null
let defeatSoundRef: HTMLAudioElement | null = null

const isGrandCelebration = computed(
  () => props.success && props.grandCelebration && props.completedChapterMedalNewlyAwarded,
)
const medalImageUrl = computed(() => {
  const fileObjectId = props.completedChapterMedal?.imageFileObjectId
  return fileObjectId === null || fileObjectId === undefined ? null : `/api/objects/${fileObjectId}`
})

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
  const duration = isGrandCelebration.value ? 3600 : 2000
  const end = Date.now() + duration
  const defaults = { zIndex: 3000 }
  const particleCount = isGrandCelebration.value ? 14 : 8
  const spread = isGrandCelebration.value ? 78 : 60

  const frame = () => {
    confetti({
      ...defaults,
      particleCount,
      angle: 60,
      spread,
      origin: { x: 0, y: 0.6 },
    })

    confetti({
      ...defaults,
      particleCount,
      angle: 120,
      spread,
      origin: { x: 1, y: 0.6 },
    })

    if (isGrandCelebration.value) {
      confetti({
        ...defaults,
        particleCount: 10,
        spread: 90,
        startVelocity: 35,
        origin: { x: 0.5, y: 0.25 },
      })
    }

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
  <div v-if="show" class="game-overlay" @click="closePopup">
    <div
      class="game-popup"
      :class="{ 'game-popup-defeat': !success, 'game-popup-grand': isGrandCelebration }"
      @click.stop
    >
      <div class="game-top">
        <img :src="image" :alt="imageAlt ?? (success ? 'Seier' : 'Tap')" class="game-image" />
      </div>

      <div class="game-content">
        <p v-if="kicker" class="game-kicker">{{ kicker }}</p>

        <h2 class="game-title">{{ title }}</h2>

        <p class="game-message">{{ message }}</p>
        <p v-if="subMessage" class="game-submessage">{{ subMessage }}</p>

        <div v-if="isGrandCelebration" class="trophy-stage" data-test="trophy-stage">
          <img class="trophy-image" :src="trophyImage" alt="Trofé" />
          <p>Du har vunnet trofeet i Datasenteret.</p>
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

        <div
          v-if="success && (highlightTitle || highlightText)"
          class="game-highlight"
          :class="{ 'game-highlight-grand': isGrandCelebration }"
        >
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
  </div>
</template>

<style scoped>
.game-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 32%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

.game-popup {
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

.game-popup-defeat {
  background: linear-gradient(180deg, #ffe1c9 0%, #f0a078 55%, #d96a55 100%);
}

.game-popup-grand {
  width: min(1060px, calc(100vw - 32px));
  background: linear-gradient(180deg, #fff6d7 0%, #f7d98c 48%, #e7ad55 100%);
  box-shadow:
    0 16px 0 #5b371f,
    0 32px 70px rgb(0 0 0 / 36%),
    inset 0 5px 0 rgb(255 255 255 / 52%);
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

.trophy-stage {
  width: min(100%, 720px);
  margin: 1.6rem auto 0;
  padding: 1rem 1.4rem 1.2rem;
  border: 5px solid var(--brown-dark);
  border-radius: 34px;
  background: linear-gradient(180deg, #fff9dc 0%, #ffdf80 100%);
  box-shadow:
    0 9px 0 #5b371f,
    inset 0 3px 0 rgb(255 255 255 / 55%);
  color: var(--brown-dark);
  font-weight: 900;
}

.trophy-image {
  width: clamp(6.5rem, 16vw, 10rem);
  height: clamp(6.5rem, 16vw, 10rem);
  display: block;
  margin: 0 auto 0.7rem;
  object-fit: contain;
  filter: drop-shadow(0 0.45rem 0.15rem rgb(91 55 31 / 28%));
}

.trophy-stage p {
  margin: 0;
  font-size: clamp(1.05rem, 2.1vw, 1.45rem);
}

.medal-reward {
  width: min(100%, 700px);
  margin: 1.8rem auto 0;
  padding: 1rem 1.2rem;
  border: 5px solid var(--brown-dark);
  border-radius: 30px;
  background: rgb(255 246 220 / 86%);
  box-shadow:
    0 8px 0 #5b371f,
    inset 0 3px 0 rgb(255 255 255 / 48%);
}

.medal-reward-new {
  background: linear-gradient(180deg, #fff8df 0%, #ffe2a0 100%);
}

.medal-kicker {
  margin: 0 0 0.7rem;
  color: var(--brown-dark);
  font-size: 1rem;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.medal-content {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 1.1rem;
  align-items: center;
  text-align: left;
}

.medal-frame {
  width: clamp(6.4rem, 14vw, 8.6rem);
  height: clamp(6.4rem, 14vw, 8.6rem);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgb(91 55 31 / 60%);
  border-radius: 26px;
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
  font-size: 2.2rem;
  font-weight: 900;
}

.medal-copy h3 {
  margin: 0;
  color: var(--brown-dark);
  font-size: clamp(1.35rem, 2.3vw, 1.8rem);
}

.medal-copy p {
  margin: 0.35rem 0 0;
  color: #2b1a10;
  font-weight: 700;
  line-height: 1.35;
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

.game-highlight-grand {
  background: rgb(255 249 221 / 90%);
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
  .game-popup {
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

  .medal-content {
    grid-template-columns: 1fr;
    justify-items: center;
    text-align: center;
  }
}
</style>