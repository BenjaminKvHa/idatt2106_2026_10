<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div v-if="route.path === '/pupil'" class="page">
    <div class="center-container">
      <router-link to="/pupil/profile" class="overlay-button profile">
        Gå til profil
      </router-link>
      <router-link to="/pupil/settings" class="overlay-button settings">
        <img src="/src/assets/homescreen/icon_settings.png"/>
      </router-link>
      <div class="image-wrapper">
        <img src="/src/assets/korktavle.png" alt="Background" />
        <router-link to="/pupil/notebook" class="overlay-button notes">
          <img src="/src/assets/homescreen/icon_notes.png"/>
        </router-link>
        <router-link to="/pupil/mystery" class="overlay-button mystery">
          <img src="/src/assets/homescreen/icon_mystery.png"/>
        </router-link>
        <router-link to="/pupil/medals" class="overlay-button medals">
          <img src="/src/assets/homescreen/icon_medals.png"/>
        </router-link>
        <router-link to="/pupil/map" class="overlay-button map">
          <img src="/src/assets/homescreen/icon_map.png"/>
        </router-link>
        <router-link to="/pupil/leaderboard" class="overlay-button leaderboard">
          <img src="/src/assets/homescreen/icon_leaderboard.png"/>
        </router-link>
        <!--router-link to="/pupil/help" class="overlay-button help">
          <img src="/src/assets/homescreen/icon_help.png"/>
        </router-link>*/-->
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

.image-wrapper{
  position: relative;
  display: inline-block;
}

.image-wrapper > img {
  width: 1100px;
  display: block;
}

.overlay-button {
  position: absolute;
  cursor: pointer;
}
.overlay-button img {
  display: block;
  width: 300px;
}

.overlay-button:hover img {
  transform: scale(1.05);
  transition: transform 0.15s ease;
}
.medals {
  top: 22%;
  left: 8%;
}

.map {
  top: 20%;
  left: 38%;
}

.leaderboard {
  top: 22%;
  right: 8%;
}

.mystery {
  top: 55%;
  left: 8%;
}

.notes {
  top: 55%;
  right: 8%;
}

.profile{
  top: 10%;
  left: 5%;
}

.settings {
  top: 1%;
  right: 2%;
}

.settings img {
  width: 150px;
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
}
</style>
