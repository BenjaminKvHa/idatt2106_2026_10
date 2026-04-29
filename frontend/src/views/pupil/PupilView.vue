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

const buttons = [
  { to: '/pupil/classroom/notebook', class: 'notes', icon: notesIcon },
  { to: '/pupil/classroom/mystery', class: 'mystery', icon: mysteryIcon },
  { to: '/pupil/classroom/medals', class: 'medals', icon: medalsIcon },
  { to: '/pupil/classroom/map', class: 'map', icon: mapIcon },
  { to: '/pupil/classroom/leaderboard', class: 'leaderboard', icon: leaderboardIcon },
  { to: '/pupil/classroom/help', class: 'help', icon: helpIcon },
]
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
            :class="btn.class">
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
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

/* page */
.page {
  position: relative;
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: url('@/assets/backgrounds/hjemskjerm-bg.png') no-repeat center center;
  background-size: cover;
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

.image-wrapper > img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

/* icons */
.overlay-button {
  position: absolute;
  cursor: pointer;
  width: clamp(195px, 22vw, 360px);
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
  top: 21%;
  left: 28%;
}

.map {
  top: 21%;
  left: 50%;
}

.leaderboard {
  top: 21%;
  left: 72%;
}

.mystery {
  top: 49%;
  left: 28%;
}

.notes {
  top: 49%;
  left: 72%;
}

.help {
  top: 49%;
  left: 50%;
}

/* tablet screen */
@media (max-width: 768px) {
  .overlay-button {
    width: clamp(104px, 20vw, 178px);
  }

  .map {
    width: clamp(90px, 24vw, 160px);
  }
}

/* phone screen */
@media (max-width: 540px) {
  .image-wrapper {
    width: min(96vw, 900px);
  }

  .overlay-button {
    width: clamp(48px, 11vw, 100px);
  }

  .map {
    width: clamp(65px, 15vw, 135px);
  }
}

@media (max-width: 480px) {
  .page {
    align-items: center;
    padding: 0.5rem;
  }

  .image-wrapper {
    width: 96vw;
  }

  .overlay-button {
    width: clamp(58px, 19vw, 96px);
  }

  .medals {
    top: 24%;
    left: 28%;
  }

  .map {
    top: 24%;
    left: 50%;
  }

  .leaderboard {
    top: 24%;
    left: 72%;
  }

  .mystery {
    top: 52%;
    left: 28%;
  }

  .notes {
    top: 52%;
    left: 72%;
  }

  .help {
    top: 52%;
    left: 50%;
  }
}
</style>
