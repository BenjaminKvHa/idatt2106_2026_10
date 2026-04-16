<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div v-if="route.path === '/pupil'" class="page">
    <div class="center-container">
      <div class="image-wrapper">
        <img src="/src/assets/korktavle.png" alt="Background" />
        <router-link to="/pupil/notebook" class="overlay-button">
          <img src="/src/assets/homescreen/icon_notes.png"/>
        </router-link>
        <router-link to="/pupil/mystery" class="overlay-button">
          <img src="/src/assets/homescreen/icon_mystery.png"/>
        </router-link>
        <router-link to="/pupil/medals" class="overlay-button">
          <img src="/src/assets/homescreen/icon_medals.png"/>
        </router-link>
        <router-link to="/pupil/map" class="overlay-button">
          <img src="/src/assets/homescreen/icon_map.png"/>
        </router-link>
        <router-link to="/pupil/profile" class="profile-button">
          Gå til profil
        </router-link>
      </div>
    </div>
  </div>
  <router-view />
</template>

<style scoped>
.page{
  display: grid;
  place-items: center;
  height: 100%;
  background-color: var(--beige-light);
}

img {
  width: 900px;
}

.image-wrapper{
  position: relative;
  display: inline-block;
}

.image-wrapper > img {
  width: 900px;
  display: block;
}

.button {
  background-color: var(--green-confirm);
  color: var(--color-text);
  border: 7px solid var(--color-text);
  padding: 2rem 2rem;
  border-radius: 38px;
  font-weight: 600;
  font-size: 50px;
  box-shadow: -10px 8px 8px rgba(0,0,0,0.4);
}

.overlay-button {
  position: absolute;
  top: 50%;       /* adjust to place it where you want */
  left: 50%;      /* adjust to place it where you want */
  transform: translate(-50%, -50%);
  cursor: pointer;
}
.overlay-button img {
  width: 150px;   /* adjust to your button png size */
}

.overlay-button:hover img {
  transform: scale(1.05);
  transition: transform 0.15s ease;
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
}
</style>
<style>
html, body, #app {
  height: 100% !important;
  margin: 0;
  padding: 0;
}
</style>
