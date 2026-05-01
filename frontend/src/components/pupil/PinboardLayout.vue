<script setup lang="ts">
import boardBackground from '@/assets/backgrounds/korktavle.png'

defineProps<{
  headingId?: string
}>()
</script>

<template>
  <main class="pinboard-layout-page pupil-scene-page">
    <section class="pupil-pinboard-stage" :aria-labelledby="headingId">
      <img class="pupil-pinboard-image" :src="boardBackground" alt="" aria-hidden="true" />
      <div class="pupil-board-blobby-wrapper" aria-hidden="true"></div>

      <slot />
    </section>
  </main>
</template>

<style scoped>
.pinboard-layout-page {
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
}
</style>
