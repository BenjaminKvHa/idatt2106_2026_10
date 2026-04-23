<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'

const props = withDefaults(
  defineProps<{
    closeOnBackdrop?: boolean
    closeOnEscape?: boolean
    panelClass?: string
  }>(),
  {
    closeOnBackdrop: true,
    closeOnEscape: true,
    panelClass: '',
  }
)

const emit = defineEmits<{ (e: 'close'): void }>()

function requestClose() {
  emit('close')
}

function onBackdropClick() {
  if (props.closeOnBackdrop) {
    requestClose()
  }
}

function onKeydown(event: KeyboardEvent) {
  if (props.closeOnEscape && event.key === 'Escape') {
    requestClose()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="onBackdropClick">
      <div class="modal-window" :class="panelClass" role="dialog" aria-modal="true">
        <slot />
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
    position: fixed;
    inset: 0;
    background: var(--obscure);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 200;
}

.modal-window {
    width: min(90vw, 640px);
    max-height: 90vh;
    overflow: auto;
}
</style>