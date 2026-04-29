<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronLeftIcon, HomeIcon } from '@lucide/vue'
import { get, type BackendAccountRole } from '@/utils/apiUtils.ts'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const props = defineProps<{
  title?: string
  previousPageUrl?: string
}>()

type MeResponse = {
  email: string
  legalName: string
  role: BackendAccountRole
  username: string | null
  avatarFileObjectId: number | null
}

const profile = ref<MeResponse | null>(null)

const profileAvatarUrl = computed(() =>
  profile.value?.avatarFileObjectId
    ? `/api/objects/${profile.value.avatarFileObjectId}`
    : '/api/avatar-assets/costume/detective.png',
)

const displayName = computed(
  () => profile.value?.username || profile.value?.legalName || profile.value?.email || 'No name',
)

const homeRoute = computed(() =>
  profile.value?.role === 'TEACHER' ? '/teacher/classrooms' : '/pupil',
)

const profileRoute = computed(() =>
  profile.value?.role === 'TEACHER' ? '/teacher/profile' : '/pupil/profile',
)

async function loadProfile() {
  try {
    profile.value = await get<MeResponse>('/api/me')
  } catch {
    profile.value = null
  }
}

onMounted(loadProfile)

function goBack() {
  if (typeof window !== 'undefined' && !props.previousPageUrl) {
    window.history.back()
  } else {
    router.push(props.previousPageUrl || '#')
  }
}

const isOnHome = computed(
  () =>
    route.fullPath === '/pupil/' ||
    route.fullPath === '/pupil' ||
    route.fullPath === '/teacher/classrooms' ||
    route.fullPath === '/teacher/classrooms/',
)
</script>

<template>
  <header class="topbar">
    <div class="topbar-shade"></div>

    <div class="topbar-left">
      <button
        v-if="!isOnHome"
        @click="goBack"
        class="nav-icon-parent transition"
        aria-label="Tilbake"
      >
        <ChevronLeftIcon :size="42" :stroke-width="3" />
      </button>

      <RouterLink
        v-if="!isOnHome"
        :to="homeRoute"
        class="nav-icon-parent transition"
        aria-label="Hjem"
      >
        <HomeIcon :size="36" :stroke-width="3" />
      </RouterLink>
    </div>

    <h1 class="nav-center-text">
      {{ title }}
    </h1>

    <div class="topbar-right">
      <RouterLink :to="profileRoute" class="avatar-button transition">
        <img :src="profileAvatarUrl" />
        <span class="username-text">{{ displayName }}</span>
      </RouterLink>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  position: relative;
  width: 100%;
  height: 4.2rem;

  display: grid;
  grid-template-columns: 12rem 1fr 12rem;
  align-items: center;

  padding: 0 1rem;
  box-sizing: border-box;
  overflow: hidden;
  z-index: 50;

  background-color: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(30px);

  border-bottom: 1px solid rgba(255, 255, 255, 0.2);

  box-shadow:
    0 2px 0 rgba(0, 0, 0, 0.4),
    0 6px 12px rgba(0, 0, 0, 0.35);
}

.topbar > * {
  position: relative;
  z-index: 2;
}

.topbar-left {
  grid-column: 1;
  display: flex;
  align-items: center;
  justify-self: start;
  gap: 0.5rem;
}

.topbar-left,
.topbar-right {
  align-self: center;
  transform: translateY(-4px);
}

.nav-center-text {
  grid-column: 2;
  margin: 0;
  justify-self: center;
  text-align: center;

  font-family: 'Inika', serif;
  font-size: clamp(1.1rem, 2vw, 1.5rem);
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;

  transform: translateY(-4px);

  color: #ffffff;

  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8);

  white-space: nowrap;
}

.topbar-right {
  grid-column: 3;
  display: flex;
  align-items: center;
  justify-self: end;
}

.nav-icon-parent {
  width: 3.1rem;
  height: 3.1rem;
  border-radius: 50%;

  background: rgba(255, 255, 255, 0.65);

  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);

  border: 2px solid rgba(0, 0, 0, 0.7);

  box-shadow:
    0 3px 0 rgba(0, 0, 0, 0.5),
    0 6px 12px rgba(0, 0, 0, 0.25);

  display: flex;
  justify-content: center;
  align-items: center;

  color: #111;

  transition: all 0.15s ease;
}

.nav-icon-parent:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.8);
}

.nav-icon-parent:active {
  background: #ffffff;

  transform: translateY(2px);

  box-shadow:
    0 1px 0 rgba(0, 0, 0, 0.5),
    0 3px 8px rgba(0, 0, 0, 0.2);
}

.nav-icon-parent svg {
  width: 1.4rem;
  height: 1.4rem;
  stroke-width: 1.75;
  stroke: #111;
  color: #111;
}

.avatar-button {
  display: flex;
  align-items: center;
  gap: 0.6rem;

  padding: 0.35rem 0.9rem;

  border-radius: 999px;

  background: rgba(255, 255, 255, 0.65);

  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);

  border: 2px solid rgba(0, 0, 0, 0.7);

  box-shadow:
    0 3px 0 rgba(0, 0, 0, 0.5),
    0 6px 12px rgba(0, 0, 0, 0.25);

  color: #111;
  font-weight: 600;

  transition: all 0.15s ease;
}

.avatar-button:hover {
  background: rgba(255, 255, 255, 0.8);
}

.avatar-button:active {
  background: #ffffff;

  transform: translateY(2px);

  box-shadow:
    0 1px 0 rgba(0, 0, 0, 0.5),
    0 3px 8px rgba(0, 0, 0, 0.2);
}

.avatar-button img {
  width: 1.9rem;
  height: 1.9rem;
  border-radius: 50%;
  border: 2px solid #1a1a1a;
}

@media (max-width: 800px) {
  .topbar {
    height: 3rem;
    grid-template-columns: 6rem 1fr 6rem;
    padding: 0 0.5rem;
  }

  .nav-icon-parent {
    width: 2.4rem;
    height: 2.4rem;
  }

  .nav-center-text {
    font-size: 1rem;
  }

  .username-text {
    display: none;
  }
}
</style>
