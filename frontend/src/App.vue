<script setup lang="ts">
import { RouterView } from 'vue-router'
import { getJwtDataWithRefresh } from './utils/jwtUtils';
import router from './router';
import { onMounted } from 'vue'

onMounted(() => {
  const tokenData = getJwtDataWithRefresh();
  if (!tokenData && (window.location.href.includes("/pupil") || window.location.href.includes("/teacher"))) {
    router.push("/")
  }
});

</script>
<template>
  <div id="app">
    <audio ref="backgroundMusic" loop>
      <source src="@/assets/backgroundMusic.mp3" type="audio/mpeg" />
      Nettleseren din støtter ikke lyd.
    </audio>

    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'

const backgroundMusic = ref<HTMLAudioElement | null>(null)

const startMusic = async () => {
  if (!backgroundMusic.value) return

  backgroundMusic.value.volume = 0.3

  try {
    await backgroundMusic.value.play()
    document.removeEventListener('click', startMusic)
  } catch (error) {
    console.error('Kunne ikke starte musikken:', error)
  }
}

onMounted(() => {
  document.addEventListener('click', startMusic)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', startMusic)
})
</script>
