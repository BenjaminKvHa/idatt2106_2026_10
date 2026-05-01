import { ref, watchEffect } from 'vue'

const VOLUME_KEY = 'nettdet_volume'
const MUTED_KEY = 'nettdet_muted'

const volume = ref(65)
const isMuted = ref(false)
const audioEl = ref<HTMLAudioElement | null>(null)
let audioElRelativeVolume = 1
const activeSounds = new Map<HTMLAudioElement, number>()

function getEffectiveVolume(relativeVolume = 1): number {
  const safeRelativeVolume = Number.isFinite(relativeVolume) ? Math.max(0, relativeVolume) : 1
  return Math.min(1, (volume.value / 100) * safeRelativeVolume)
}

if (typeof window !== 'undefined') {
  const savedVolume = localStorage.getItem(VOLUME_KEY)
  if (savedVolume !== null) {
    const n = Number(savedVolume)
    if (!isNaN(n) && n >= 0 && n <= 100) volume.value = n
  }
  const savedMuted = localStorage.getItem(MUTED_KEY)
  if (savedMuted !== null) isMuted.value = savedMuted === 'true'

  watchEffect(() => {
    if (!audioEl.value) return
    audioEl.value.volume = isMuted.value ? 0 : getEffectiveVolume(audioElRelativeVolume)
  })
}

function applyVolume() {
  if (!audioEl.value) return
  audioEl.value.volume = isMuted.value ? 0 : getEffectiveVolume(audioElRelativeVolume)
}

export function useAudio() {
  function registerAudioElement(el: HTMLAudioElement, relativeVolume = 1) {
    audioEl.value = el
    audioElRelativeVolume = relativeVolume
    applyVolume()
  }

  function setVolume(val: number) {
    volume.value = Math.max(0, Math.min(100, val))
    localStorage.setItem(VOLUME_KEY, String(volume.value))
    for (const [sound, relVol] of activeSounds) {
      sound.volume = getEffectiveVolume(relVol)
    }
    applyVolume()
  }

  function toggleMute() {
    isMuted.value = !isMuted.value
    localStorage.setItem(MUTED_KEY, String(isMuted.value))
    if (isMuted.value) {
      for (const sound of activeSounds.keys()) {
        sound.pause()
      }
      activeSounds.clear()
    }
    applyVolume()
  }

  function playSound(src: string, relVol = 1, startTime = 0): HTMLAudioElement | null {
    if (isMuted.value) return null
    const audio = new Audio(src)
    audio.volume = getEffectiveVolume(relVol)
    if (startTime > 0) audio.currentTime = startTime
    activeSounds.set(audio, relVol)
    audio.addEventListener('ended', () => activeSounds.delete(audio), { once: true })
    audio.play().catch((e) => {
      console.error('useAudio: playSound failed for', src, e)
    })
    return audio
  }

  function stopSound(audio: HTMLAudioElement | null) {
    if (!audio) return
    audio.pause()
    audio.currentTime = 0
    activeSounds.delete(audio)
  }

  return { volume, isMuted, registerAudioElement, setVolume, toggleMute, playSound, stopSound }
}
