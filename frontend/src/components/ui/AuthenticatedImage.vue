<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getBlobUrl } from '@/utils/apiUtils'

defineOptions({
  name: 'AuthenticatedImage',
})

const props = defineProps<{
  src?: string | null
  alt: string
}>()

const objectUrl = ref('')

function revokeObjectUrl() {
  if (!objectUrl.value) {
    return
  }

  URL.revokeObjectURL(objectUrl.value)
  objectUrl.value = ''
}

watch(
  () => props.src,
  async (src) => {
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
  revokeObjectUrl()
})
</script>

<template>
  <img v-if="objectUrl" :src="objectUrl" :alt="alt" v-bind="$attrs" />
</template>
