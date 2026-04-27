import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import clickSoundFile from '@/assets/click-sound.mp3'
import { useAudio } from '@/composables/useAudio'

const app = createApp(App)

const { playSound } = useAudio()

document.addEventListener('click', (e) => {
  const target = e.target as HTMLElement | null
  if (!target) return

  const clickableElement = target.closest(
    'button:not(:disabled), a, [role="button"], .clickable'
  )

  if (!clickableElement) return

  playSound(clickSoundFile, 1, 0.9)
})

app.use(router)
app.mount('#app')
