<script setup lang="ts">
import { ref } from 'vue'

import blobbyImage from '@/assets/icons/blobby/creaturePeaking.png'
import { useAudio } from '@/composables/useAudio'

import blobbySound1 from '@/assets/sounds/blobby/blobby-grunt.mp3'
import blobbySound2 from '@/assets/sounds/blobby/blobby-wow.mp3'
import blobbySound3 from '@/assets/sounds/blobby/blobby-pop.mp3'
import blobbySound4 from '@/assets/sounds/blobby/blobby-plingpling.mp3'
import blobbySound5 from '@/assets/sounds/blobby/blobby-blinking.mp3'

const showBlobbyMessage = ref(false)
const blobbyStep = ref(0)
const isAnimatingBlobby = ref(false)

const { playSound } = useAudio()

const blobbyMessage = 'Hei mitt navn er Blobby. Trykk på meg for å høre hvilke lyder jeg kan lage!'

const blobbySounds = [blobbySound1, blobbySound2, blobbySound3, blobbySound4, blobbySound5]

function handleBlobbyClick() {
  if (blobbyStep.value === 0) {
    showBlobbyMessage.value = true
    blobbyStep.value = 1
    return
  }

  if (blobbyStep.value >= 1 && blobbyStep.value <= blobbySounds.length) {
    showBlobbyMessage.value = false

    const soundIndex = blobbyStep.value - 1
    const soundFile = blobbySounds[soundIndex]
    if (soundFile) playSound(soundFile)

    blobbyStep.value += 1
    return
  }

  if (blobbyStep.value === blobbySounds.length + 1) {
    isAnimatingBlobby.value = false

    requestAnimationFrame(() => {
      isAnimatingBlobby.value = true
    })

    setTimeout(() => {
      isAnimatingBlobby.value = false
    }, 800)

    showBlobbyMessage.value = false
    blobbyStep.value = 0
  }
}
</script>

<template>
  <div class="blobby-wrapper">
    <button
      class="blobby-button"
      :class="{ animate: isAnimatingBlobby }"
      type="button"
      aria-label="Blobby"
      data-no-click-sound
      @click="handleBlobbyClick"
    >
      <img
        :src="blobbyImage"
        alt="Blobby"
        class="blobby-image"
        :class="{ wiggle: isAnimatingBlobby }"
      />
    </button>

    <div v-if="showBlobbyMessage" class="blobby-message">
      {{ blobbyMessage }}
    </div>
  </div>
</template>

<style scoped>
.blobby-wrapper {
  position: absolute;
  top: 10.5%;
  right: 6%;
  width: 30%;
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
  right: 75%;
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
  .blobby-wrapper {
    top: -40px;
    right: 20px;
  }

  .blobby-image {
    width: 105px;
  }

  .blobby-message {
    right: 105px;
    max-width: 220px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .blobby-wrapper {
    top: -30px;
    right: 10px;
  }

  .blobby-image {
    width: 90px;
  }

  .blobby-message {
    top: 15px;
    right: 90px;
    max-width: 180px;
    font-size: 12px;
    padding: 8px 10px;
  }
}
</style>
