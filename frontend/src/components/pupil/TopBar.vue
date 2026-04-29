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
  <div class="mega-nav-container">
    <div class="nav-container heavy-shadow">
      <!-- VENSTRE -->
      <div>
        <button v-if="!isOnHome" @click="goBack" class="nav-icon-parent transition">
          <ChevronLeftIcon />
        </button>

        <RouterLink v-if="!isOnHome" :to="homeRoute" class="nav-icon-parent transition">
          <HomeIcon />
        </RouterLink>
      </div>

      <!-- MIDTEN -->
      <div class="nav-center-text">
        {{ title }}
      </div>

      <!-- HØYRE -->
      <div>
        <RouterLink :to="profileRoute" class="avatar-button transition">
          <img :src="profileAvatarUrl" />
          <span class="username-text">{{ displayName }}</span>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mega-nav-container {
  position: relative;
}

/* MAIN BAR */
.nav-container {
  position: relative;
  background-color: var(--beige-medium);
  width: 100%;
  height: 5rem;
  display: flex;
  justify-content: space-between;
  padding-left: 2rem;
  padding-right: 2rem;
  border-bottom: var(--border-wide) solid var(--brown-dark);
}

/* FLEX CHILDREN */
.nav-container > div {
  display: flex;
  align-items: center;
  gap: 1rem;
}

/* CENTER TITLE */
.nav-center-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  color: var(--brown-dark);
  font-weight: 600;
  white-space: nowrap;
  text-decoration: none;
}

.nav-icon-parent {
  width: 3rem;
  height: 3rem;
  border-radius: 100%;
  border: var(--border-thin) solid var(--brown-dark);
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--beige-light);

  color: var(--brown-dark);
}

.nav-icon-parent:hover {
  background-color: var(--brown-dark);
  color: var(--beige-light);
}

.avatar-button {
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  background-color: var(--beige-light);
  color: var(--brown-dark);
  text-decoration: none;
  display: flex;
  align-items: center;
  padding: 0.25rem 1rem;
  font-weight: 600;
  gap: 0.5rem;
}

.avatar-button:hover {
  color: var(--beige-light);
  background-color: var(--brown-dark);
}

.avatar-button img {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 100%;
  background-color: white;
  border: 1px solid var(--brown-dark-40);
}

.username-text {
  text-decoration: none;
}
</style>
