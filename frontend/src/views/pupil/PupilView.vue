<script setup lang="ts">
import TopBar from '@/components/pupil/TopBar.vue'
import PinboardLayout from '@/components/pupil/PinboardLayout.vue'
import Blobby from '@/components/pupil/home/Blobby.vue'

import notesIcon from '@/assets/icons/homescreen/icon_notes.png'
import mysteryIcon from '@/assets/icons/homescreen/icon_mystery.png'
import medalsIcon from '@/assets/icons/homescreen/icon_medals.png'
import mapIcon from '@/assets/icons/homescreen/icon_map.png'
import leaderboardIcon from '@/assets/icons/homescreen/icon_leaderboard.png'
import helpIcon from '@/assets/icons/homescreen/icon_help.png'

import { useAudio } from '@/composables/useAudio'
import openFileSound from '@/assets/sounds/shared/open-file-sound.mp3'

const MAP_OPEN_SOUND_RELATIVE_VOLUME = 2

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
  <div class="pupil-home-bg pupil-scene-screen">
    <TopBar title="Detektivbyrået" />

    <PinboardLayout>
      <Blobby
        message="Velkommen til hjemskjermen. Her du kan komme deg til de ulike andre sidene. Trykk på knappene inni tavlen for å flytte deg til en ny side."
      />

      <router-link
        v-for="btn in buttons"
        :key="btn.to"
        :to="btn.to"
        class="overlay-button"
        :class="btn.class"
        :data-no-click-sound="btn.sound ? true : null"
        @click="handleButtonClick(btn.sound)"
      >
        <img :src="btn.icon" alt="" />
      </router-link>
    </PinboardLayout>
  </div>
</template>

<style scoped>
.pupil-home-bg {
  min-height: 100vh;
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
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
  width: 16%;
  transform: translateX(-50%);
  z-index: 2;
}

.overlay-button img {
  display: block;
  width: 100%;
}

.overlay-button:hover img {
  transform: scale(1.05);
  transition: transform 0.15s ease;
}

.medals {
  top: 22%;
  left: 26%;
}

.map {
  top: 22%;
  left: 50%;
}

.leaderboard {
  top: 22%;
  left: 74%;
}

.mystery {
  top: 54%;
  left: 26%;
}

.help {
  top: 54%;
  left: 50%;
}

.notes {
  top: 54%;
  left: 74%;
}

@media (max-width: 768px) {
  .overlay-button {
    position: absolute;
    cursor: pointer;
    width: 22%;
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
@media (max-width: 600px) {
  .overlay-button {
    position: absolute;
    cursor: pointer;
    width: clamp(7rem, 12vw, 17rem);
    transform: translateX(-50%);
  }
}
</style>
