<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearAuthSessionStorage, post } from '@/utils/apiUtils'
import BaseModal from '@/components/ui/BaseModal.vue'

const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()

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
          <label>Brukernavn</label>
          <input type="text" placeholder="Brukernavn" disabled title="Ikke tilgjengelig" />
        </div>
      </div>
    </div>

    <div class="settings-actions">
      <button type="button" class="button-variant-1" @click="logout">Logg ut</button>
      <!--no delete-account endpoint in API rn -->
      <button type="button" class="button-warning" disabled title="Ikke tilgjengelig">
        Slett bruker
      </button>
      <!-- no update-username endpoint in API rn -->
      <button type="button" class="button-variant-1" disabled title="Ikke tilgjengelig">
        Lagre endringer
      </button>
    </div>
  </BaseModal>
</template>

