import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import clickSoundFile from '@/assets/click-sound.mp3'

const app = createApp(App)

const clickSound = new Audio(clickSoundFile)
clickSound.volume = 1

document.addEventListener('click', (e) => {
  const target = e.target as HTMLElement | null
  if (!target) return

  const clickableElement = target.closest(
    'button:not(:disabled), a, [role="button"], .clickable'
  )

  if (!clickableElement) return

  clickSound.pause()
  clickSound.currentTime = 0.9
  clickSound.play().catch(() => {})
})

app.use(router)
app.mount('#app')
