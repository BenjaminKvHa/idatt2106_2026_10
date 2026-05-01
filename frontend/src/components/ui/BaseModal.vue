<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useKeyboardTrapFactory } from '@pdanpdan/vue-keyboard-trap'

const props = withDefaults(
  defineProps<{
    closeOnBackdrop?: boolean
    closeOnEscape?: boolean
    panelClass?: string
    showCloseButton?: boolean
    noPadding?: boolean
  }>(),
  {
    closeOnBackdrop: true,
    closeOnEscape: true,
    panelClass: '',
    showCloseButton: true,
    noPadding: false,
  }
)

const emit = defineEmits<{ (e: 'close'): void }>()

const modalWindow = ref<HTMLElement | null>(null)
const useKeyboardTrap = useKeyboardTrapFactory()

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
  document.body.style.overflow = 'hidden'
  document.addEventListener('keydown', onKeydown)
  if (modalWindow.value) {
    useKeyboardTrap(modalWindow.value, { autofocus: true })
  }
})

onBeforeUnmount(() => {
  document.body.style.overflow = ''
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="onBackdropClick">
      <div
        ref="modalWindow"
        class="modal-window"
        :class="[panelClass, { 'modal-window--no-padding': noPadding }]"
        role="dialog"
        aria-modal="true"
      >
        <button v-if="showCloseButton" class="modal-close" @click="requestClose" aria-label="close">✕</button>
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
    justify-content: center;
    align-items: flex-start;
    z-index: 200;
    overflow-y: auto;
    padding: 2rem 0;
}

.modal-window {
    position: relative;
    margin: auto;
    display: flex;
    flex-direction: column;
    padding: var(--thick-padding);
    box-sizing: border-box;
    overflow: hidden;
}

.modal-window--no-padding {
    padding: 0;
}

.modal-window :deep(*) {
    max-width: 100%;
    box-sizing: border-box;
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
    color: inherit;
    opacity: 0.6;
}

.modal-close:hover {
    opacity: 1;
}
</style>