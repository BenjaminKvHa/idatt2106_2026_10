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

const MAP_OPEN_SOUND_RELATIVE_VOLUME = 1.5

const buttons = [
  { to: '/pupil/classroom/medals', class: 'medals', icon: medalsIcon },
  { to: '/pupil/classroom/map', class: 'map', icon: mapIcon, sound: openFileSound },
  { to: '/pupil/classroom/leaderboard', class: 'leaderboard', icon: leaderboardIcon },
  { to: '/pupil/classroom/mystery', class: 'mystery', icon: mysteryIcon },
  { to: '/pupil/classroom/help', class: 'help', icon: helpIcon },
  { to: '/pupil/classroom/notebook', class: 'notes', icon: notesIcon },
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
          <img :src="corkboardImage" alt="Background" />
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
.screen {
  --screen-overlay-opacity: 0.4;

  --board-max-width: 1400px;
  --board-aspect-ratio: 1400 / 900;

  --icon-width: clamp(195px, 22vw, 360px);

  --row-1-top: 21%;
  --row-2-top: 49%;
  --col-left: 28%;
  --col-center: 50%;
  --col-right: 72%;
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
  background: rgba(0, 0, 0, var(--screen-overlay-opacity));
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
  width: min(96vw, var(--board-max-width));
  aspect-ratio: var(--board-aspect-ratio);
}

.image-wrapper > img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

/* icon buttons */
.overlay-button {
  position: absolute;
  cursor: pointer;
  width: var(--icon-width);
  transform: translateX(-50%);
  outline: none;
}

.overlay-button img {
  display: block;
  width: 100%;
  transition:
    filter ease-in-out var(--icon-transition-duration),
    transform ease-in-out var(--icon-transition-duration);
}

.overlay-button:hover img,
.overlay-button:focus-visible img {
  transform: scale(var(--icon-hover-scale)) rotate(var(--icon-hover-rotate));
  filter:
    drop-shadow(0 -2px 0 var(--beige-light-100))
    drop-shadow(2px 0 0 var(--beige-light-100))
    drop-shadow(0 2px 0 var(--beige-light-100))
    drop-shadow(-2px 0 0 var(--beige-light-100));
}

/* icon positions */

.medals     { top: var(--row-1-top); left: var(--col-left);   }
.map        { top: var(--row-1-top); left: var(--col-center); }
.leaderboard{ top: var(--row-1-top); left: var(--col-right);  }
.mystery    { top: var(--row-2-top); left: var(--col-left);   }
.help       { top: var(--row-2-top); left: var(--col-center); }
.notes      { top: var(--row-2-top); left: var(--col-right);  }

/* tablet screen */
@media (max-width: 768px) {
  .screen {
    --icon-width: clamp(104px, 20vw, 178px);
  }

  .map {
    width: clamp(90px, 24vw, 160px);
  }
}

@media (max-width: 540px) {
  .screen {
    --icon-width: clamp(48px, 11vw, 100px);
    --board-max-width: 900px;
  }

  .map {
    width: clamp(65px, 15vw, 135px);
  }
}

@media (max-width: 480px) {
  .screen {
    --icon-width: clamp(58px, 19vw, 96px);
    --row-1-top: 24%;
    --row-2-top: 52%;
  }

  .page {
    align-items: center;
    padding: 0.5rem;
  }

  .image-wrapper {
    width: 96vw;
  }
}
</style>
