import { config } from '@vue/test-utils'

if (typeof window !== 'undefined' && typeof window.localStorage?.getItem !== 'function') {
  const storage = new Map<string, string>()

  Object.defineProperty(window, 'localStorage', {
    value: {
      getItem: (key: string) => storage.get(key) ?? null,
      setItem: (key: string, value: string) => storage.set(key, value),
      removeItem: (key: string) => storage.delete(key),
      clear: () => storage.clear(),
    },
    configurable: true,
  })
}

config.global.stubs = {
  ...config.global.stubs,
  BaseModal: {
    props: {
      panelClass: { type: String, default: '' },
      backdropClass: { type: String, default: '' },
      closeOnBackdrop: { type: Boolean, default: true },
    },
    emits: ['close'],
    template: `
      <div
        class="modal-backdrop overlay popup-overlay"
        :class="backdropClass"
        @click.self="closeOnBackdrop && $emit('close')"
      >
        <div class="modal-window center-container" :class="panelClass">
          <div class="guide-text">
            <slot />
          </div>
        </div>
      </div>
    `,
  },
  ConfirmModal: {
    props: {
      message: { type: String, required: true },
      confirmLabel: { type: String, default: 'Bekreft' },
      cancelLabel: { type: String, default: 'Avbryt' },
    },
    emits: ['confirm', 'cancel'],
    template: `
      <div class="confirm-modal-panel teacher-confirm-popup leave-classroom-popup unsaved-notes-popup">
        <p class="confirm-modal-message">{{ message }}</p>
        <button
          type="button"
          class="button-warning confirm-delete-classroom confirm-leave-classroom confirm-remove-pupil confirm-reject-pending confirm-discard-notes confirm-discard-username confirm-leave-button"
          @click="$emit('confirm')"
        >
          {{ confirmLabel }}
        </button>
        <button
          type="button"
          class="button-variant-1 cancel-delete-classroom cancel-leave-classroom cancel-remove-pupil cancel-reject-pending cancel-leave-button"
          @click="$emit('cancel')"
        >
          {{ cancelLabel }}
        </button>
      </div>
    `,
  },
}
