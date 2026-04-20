<script setup lang="ts">
import profileIcon from '@/assets/icons/profil.png'
import { useRouter } from 'vue-router'

defineProps<{
  title?: string
}>()

const emit = defineEmits<{
  (e: 'help'): void
  (e: 'hint'): void
}>()

const router = useRouter()

function goBack(): void {
  router.push('/pupil')
}

function goToProfile(): void {
  router.push('/pupil/profile')
}

function openHelp(): void {
  emit('help')
}

function openHint(): void {
  emit('hint')
}
</script>

<template>
  <div class="top-bar">
    <div class="left">
      <button class="icon-btn profile-btn" @click="goToProfile">
        <img :src="profileIcon" alt="Profil" />
        <span class="profile-name">Navn navnesen</span>
      </button>

      <button class="icon-btn" aria-label="Hjelp" @click="openHelp">
        <span class="icon-text">?</span>
      </button>
    </div>

    <div class="center">
      <h1 class="page-title">{{ title }}</h1>
    </div>

    <div class="right">
      <button class="icon-btn" aria-label="Tips" @click="openHint">
        <span class="icon-text">💡</span>
      </button>
      <button class="back-btn" @click="goBack">Tilbake</button>
    </div>
  </div>
</template>

<style scoped>
.top-bar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  width: 100%;
  box-sizing: border-box;
}

.left,
.right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  min-width: 0;
}

.right {
  justify-content: flex-end;
}

.center {
  min-width: 0;
  text-align: center;
}

.page-title {
  font-size: clamp(1.4rem, 4vw, 4rem);
  font-weight: bold;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: clamp(64px, 9vw, 122px);
  height: clamp(64px, 9vw, 122px);
  background: #f0f0f0;
  border: clamp(2px, 0.35vw, 5px) solid var(--brown-dark);
  border-radius: clamp(18px, 3vw, 38px);
  cursor: pointer;
  padding: 0.35rem;
  transition: background 0.15s;
  flex-shrink: 0;
  box-sizing: border-box;
}

.icon-btn:hover {
  background: #ddd;
}

.icon-text {
  font-size: clamp(1.25rem, 2.5vw, 2.25rem);
  line-height: 1;
}

.profile-btn {
  width: auto;
  min-width: clamp(64px, 9vw, 122px);
  padding: 0.4rem 0.6rem;
  gap: 0.25rem;
}

.profile-btn img {
  width: clamp(34px, 5vw, 80px);
  height: clamp(34px, 5vw, 80px);
  object-fit: contain;
  border-radius: 50%;
}

.profile-name {
  font-size: clamp(0.6rem, 1vw, 0.85rem);
  color: var(--color-text);
  white-space: nowrap;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.back-btn {
  height: clamp(64px, 9vw, 122px);
  min-width: clamp(100px, 14vw, 200px);
  padding: 0 1rem;
  background: var(--red-danger);
  color: black;
  font-weight: bold;
  font-size: clamp(1rem, 2.2vw, 2.25rem);
  border: clamp(2px, 0.35vw, 5px) solid var(--brown-dark);
  border-radius: clamp(22px, 4vw, 50px);
  cursor: pointer;
  transition: background 0.15s;
  flex-shrink: 0;
  box-sizing: border-box;
}
.back-btn:hover {
  background: var(--red-danger-80);
}

/**Medium screens*/
@media (max-width: 900px) {
  .top-bar {
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
  }
  .left,
  .right {
    gap: 0.5rem;
  }
  .profile-name {
    display: none;
  }
  .back-btn {
    min-width: 90px;
    padding: 0 0.8rem;
  }
}

/**Small screens*/
@media (max-width: 600px) {
  .top-bar {
    grid-template-columns: auto 1fr auto;
    gap: 0.4rem;
    padding: 0.4rem 0.5rem;
  }
  .left,
  .right {
    gap: 0.35rem;
  }
  .page-title {
    font-size: clamp(1rem, 3.8vw, 1.6rem);
  }
  .back-btn {
    min-width: 80px;
    font-size: clamp(0.9rem, 2.8vw, 1.2rem);
  }
}

</style>
