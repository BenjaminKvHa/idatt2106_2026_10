<script setup lang="ts">
import profileIcon from '@/assets/icons/profil.png'

const emit = defineEmits(['click'])
</script>

<template>
  <button class="profile-button" @click="emit('click')">
    <img :src="profileIcon" alt="Profil" />
  </button>
</template>

<style scoped>
.profile-button {
  width: 92px;
  height: 92px;
  border-radius: 22px;
  border: 4px solid #7a5b43;
  background: #ffffff;

  display: flex;
  align-items: center;
  justify-content: center;

  cursor: pointer;
  overflow: hidden;
}

.profile-button img {
  width: 56px;
  height: 56px;
  object-fit: contain;
}
</style>
