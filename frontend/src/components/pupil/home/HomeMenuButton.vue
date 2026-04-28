<script setup lang="ts">
defineProps<{
  to: string
  image: string
  alt: string
  className?: string
}>()
</script>

<template>
  <RouterLink :to="to" class="home-menu-button" :class="className">
    <img :src="image" :alt="alt" />
  </RouterLink>
</template>

<style scoped>
.home-menu-button {
  position: absolute;
  cursor: pointer;
  width: clamp(200px, 20vw, 200px);
}

.home-menu-button img {
  display: block;
  width: 100%;
  transition: transform 0.15s ease;
}

.home-menu-button:hover img {
  transform: scale(1.05);
}
</style>
