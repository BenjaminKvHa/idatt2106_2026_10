<script setup lang="ts">
import { ref } from 'vue'
import TopBar from '@/components/TopBar.vue'

import blobbyImage from '@/assets/creaturePeaking.png'

import blobbySound1 from '@/assets/sounds/blobby-laugh.mp3'
import blobbySound2 from '@/assets/sounds/blobby-sad.mp3'
import blobbySound3 from '@/assets/sounds/blobby-grunt.mp3'
import blobbySound4 from '@/assets/sounds/blobby-wow.mp3'
import blobbySound5 from '@/assets/sounds/blobby-pop.mp3'

import corkboardImage from '@/assets/korktavle.png'

const showBlobbyMessage = ref(false)
const blobbyStep = ref(0)
const isAnimatingBlobby = ref(false)

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
    const audio = new Audio(blobbySounds[soundIndex])

    audio.play().catch((error) => {
      console.error('Kunne ikke spille av lyd:', error)
    })

    blobbyStep.value++
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
  <TopBar />

  <div class="page">
    <div class="center-container">
      <div class="image-wrapper">
        <img :src="corkboardImage" alt="Background" />

        <div class="blobby-wrapper">
          <button
            class="blobby-button"
            :class="{ animate: isAnimatingBlobby }"
            @click="handleBlobbyClick"
            aria-label="Blobby"
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

        <router-link to="/pupil/classroom/notebook" class="overlay-button notes">
          <img src="/src/assets/homescreen/icon_notes.png" />
        </router-link>

        <router-link to="/pupil/classroom/mystery" class="overlay-button mystery">
          <img src="/src/assets/homescreen/icon_mystery.png" />
        </router-link>

        <router-link to="/pupil/classroom/medals" class="overlay-button medals">
          <img src="/src/assets/homescreen/icon_medals.png" />
        </router-link>

        <router-link to="/pupil/classroom/map" class="overlay-button map">
          <img src="/src/assets/homescreen/icon_map.png" />
        </router-link>

        <router-link to="/pupil/classroom/leaderboard" class="overlay-button leaderboard">
          <img src="/src/assets/homescreen/icon_leaderboard.png" />
        </router-link>

        <router-link to="/pupil/classroom/help" class="overlay-button help">
          <img src="/src/assets/homescreen/icon_help.png" />
        </router-link>
      </div>
    </div>
  </div>

  <router-view />
</template>

<style scoped>
.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 1100px;
  padding: 0 0.5rem;
}

.top-bar .settings img {
  width: 80px;
}

.image-wrapper {
  position: relative;
  width: min(96vw, 1400px);
}

.image-wrapper > img {
  width: 100%;
  height: auto;
  display: block;
}

.overlay-button {
  position: absolute;
  cursor: pointer;
  width: 23%;
  max-width: none;
}

.overlay-button img {
  display: block;
  width: 100%;
}

.overlay-button:hover img {
  transform: scale(1.05);
  transition: transform 0.15s ease;
}

/* Blobby */
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

/* Responsive positions */
.medals {
  top: 19.5%;
  left: 16%;
}
.map {
  top: 17.5%;
  left: 38.5%;
}
.leaderboard {
  top: 19.5%;
  right: 16%;
}

.mystery {
  top: 52%;
  left: 16%;
}
.notes {
  top: 52%;
  right: 16%;
}
.help {
  top: 52%;
  left: 38.5%;
}

/* Medium screen */
@media (max-width: 768px) {
  .overlay-button {
    width: 30%;
  }

  .top-bar .settings img {
    width: 50px;
  }

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

/* Small screen */
@media (max-width: 480px) {
  .overlay-button {
    width: 35%;
  }

  .top-bar {
    padding: 0 0.25rem;
  }

  .top-bar .settings img {
    width: 40px;
  }

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
