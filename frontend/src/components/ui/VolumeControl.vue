<script setup lang="ts">
import { useAudio } from '@/composables/useAudio'
import volume2Icon from '@/assets/icons/volume-2.svg'
import volumeXIcon from '@/assets/icons/volume-x.svg'

withDefaults(defineProps<{ inputId?: string }>(), { inputId: 'volume-control-slider' })

const { volume, isMuted, setVolume, toggleMute } = useAudio()
</script>

<template>
  <div class="volume-control">
    <button
      type="button"
      class="mute-btn"
      :aria-label="isMuted ? 'Skru på lyd' : 'Demp lyd'"
      @click="toggleMute"
    >
      <img :src="isMuted ? volumeXIcon : volume2Icon" alt="" aria-hidden="true" class="volume-icon" />
    </button>
    <input
      :id="inputId"
      :value="volume"
      type="range"
      min="0"
      max="100"
      :disabled="isMuted"
      @input="setVolume(+($event.target as HTMLInputElement).value)"
    />
    <span class="volume-label">{{ isMuted ? 'Dempet' : volume + '%' }}</span>
  </div>
</template>

<style scoped>
.volume-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mute-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.volume-icon {
  width: 20px;
  height: 20px;
}

.volume-label {
  min-width: 4ch;
  font-size: 0.85rem;
}

input[type='range'] {
  flex: 1;
  min-width: 80px;
  cursor: pointer;
}

input[type='range']:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
