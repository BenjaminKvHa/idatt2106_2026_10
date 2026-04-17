<script setup lang="ts">
import profileIcon from '@/assets/icons/profil.png'
import { useRouter } from 'vue-router'

// 👇 legg til props
defineProps<{
  title?: string
}>()

const router = useRouter()

function goBack() {
  router.push('/pupil')
}

function goToProfile() {
  router.push('/pupils-profile')
}
</script>

<template>
  <div class="top-bar">
    <!-- Venstre side -->
    <div class="left">
      <!-- Profile -->
      <button class="icon-btn profile-btn" @click="goToProfile">
        <img :src="profileIcon" alt="Profil" />
        <span class="profile-name">Navn navnesen</span>
      </button>

      <!-- Help -->
      <button class="icon-btn" aria-label="Hjelp">
        <span class="icon-text">?</span>
      </button>
    </div>

    <!-- 👇 NY: Tittel i midten -->
    <div class="center">
      <h1 class="page-title">{{ title }}</h1>
    </div>

    <!-- Høyre side -->
    <div class="right">
      <!-- Tips -->
      <button class="icon-btn" aria-label="Tips">
        <span class="icon-text">💡</span>
      </button>

      <!-- Back -->
      <button class="back-btn" @click="goBack">Tilbake</button>
    </div>
  </div>
</template>

<style scoped>
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between; /* viktig */
  padding: 10px 16px;
}

/* grupper */
.left,
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* midten */
.center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.page-title {
  font-size: 32px;
  font-weight: bold;
  margin: 0;
}

/* resten er ditt originale */

.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 122px;
  height: 122px;
  background: #f0f0f0;
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  cursor: pointer;
  padding: 6px;
  transition: background 0.15s;
}

.icon-btn:hover {
  background: #ddd;
}

.icon-text {
  font-size: 36px;
  line-height: 1;
}

.profile-btn {
  width: auto;
  min-width: 122px;
  padding: 6px 10px;
  gap: 4px;
}

.profile-btn img {
  width: 80px;
  height: 80px;
  object-fit: contain;
  border-radius: 50%;
}

.profile-name {
  font-size: 11px;
  color: #333;
  white-space: nowrap;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.back-btn {
  height: 122px;
  width: 200px;
  padding: 0 22px;
  background: #e63946;
  color: black;
  font-weight: bold;
  font-size: 36px;
  border: 5px solid var(--brown-dark);
  border-radius: 50px;
  cursor: pointer;
  transition: background 0.15s;
}

.back-btn:hover {
  background: #c1121f;
}
</style>
