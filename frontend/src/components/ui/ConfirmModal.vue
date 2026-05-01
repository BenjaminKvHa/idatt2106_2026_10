<script setup lang="ts">
import BaseModal from '@/components/ui/BaseModal.vue'

defineProps<{
  message: string
  confirmLabel?: string
  cancelLabel?: string
}>()

const emit = defineEmits<{
  (e: 'confirm'): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <BaseModal :close-on-backdrop="true" :close-on-escape="true" @close="emit('cancel')">
    <p class="confirm-modal-message">{{ message }}</p>
    <div class="confirm-modal-actions">
      <button type="button" class="button-warning" @click="emit('confirm')">
        {{ confirmLabel ?? 'Bekreft' }}
      </button>
      <button type="button" class="button-variant-1" @click="emit('cancel')">
        {{ cancelLabel ?? 'Avbryt' }}
      </button>
    </div>
  </BaseModal>
</template>

<style scoped>
.confirm-modal-message {
  margin: 0 0 var(--medium-gap);
  font-size: 1rem;
  line-height: 1.5;
  color: var(--txt);
  text-align: center;
}

.confirm-modal-actions {
  display: flex;
  gap: var(--thin-gap);
  justify-content: center;
  flex-wrap: wrap;
}

.confirm-modal-actions button {
  flex: 1;
  min-width: 100px;
  padding: var(--thin-padding) var(--medium-padding);
  font-size: 0.9rem;
  font-weight: var(--fw-semibold);
  cursor: pointer;
}
</style>
