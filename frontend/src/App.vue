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
