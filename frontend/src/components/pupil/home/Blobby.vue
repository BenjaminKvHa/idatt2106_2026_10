<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

import blobbyImage from '@/assets/icons/blobby/creaturePeaking.png'
import blobbyHoverImage from '@/assets/icons/blobby/creaturePeakingDetective.png'
import { useAudio } from '@/composables/useAudio'

import blobbySound1 from '@/assets/sounds/blobby/blobby-grunt.mp3'
import blobbySound2 from '@/assets/sounds/blobby/blobby-wow.mp3'
import blobbySound3 from '@/assets/sounds/blobby/blobby-pop.mp3'
import blobbySound4 from '@/assets/sounds/blobby/blobby-plingpling.mp3'
import blobbySound5 from '@/assets/sounds/blobby/blobby-blinking.mp3'

const props = withDefaults(
  defineProps<{
    message?: string
  }>(),
  {
    message: 'Hei mitt navn er Blobby. Trykk på meg for å høre hvilke lyder jeg kan lage!',
  },
)

const showBlobbyMessage = ref(false)
const blobbyStep = ref(0)
const isAnimatingBlobby = ref(false)
const isHoveringBlobby = ref(false)

const { playSound } = useAudio()

const blobbySounds = [blobbySound1, blobbySound2, blobbySound3, blobbySound4, blobbySound5]

function handleBlobbyClick(event: MouseEvent) {
  event.stopPropagation()

  showBlobbyMessage.value = true

  const soundFile = blobbySounds[blobbyStep.value]

  if (soundFile) {
    playSound(soundFile)
    blobbyStep.value += 1
    return
  }

  isAnimatingBlobby.value = false

  requestAnimationFrame(() => {
    isAnimatingBlobby.value = true
  })

  setTimeout(() => {
    isAnimatingBlobby.value = false
  }, 800)

  blobbyStep.value = 0
}

function closeBlobbyMessage() {
  showBlobbyMessage.value = false
}

onMounted(() => {
  document.addEventListener('click', closeBlobbyMessage)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeBlobbyMessage)
})
</script>

<template>
  <div class="blobby-wrapper" @click.stop>
    <button
      class="blobby-button"
      :class="{ animate: isAnimatingBlobby }"
      type="button"
      aria-label="Blobby"
      data-no-click-sound
      @mouseenter="isHoveringBlobby = true"
      @mouseleave="isHoveringBlobby = false"
      @click="handleBlobbyClick"
    >
      <img
        :src="isHoveringBlobby ? blobbyHoverImage : blobbyImage"
        alt="Blobby"
        class="blobby-image"
        :class="{ wiggle: isAnimatingBlobby }"
      />
    </button>

    <div v-if="showBlobbyMessage" class="blobby-message">
      {{ props.message }}
    </div>
  </div>
</template>

<style scoped>
.blobby-wrapper {
  position: absolute;
  top: 8.5%;
  right: 10%;
  width: clamp(60px, 16%, 180px);
  z-index: 10;
  transform: translateY(-50%);
}

.blobby-button {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  width: 100%;
  transform-origin: bottom center;
}

.blobby-image {
  width: 100%;
  height: auto;
  display: block;
  filter: drop-shadow(0 4px 0 rgba(0, 0, 0, 0.2));
  transition:
    transform 0.15s ease,
    filter 0.15s ease;
}

.blobby-button:hover .blobby-image {
  transform: scale(1.04);
}

.blobby-button.animate {
  animation: blobby-bounce 0.8s ease;
}

.blobby-image.wiggle {
  animation: blobby-wiggle 0.8s ease;
}

.blobby-message {
  position: absolute;
  top: 35%;
  right: 100%;
  width: 100%;
  max-width: none;
  background: #fff8e8;
  border: 2px solid #5c4033;
  border-radius: 16px;
  padding: 10px 14px;
  font-size: clamp(12px, 1.1vw, 18px);
  text-align: center;
  box-shadow: 0 4px 0 rgba(0, 0, 0, 0.12);
}

@keyframes blobby-bounce {
  0% {
    transform: translateY(0);
  }

  30% {
    transform: translateY(-20px);
  }

  60% {
    transform: translateY(0);
  }

  80% {
    transform: translateY(-10px);
  }

  100% {
    transform: translateY(0);
  }
}

@keyframes blobby-wiggle {
  0% {
    transform: rotate(0deg);
  }

  15% {
    transform: rotate(-6deg);
  }

  30% {
    transform: rotate(6deg);
  }

  45% {
    transform: rotate(-5deg);
  }

  60% {
    transform: rotate(5deg);
  }

  75% {
    transform: rotate(-2deg);
  }

  100% {
    transform: rotate(0deg);
  }
}

@media (max-width: 768px) {
  .blobby-message {
    right: 75%;
    max-width: 220px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .blobby-message {
    top: 15px;
    right: 75%;
    max-width: 180px;
    font-size: 12px;
    padding: 8px 10px;
  }
}
</style>
