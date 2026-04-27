<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronLeftIcon, HomeIcon } from '@lucide/vue'
import { get, type BackendAccountRole } from '@/utils/apiUtils'
import { useRoute, useRouter } from 'vue-router'


const route = useRoute();
const router = useRouter()

const props = defineProps<{
  title?: string
  hint?: string
  help?: string
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

const displayName = computed(() => profile.value?.username || profile.value?.legalName || profile.value?.email || 'No name')

const homeRoute = computed(() => profile.value?.role === 'TEACHER' ? '/teacher/classrooms' : '/pupil')

const profileRoute = computed(() => profile.value?.role === 'TEACHER' ? '/teacher/profile' : '/pupil/profile')

async function loadProfile() {
  try {
    profile.value = await get<MeResponse>('/api/me')
  } catch {
    profile.value = null
  }
}

function openHelp(): void {
  alert(props.help || 'No help')
}

function openHint(): void {
  alert(props.hint || 'No hint')
}

onMounted(async () => {
  await loadProfile()
})

function goBack() {
  if (typeof window !== 'undefined' && !props.previousPageUrl) window.history.back()
  else router.push(props.previousPageUrl || '#');
}

console.log("route", route.fullPath)

const isOnHome = computed(() => route.fullPath == '/pupil/' || route.fullPath == '/pupil' || route.fullPath == '/teacher/classrooms' || route.fullPath == '/teacher/classrooms/');
</script>

<template>
  <div class="mega-nav-container">
    <div class="nav-container heavy-shadow">
      <div>
        <button @click="goBack" v-if="!isOnHome" class="nav-icon-parent transition">
          <ChevronLeftIcon />
        </button>
        <RouterLink :to="homeRoute" v-if="!isOnHome" class="nav-icon-parent transition">
          <HomeIcon />
        </RouterLink>
      </div>
      <div class="nav-center-text">
        {{ title }}
      </div>
      <div>
        <RouterLink :to="profileRoute" class="avatar-button transition">
          <img :src="profileAvatarUrl" />
          {{ displayName }}
        </RouterLink>
      </div>
    </div>
    <div class="extras">
      <button v-if="hint" @click="openHint" class="heavy-shadow transition">
        Få hint!
      </button>
      <button v-if="help" @click="openHelp" class="heavy-shadow transition">
        Hjelp
      </button>
    </div>
  </div>
</template>

<style scoped>
.avatar-button {
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  background-color: var(--beige-light);
  color: var(--brown-dark);
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: .25rem 1rem;
  font-weight: 600;
  gap: .5rem;

  > img {
    width: 2.5rem;
    height: 2.5rem;
    border-radius: 100%;
    background-color: white;
    border: 1px solid var(--brown-dark-40);
  }
}
.avatar-button:hover {
  color: var(--beige-light);
  background-color: var(--brown-dark);
}

.extras {
  position: absolute;
  right: 0;
  top: 5rem;
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
  margin-right: 2rem;
  > button {
    border: var(--border-wide) solid var(--brown-dark);
    border-radius: 1rem;
    padding: .5rem 1rem;
    background-color: var(--beige-light);
    color: var(--brown-dark);
    font-weight: 600;
  }
  > button:hover {
    background-color: var(--brown-dark);
    color: var(--beige-light);
  }
}
.mega-nav-container {
  position: relative;
}
.nav-center-text {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  text-decoration: underline;
  color: var(--brown-dark);
  font-weight: 600;
  white-space: nowrap;
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
  /* background-color: red; */
  stroke: var(--brown-dark);
}
.nav-icon-parent > svg {
  stroke: var(--brown-dark);
}
.nav-icon-parent:hover > svg {
  stroke: var(--beige-light);
}
.nav-icon-parent:hover {
  background-color: var(--brown-dark);
  stroke: var(--beige-light);
}

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

  > div {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
    gap: 1rem;
  }
}
</style>
