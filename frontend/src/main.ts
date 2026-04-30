import './assets/styles/main.css'

import { createApp, watch, nextTick } from 'vue'
import App from './App.vue'
import router from './router'
import { VueKeyboardTrapDirectivePlugin } from '@pdanpdan/vue-keyboard-trap'
import clickSoundFile from '@/assets/sounds/shared/click-sound.mp3'
import backgroundMusicFile from '@/assets/sounds/shared/backgroundMusic.mp3'
import { useAudio } from '@/composables/useAudio'

const { registerAudioElement, playSound, isMuted } = useAudio()

const app = createApp(App)

const backgroundMusic = new Audio(backgroundMusicFile)
backgroundMusic.loop = true
backgroundMusic.preload = 'auto'
registerAudioElement(backgroundMusic)

let backgroundMusicStarted = false

function startBackgroundMusic() {
  if (backgroundMusicStarted) return
  backgroundMusicStarted = true
  if (isMuted.value) return
  backgroundMusic.play().catch((error) => {
    backgroundMusicStarted = false
    console.error('Kunne ikke starte bakgrunnsmusikk:', error)
  })
}

watch(isMuted, (muted) => {
  if (muted) {
    backgroundMusic.pause()
  } else if (backgroundMusicStarted) {
    backgroundMusic.play().catch(() => {})
  }
})

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

router.afterEach(() => {
  nextTick(() => {
    document.querySelectorAll('a:not([tabindex])').forEach((el) => {
      el.setAttribute('tabindex', '0')
    })
  })
})

app.use(router)
app.use(VueKeyboardTrapDirectivePlugin, {})
app.mount('#app')