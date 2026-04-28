<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearAuthSessionStorage, post } from '@/utils/apiUtils'
import BaseModal from '@/components/ui/BaseModal.vue'
import { useAudio } from '@/composables/useAudio'
import volume2Icon from '@/assets/icons/volume-2.svg'
import volumeXIcon from '@/assets/icons/volume-x.svg'

const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const { volume, isMuted, setVolume, toggleMute } = useAudio()

async function logout() {
  try {
    await post('/api/auth/logout', {})
  } catch {
    console.error('Logout API call failed, proceeding with client-side logout.')
  }
  clearAuthSessionStorage()
  router.push('/sign-in')
}

function close() {
  emit('close')
}
</script>

<template>
  <BaseModal panel-class="teacher-variant-1 settings-modal-box" @close="close">
    <h2 class="settings-modal-title">Innstillinger</h2>

    <div class="settings-grid">
      <!--no API endpoint for changing username rn -->
      <div style="display: flex; flex-direction: column; gap: var(--thin-gap);">
        <div class="settings-field">
          <label for="options-volume-slider">Volum</label>
          <div class="volume-row">
            <button type="button" class="mute-btn" :aria-label="isMuted ? 'Skru på lyd' : 'Demp lyd'" @click="toggleMute">
              <img :src="isMuted ? volumeXIcon : volume2Icon" alt="" aria-hidden="true" class="volume-icon" />
            </button>
            <input
              id="options-volume-slider"
              :value="volume"
              type="range"
              min="0"
              max="100"
              :disabled="isMuted"
              @input="setVolume(+($event.target as HTMLInputElement).value)"
            />
            <span class="volume-label">{{ isMuted ? 'Dempet' : volume + '%' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="settings-actions">
      <button type="button" class="button-variant-1" @click="logout">Logg ut</button>
    </div>
  </BaseModal>
</template>

<style scoped>
.settings-modal-box {
    min-width: 320px;
    max-width: 500px;
    width: 90vw;
    gap: var(--medium-gap);
}

.settings-modal-title {
    font-size: 1.3rem;
    font-weight: var(--fw-bold);
    text-align: center;
    color: var(--txt);
    margin: 0;
}

.settings-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--medium-gap);
    align-items: start;
}

.settings-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.settings-field label {
    font-size: 0.85rem;
    font-weight: var(--fw-bold);
    color: var(--txt);
}

.settings-field input {
    padding: var(--thin-padding);
    border-radius: var(--card-item-radius);
    border: 2px solid var(--accent);
    background: var(--background);
    color: var(--txt);
    font-size: 0.9rem;
}

.settings-field input:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.settings-actions {
    display: flex;
    gap: var(--thin-gap);
    flex-wrap: wrap;
    justify-content: center;
}

.settings-actions button {
    flex: 1;
    min-width: 100px;
    padding: var(--thin-padding) var(--medium-padding);
    cursor: pointer;
    font-size: 0.9rem;
    font-weight: var(--fw-semibold);
}

</style>
