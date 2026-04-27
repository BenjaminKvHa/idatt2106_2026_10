<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ChevronLeftIcon, HomeIcon } from '@lucide/vue'
import { get, type BackendAccountRole } from '@/utils/apiUtils'
import { useRoute, useRouter } from 'vue-router'
import hintCreature from '@/assets/creatureSittingQuestion.png'

const route = useRoute()
const router = useRouter()

const props = defineProps<{
  title?: string
  hint?: string
  help?: string
  previousPageUrl?: string
}>()

const emit = defineEmits<{
  (e: 'hint'): void
}>()

type MeResponse = {
  email: string
  legalName: string
  role: BackendAccountRole
  username: string | null
  avatarFileObjectId: number | null
}

const profile = ref<MeResponse | null>(null)

const showHintPopup = ref(false)
const showHintInstruction = ref(false)
const hasClickedHintCreature = ref(false)

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

const isOnHome = computed(
  () =>
    route.fullPath === '/pupil/' ||
    route.fullPath === '/pupil' ||
    route.fullPath === '/teacher/classrooms' ||
    route.fullPath === '/teacher/classrooms/',
)

async function loadProfile() {
  try {
    profile.value = await get<MeResponse>('/api/me')
  } catch {
    profile.value = null
  }
}

function openHint(): void {
  if (!hasClickedHintCreature.value) {
    hasClickedHintCreature.value = true
    showHintInstruction.value = true
    return
  }

  emit('hint')

  if (props.hint) {
    showHintPopup.value = true
  }
}

function goBack() {
  if (typeof window !== 'undefined' && !props.previousPageUrl) {
    window.history.back()
  } else {
    router.push(props.previousPageUrl || '#')
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="mega-nav-container">
    <div class="nav-container heavy-shadow">
      <div class="nav-left">
        <button v-if="!isOnHome" type="button" @click="goBack" class="nav-icon-parent transition">
          <ChevronLeftIcon />
        </button>

        <RouterLink v-if="!isOnHome" :to="homeRoute" class="nav-icon-parent transition">
          <HomeIcon />
        </RouterLink>
      </div>

      <div class="nav-center-text">
        {{ title }}
      </div>

      <div class="nav-right">
        <RouterLink :to="profileRoute" class="avatar-button transition">
          <img :src="profileAvatarUrl" />
          <span>{{ displayName }}</span>
        </RouterLink>
      </div>
    </div>

    <div v-if="hint" class="extras">
      <button
        type="button"
        class="hint-creature-button transition"
        @click="openHint"
        aria-label="Få hint"
      >
        <img :src="hintCreature" alt="Få hint" />
      </button>
    </div>

    <div v-if="showHintInstruction" class="popup-overlay" @click="showHintInstruction = false">
      <div class="topbar-popup small-popup" @click.stop>
        <h2>Trykk på meg for å få et hint!</h2>

        <button class="popup-close-button" @click="showHintInstruction = false">Ok</button>
      </div>
    </div>

    <div v-if="showHintPopup" class="popup-overlay" @click="showHintPopup = false">
      <div class="topbar-popup" @click.stop>
        <h2>Tips 💡</h2>
        <p>{{ hint }}</p>

        <button class="popup-close-button" @click="showHintPopup = false">Ok</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mega-nav-container {
  position: relative;
  z-index: 100;
}

.nav-container {
  background-color: var(--beige-medium);
  width: 100%;
  height: 5rem;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding-left: 2rem;
  padding-right: 2rem;
  border-bottom: var(--border-wide) solid var(--brown-dark);
  box-sizing: border-box;
}

.nav-left,
.nav-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.nav-left {
  justify-content: flex-start;
}

.nav-right {
  justify-content: flex-end;
}

.nav-center-text {
  text-decoration: underline;
  color: var(--brown-dark);
  font-weight: 600;
  text-align: center;
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
  stroke: var(--brown-dark);
  cursor: pointer;
  padding: 0;
  box-sizing: border-box;
}

.nav-icon-parent > svg {
  stroke: var(--brown-dark);
}

.nav-icon-parent:hover {
  background-color: var(--brown-dark);
}

.nav-icon-parent:hover > svg {
  stroke: var(--beige-light);
}

.avatar-button,
.avatar-button:visited,
.avatar-button:hover,
.avatar-button:active {
  text-decoration: none;
}

.avatar-button {
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  background-color: var(--beige-light);
  color: var(--brown-dark);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0.25rem 1rem;
  font-weight: 600;
  gap: 0.5rem;
  min-height: 3.2rem;
  box-sizing: border-box;
}

.avatar-button > img {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 100%;
  background-color: white;
  border: 1px solid var(--brown-dark-40);
}

.avatar-button:hover {
  color: var(--beige-light);
  background-color: var(--brown-dark);
}

.extras {
  position: absolute;
  left: 2rem;
  top: 5.6rem;
  z-index: 200;
}

.hint-creature-button {
  width: 190px;
  height: 190px;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hint-creature-button img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
  filter: drop-shadow(0 8px 10px rgb(0 0 0 / 25%));
  transition: transform 0.2s ease;
}

.hint-creature-button:hover img {
  transform: scale(1.08);
}

.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 30%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.topbar-popup {
  width: min(700px, calc(100vw - 32px));
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 32px;
  box-shadow: 0 14px 30px rgb(0 0 0 / 25%);
  padding: 2rem;
  text-align: center;
}

.small-popup {
  max-width: 520px;
}

.topbar-popup h2 {
  margin: 0 0 1rem;
}

.topbar-popup p {
  margin: 0;
  line-height: 1.5;
  font-size: 1.15rem;
}

.popup-close-button {
  margin-top: 1.6rem;
  min-width: 160px;
  padding: 0.9rem 1.4rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: var(--green-confirm);
  color: var(--color-text);
  font-weight: 800;
  cursor: pointer;
}

.popup-close-button:hover {
  filter: brightness(1.08);
  transform: scale(1.03);
}

@media (max-width: 768px) {
  .nav-container {
    padding-left: 1rem;
    padding-right: 1rem;
  }

  .extras {
    left: 1rem;
    top: 5.4rem;
  }

  .hint-creature-button {
    width: 130px;
    height: 130px;
  }
}
</style>
