<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { getJwtDataWithRefresh } from './utils/jwtUtils'
import router from './router'

onMounted(async () => {
  const tokenData = await getJwtDataWithRefresh()

  if (
    !tokenData &&
    (window.location.href.includes('/pupil') ||
      window.location.href.includes('/teacher'))
  ) {
    router.push('/')
  }
})
</script>
