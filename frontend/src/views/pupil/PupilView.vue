<script setup lang="ts">
import TopBar from '@/components/pupil/TopBar.vue'
import corkboardImage from '@/assets/backgrounds/korktavle.png'
import Blobby from '@/components/pupil/home/Blobby.vue'
import notesIcon from '@/assets/icons/homescreen/icon_notes.png'
import mysteryIcon from '@/assets/icons/homescreen/icon_mystery.png'
import medalsIcon from '@/assets/icons/homescreen/icon_medals.png'
import mapIcon from '@/assets/icons/homescreen/icon_map.png'
import leaderboardIcon from '@/assets/icons/homescreen/icon_leaderboard.png'
import helpIcon from '@/assets/icons/homescreen/icon_help.png'
import { useAudio } from '@/composables/useAudio'
import openFileSound from '@/assets/sounds/shared/open-file-sound.mp3'
import boardBackground from '@/assets/backgrounds/korktavle.png'
import boardBackgroundLarge from '@/assets/backgrounds/korktavle-stor.png'

const MAP_OPEN_SOUND_RELATIVE_VOLUME = 1.5

const buttons = [
  { to: '/pupil/classroom/notebook', class: 'notes', icon: notesIcon },
  { to: '/pupil/classroom/mystery', class: 'mystery', icon: mysteryIcon },
  { to: '/pupil/classroom/medals', class: 'medals', icon: medalsIcon },
  { to: '/pupil/classroom/map', class: 'map', icon: mapIcon, sound: openFileSound },
  { to: '/pupil/classroom/leaderboard', class: 'leaderboard', icon: leaderboardIcon },
  { to: '/pupil/classroom/help', class: 'help', icon: helpIcon },
]

const { playSound } = useAudio()

function handleButtonClick(sound?: string) {
  if (!sound) return
  playSound(sound, MAP_OPEN_SOUND_RELATIVE_VOLUME, 0.7)
}
</script>

<template>
  <div class="screen">
    <TopBar title="Detektivbyrået" />
    <div class="page">
      <div class="center-container">
        <div class="image-wrapper">
          <!-- <img :src="corkboardImage" alt="Background" /> -->
          <img class="pupil-pinboard-image" :src="boardBackground" alt="" aria-hidden="true" />
          <img class="pupil-pinboard-image-large" :src="boardBackgroundLarge" alt="" aria-hidden="true" />
          <Blobby />
          <router-link
            v-for="btn in buttons"
            :key="btn.to"
            :to="btn.to"
            class="overlay-button"
            :class="btn.class"
            :data-no-click-sound="btn.sound ? true : null"
            @click="handleButtonClick(btn.sound)"
          >
            <img :src="btn.icon" />
          </router-link>
        </div>
      </div>
    </div>
  </div>
  <router-view />
</template>

<style scoped>

.pupil-pinboard-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
  user-select: none;
}
.pupil-pinboard-image-large {
  display: none;
  width: 100%;
  pointer-events: none;
  user-select: none;
}
.screen {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  position: relative;

  background: url('@/assets/backgrounds/hjemskjerm-bg.png') no-repeat center center;
  background-size: cover;
}

.screen::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 0;
  pointer-events: none;
}

.screen > * {
  position: relative;
  z-index: 1;
}

.page {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: transparent;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: clamp(0.5rem, 2vw, 2rem);
}

.center-container {
  width: 100%;
  min-height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 1;
}

/* pin-board */
.image-wrapper {
  position: relative;
  width: min(96vw, 1400px);
  aspect-ratio: 1400 / 900;
}

/* icons */
.overlay-button {
  position: absolute;
  cursor: pointer;
  width: clamp(10rem, 13vw, 20rem);
  transform: translateX(-50%);
}

.overlay-button img {
  display: block;
  width: 100%;
}

.overlay-button:hover img {
  transform: scale(1.05);
  transition: transform 0.15s ease;
}

/* positions */
.medals {
  top: 20%;
  left: 28%;
}

.map {
  top: 20%;
  left: 50%;
}

.leaderboard {
  top: 20%;
  left: 72%;
}

.mystery {
  top: 55%;
  left: 28%;
}

.notes {
  top: 55%;
  left: 72%;
}

.help {
  top: 55%;
  left: 50%;
}

/* tablet screen */
@media (max-width: 768px) {
  .overlay-button {
    position: absolute;
    cursor: pointer;
    width: clamp(7rem, 12vw, 17rem);
    transform: translateX(-50%);
  }
  .pupil-pinboard-image {
    display: none;
  }
  .pupil-pinboard-image-large {
    display: block;
  }
  .map {
    top: 15%;
    left: 70%;
  }
  .notes {
    top: 65%;
    left: 70%;
  }
  .page {
    align-items: center;
    padding: 0.5rem;
  }
  .image-wrapper {
    width: 96vw;
  }
  .medals {
    top: 15%;
    left: 30%;
  }
  .leaderboard {
    top: 40%;
    left: 30%;
  }
  .mystery {
    top: 40%;
    left: 70%;
  }
  .help {
    top: 65%;
    left: 30%;
  }
}
</style>
