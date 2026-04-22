<script setup lang="ts">
import { useRouter } from 'vue-router'
import { user } from '@/components/exampleUser'
import { computed } from 'vue'
import { getJwtData } from '@/utils/jwtUtils';

const props = withDefaults(
  defineProps<{
    to?: string
    variant?: 'topbar' | 'home'
  }>(),
  {
    to: '/pupil/profile',
    variant: 'topbar',
  },
)

const router = useRouter()

function getUsername(): string {
  const jwtData = getJwtData();
  if (jwtData) {
    return jwtData.legalName || jwtData.email || "Bruker uten navn"
  }
  return "Uten navn"
}

const displayName = computed(() => getUsername())
const displayAvatar = computed(() => user.value.avatarUrl)
function goToProfile() {
  router.push(props.to)
  history.back();
}
</script>

<template>
  <button
    class="profile-btn"
    :class="`profile-btn--${variant}`"
    @click="goToProfile"
    aria-label="Profil"
  >
    <img :src="displayAvatar" :alt="displayName" />
    <span class="profile-name">{{ displayName }}</span>
  </button>
</template>

<style scoped>
.profile-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: var(--beige-light);
  border: clamp(2px, 0.35vw, 5px) solid var(--brown-dark);
  border-radius: clamp(18px, 3vw, 38px);
  transition: background 0.15s;
  box-sizing: border-box;
  padding: 0.35rem 0.45rem;
  gap: 0.2rem;
}

.profile-btn:hover {
  transform: scale(1.05);
}

.profile-name {
  color: var(--color-text);
  text-align: center;
  max-width: 100%;
}


.profile-btn--topbar {
  width: clamp(64px, 9vw, 122px);
  height: clamp(64px, 9vw, 122px);
}

.profile-btn--topbar img {
  width: clamp(34px, 5vw, 62px);
  height: clamp(34px, 5vw, 62px);
  object-fit: cover;
  border-radius: 50%;
}

.profile-btn--topbar .profile-name {
  font-size: clamp(0.55rem, 0.8vw, 0.8rem);
  line-height: 1.05;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.profile-btn--home {
  width: 150px;
  height: 150px;
}

.profile-btn--home img {
  width: 110px;
  height: 110px;
  object-fit: cover;
  border-radius: 50%;
}

.profile-btn--home .profile-name {
  font-size: 0.95rem;
  line-height: 1.1;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}
</style>
