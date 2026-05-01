<script setup lang="ts">
import blobbyImage from '@/assets/icons/blobby/creaturePeaking.png'
import boardBackground from '@/assets/backgrounds/korktavle.png'
import boardBackgroundLarge from '@/assets/backgrounds/korktavle-stor.png'

defineProps<{
  headingId?: string
}>()
</script>

<template>
  <main class="pinboard-layout-page pupil-scene-page">
    <section class="pupil-pinboard-stage" :aria-labelledby="headingId">
      <img class="pupil-pinboard-image" :src="boardBackground" alt="" aria-hidden="true" />
      <img class="pupil-pinboard-image-large" :src="boardBackgroundLarge" alt="" aria-hidden="true" />
      <div class="pupil-board-blobby-wrapper" aria-hidden="true">
        <img class="pupil-board-blobby-image" :src="blobbyImage" alt="" />
      </div>

      <slot />
    </section>
  </main>
</template>

<style scoped>
.pinboard-layout-page {
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
}
</style>
