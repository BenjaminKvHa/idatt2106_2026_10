<script setup lang="ts">
import { h, onBeforeUnmount, ref, render, watch } from 'vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { getBlobUrl } from '@/utils/apiUtils'

defineOptions({
  name: 'AuthenticatedImage',
})

const props = defineProps<{
  src?: string | null
  alt: string
  previewOnClick?: boolean
}>()

const objectUrl = ref('')
let previewContainer: HTMLDivElement | null = null

function revokeObjectUrl() {
  if (!objectUrl.value) {
    return
  }

  URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
}

function closePreview() {
  if (!previewContainer) {
    return
  }

  render(null, previewContainer)
  previewContainer.remove()
  previewContainer = null
}

function openPreview() {
  if (!props.previewOnClick || !objectUrl.value || previewContainer) {
    return
  }

  previewContainer = document.createElement('div')
  document.body.appendChild(previewContainer)

  render(
    h(
      BaseModal,
      {
        panelClass: 'authenticated-image-modal',
        onClose: closePreview,
      },
      {
        default: () => [
          h('img', {
            src: objectUrl.value,
            alt: props.alt,
            class: 'authenticated-image-modal__image',
          }),
        ],
      },
    ),
    previewContainer,
  )
}

watch(
  () => props.src,
  async (src) => {
    closePreview()
    revokeObjectUrl()

    if (!src) {
      return
    }

    try {
      objectUrl.value = await getBlobUrl(src)
    } catch (error) {
      console.warn('Failed to load authenticated image', src, error)
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  closePreview()
  revokeObjectUrl()
})
</script>

<template>
  <img
    v-if="objectUrl"
    :src="objectUrl"
    :alt="alt"
    :class="{ 'preview-enabled': previewOnClick }"
    :role="previewOnClick ? 'button' : undefined"
    :tabindex="previewOnClick ? 0 : undefined"
    @click="openPreview"
    @keydown.enter.prevent="openPreview"
    @keydown.space.prevent="openPreview"
  />
</template>

<style>
.preview-enabled {
  cursor: zoom-in;
}
.preview-enabled:focus-visible {
  outline: 3px solid var(--brown-dark) !important;
  outline-offset: 3px;
  border-radius: 4px;
}

.authenticated-image-modal {
  width: min(92vw, 960px);
  padding: 1rem;
  background: transparent;
  border: none;
  outline: none;
  box-shadow: none;
}

.authenticated-image-modal__image {
  display: block;
  width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 1rem;
  background: rgba(0, 0, 0, 0.15);
}
</style>
