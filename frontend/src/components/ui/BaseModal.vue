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
        <button class="modal-close" @click="requestClose" aria-label="close">✕</button>
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
    position: relative;
    width: min(90vw, 640px);
    max-height: 90vh;
    overflow: auto;
    display: flex;
    flex-direction: column;
    padding: var(--thick-padding);
}

.modal-close {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    background: none;
    border: none;
    cursor: pointer;
    font-size: 1.2rem;
    line-height: 1;
    padding: 0.25rem 0.5rem;
    color: white;
    background-color: rgba(0, 0, 0, 0.7);
    border-radius: 100%;
    padding: .5rem;
}

.modal-close:hover {
    opacity: 1;
}
</style>