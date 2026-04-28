import './assets/styles/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import clickSoundFile from '@/assets/sounds/shared/click-sound.mp3'
import backgroundMusicFile from '@/assets/sounds/shared/backgroundMusic.mp3'
import { useAudio } from '@/composables/useAudio'

const { registerAudioElement, playSound } = useAudio()

const app = createApp(App)

const backgroundMusic = new Audio(backgroundMusicFile)
backgroundMusic.loop = true
backgroundMusic.preload = 'auto'
registerAudioElement(backgroundMusic)

let backgroundMusicStarted = false

function startBackgroundMusic() {
  if (backgroundMusicStarted) return

  backgroundMusicStarted = true

  backgroundMusic.play().catch((error) => {
    backgroundMusicStarted = false
    console.error('Kunne ikke starte bakgrunnsmusikk:', error)
  })
}

function playClickSound() {
  playSound(clickSoundFile, 1, 0.05)
}

document.addEventListener(
  'pointerdown',
  (e) => {
    startBackgroundMusic()

    const target = e.target as HTMLElement | null
    if (!target) return

    const shouldSkipClickSound = target.closest('[data-no-click-sound]')
    if (shouldSkipClickSound) return

    const clickableElement = target.closest(
      'button:not(:disabled), a[href], [role="button"], .clickable, [data-click-sound], input[type="button"], input[type="submit"]'
    )

    if (!clickableElement) return

    playClickSound()
  },
  true
)

app.use(router)
app.mount('#app')
