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
import { RouterView } from 'vue-router'
import { getJwtDataWithRefresh } from './utils/jwtUtils'
import router from './router'

const backgroundMusic = ref<HTMLAudioElement | null>(null)

const startMusic = async () => {
  if (!backgroundMusic.value) return

  const audio = backgroundMusic.value
  audio.volume = 0

  const targetVolume = 0.3
  const fadeDuration = 2000 // 2 sek
  const stepTime = 50
  const steps = fadeDuration / stepTime
  const volumeStep = targetVolume / steps

  try {
    await audio.play()

    let currentStep = 0

    const fade = setInterval(() => {
      currentStep++

      audio.volume = Math.min(audio.volume + volumeStep, targetVolume)

      if (currentStep >= steps) {
        clearInterval(fade)
      }
    }, stepTime)

    document.removeEventListener('click', startMusic)
  } catch (error) {
    console.error('Kunne ikke starte musikken:', error)
  }
}

onMounted(() => {
  const tokenData = getJwtDataWithRefresh()

  document.addEventListener('click', startMusic)

  if (
    !tokenData &&
    (window.location.href.includes('/pupil') ||
      window.location.href.includes('/teacher'))
  ) {
    router.push('/')
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', startMusic)
})
</script>
