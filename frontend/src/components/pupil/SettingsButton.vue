<script setup lang="ts">
import settingsIcon from '@/assets/icons/homescreen/icon_settings.png'
import { useRouter } from 'vue-router'

const props = withDefaults(
    defineProps<{
      to?: string
      alt?: string
      size?: string
    }>(),
    {
      to: '/pupil/classroom/settings',
      alt: 'Innstillinger',
      size: '150px',
    },
)

const router = useRouter()

function goToSettings(): void {
  router.push(props.to)
}
</script>

<template>
  <button class="settings-btn" @click="goToSettings" :aria-label="alt">
    <img :src="settingsIcon" :alt="alt" :style="{ width: size }" />
  </button>
</template>

<style scoped>
.settings-btn {
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  display: block;
}

.settings-btn img {
  display: block;
  transition: transform 0.15s ease;
}

.settings-btn:hover img {
  transform: scale(1.05);
}
</style>
