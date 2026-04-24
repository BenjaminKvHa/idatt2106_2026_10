import { ref } from 'vue'

const VOLUME_KEY = 'nettdet_volume'
const MUTED_KEY = 'nettdet_muted'

const volume = ref(65)
const isMuted = ref(false)
const audioEl = ref<HTMLAudioElement | null>(null)

if (typeof window !== 'undefined') {
  const savedVolume = localStorage.getItem(VOLUME_KEY)
  if (savedVolume !== null) {
    const n = Number(savedVolume)
    if (!isNaN(n) && n >= 0 && n <= 100) volume.value = n
  }
  const savedMuted = localStorage.getItem(MUTED_KEY)
  if (savedMuted !== null) isMuted.value = savedMuted === 'true'
}

function applyVolume() {
  if (!audioEl.value) return
  audioEl.value.volume = isMuted.value ? 0 : volume.value / 100
}

export function useAudio() {
  function registerAudioElement(el: HTMLAudioElement) {
    audioEl.value = el
    applyVolume()
  }

  function setVolume(val: number) {
    volume.value = Math.max(0, Math.min(100, val))
    localStorage.setItem(VOLUME_KEY, String(volume.value))
    applyVolume()
  }

  function toggleMute() {
    isMuted.value = !isMuted.value
    localStorage.setItem(MUTED_KEY, String(isMuted.value))
    applyVolume()
  }

  function playSound(src: string, relVol = 1): HTMLAudioElement {
    const audio = new Audio(src)
    audio.volume = isMuted.value ? 0 : (volume.value / 100) * relVol
    audio.play().catch(() => {})
    return audio
  }

  return { volume, isMuted, registerAudioElement, setVolume, toggleMute, playSound }
}
