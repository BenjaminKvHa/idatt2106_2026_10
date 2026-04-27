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
import { useAudio } from './composables/useAudio'

const backgroundMusic = ref<HTMLAudioElement | null>(null)
const { registerAudioElement, volume, isMuted } = useAudio()

const startMusic = async () => {
  if (!backgroundMusic.value) return

  const audio = backgroundMusic.value
  registerAudioElement(audio)
  audio.volume = 0

  const fadeDuration = 2000 // 2 sek
  const stepTime = 50
  const steps = fadeDuration / stepTime

  try {
    await audio.play()

    if (!isMuted.value && volume.value > 0) {
      let currentStep = 0

      const fade = setInterval(() => {
        currentStep++
        const target = isMuted.value ? 0 : volume.value / 100
        audio.volume = Math.min(audio.volume + target / steps, target)
        if (currentStep >= steps) clearInterval(fade)
      }, stepTime)
    }

    document.removeEventListener('click', startMusic)
  } catch (error) {
    console.error('Kunne ikke starte musikken:', error)
  }
}

onMounted(async () => {
  const tokenData = await getJwtDataWithRefresh()

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
