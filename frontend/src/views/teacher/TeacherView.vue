<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div v-if="route.path === '/teacher'" class="page">
    <div class="center-container">
      <router-link to="/teacher/profile" class="overlay-button profile">
        Gå til profil
      </router-link>

    </div>
  </div>
  <router-view />
</template>

<style scoped>

</style>
