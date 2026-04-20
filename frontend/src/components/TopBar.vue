<script setup lang="ts">
import { useRouter } from 'vue-router'
import ProfileButton from '@/components/ProfileButton.vue'

defineProps<{
  title?: string
  profileName?: string | null
  profileAvatarUrl?: string | null
}>()

const emit = defineEmits<{
  (e: 'help'): void
  (e: 'hint'): void
}>()

const router = useRouter()

function goBack(): void {
  router.push('/pupil')
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
      <ProfileButton variant="topbar" />
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
  background: var(--beige-light);
  border: clamp(2px, 0.35vw, 5px) solid var(--brown-dark);
  border-radius: clamp(18px, 3vw, 38px);
  cursor: pointer;
  padding: 0.35rem;
  transition: background 0.15s;
  flex-shrink: 0;
  box-sizing: border-box;
}

.icon-btn:hover {
  transform: scale(1.05);
}
.icon-text {
  font-size: clamp(1.25rem, 2.5vw, 2.25rem);
  line-height: 1;
}

.back-btn {
  height: clamp(64px, 9vw, 122px);
  min-width: clamp(100px, 14vw, 200px);
  padding: 0 1rem;
  background: var(--red-danger);
  color: var(--color-text);
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
  transform: scale(1.05);
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
