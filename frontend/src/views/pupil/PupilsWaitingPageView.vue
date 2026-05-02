<script setup lang="ts">
import { useRouter } from 'vue-router'
import { onMounted, onUnmounted, ref } from 'vue'
import { clearMeCache, fetchMeCached } from '@/utils/authRedirect'
import bgImg from '@/assets/backgrounds/hjemskjerm-bg.png'
import { del } from '@/utils/apiUtils'


const router = useRouter()
const classroomCode = ref<string | null>(null)
const canceling = ref(false)
const cancelError = ref('')

let interval: number | null = null

async function checkStatus() {
  const profile = await fetchMeCached({ force: true })
  classroomCode.value = profile?.classroomCode ?? null

  if (!profile?.classroomCode) {
    clearMeCache()
    await router.push('/join-classroom/enter-classroom-code')
    return
  }

  if (profile.membershipConfirmed) {
    clearMeCache()
    await router.push('/pupil')
  }
}

function stopPolling(): void {
  if (interval) {
    clearInterval(interval)
    interval = null
  }
}

onMounted(() => {
  checkStatus()
  interval = window.setInterval(checkStatus, 3000)
})

onUnmounted(() => {
  stopPolling()
})

async function cancelJoinRequest(): Promise<void> {
  if (canceling.value) return

  if (!classroomCode.value) {
    clearMeCache()
    await router.push('/join-classroom/enter-classroom-code')
    return
  }

  canceling.value = true
  cancelError.value = ''
  stopPolling()

  try {
    await del(`/api/classrooms/${classroomCode.value}/pupils/me`)
    clearMeCache()
    await router.push('/join-classroom/enter-classroom-code')
  } catch {
    cancelError.value = 'Kunne ikke kansellere forespørselen. Prøv igjen.'
    interval = window.setInterval(checkStatus, 3000)
  } finally {
    canceling.value = false
  }
}
</script>

<template>
  <div class="page" :style="{ '--page-bg': `url(${bgImg})` }">
    <div class="center-container">
      <div class="waiting-container">
        <div class="text-container">
          <h1>Venter på godkjenning</h1>
          <div class="divider"></div>
          <div class="text">
            <p>Du venter nå på at en lærer skal godkjenne forespørselen din om å bli med i klasserommet.</p>
            <p v-if="classroomCode" class="classroom-code">
              Klasseromskode: <span>{{ classroomCode }}</span>
            </p>
            <p> I klasserommet skal dere kjempe om å få flere poeng enn andre klasserom, og kjempe mot dem om å være de beste nettdetektivene.</p>
            <p>Hvis det tar veldig lang tid å slippe inn i klasserommet må du kontakte læreren din.</p>
          </div>
        </div>
        <div class="loading-container">
          <p>Venter på godkjenning...</p>
          <div class="loading-bar">
            <div
              v-for="i in 6"
              :key="i"
              class="loading-block"
              :style="{ animationDelay: `${i * 0.2}s` }"
            ></div>
          </div>
        </div>
      </div>
      <div class="button-container">
        <button class="cancel-button" :disabled="canceling" @click="cancelJoinRequest">
          {{ canceling ? 'Kansellerer...' : 'Kanseller forespørsel' }}
        </button>
        <p v-if="cancelError" class="cancel-error" role="alert">{{ cancelError }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(1rem, 4vw, 3rem);
  overflow: hidden;
}

.page::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image: var(--page-bg);
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: blur(12px);
  transform: scale(1.08);
  z-index: 0;
  pointer-events: none;
}

.page::after {
  content: '';
  position: fixed;
  inset: 0;
  background: rgba(40, 25, 10, 0.18);
  z-index: 1;
  pointer-events: none;
}

.page > * {
  position: relative;
  z-index: 2;
}

.center-container {
  position: relative;
  width: min(540px, 100%);
  padding: clamp(2rem, 5vw, 3rem) clamp(1.5rem, 5vw, 2.5rem) clamp(1.75rem, 5vw, 2.5rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(1rem, 3vw, 1.4rem);
  text-align: center;
  background: var(--beige-light-100);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: clamp(28px, 6vw, 40px);

  box-shadow:
    0 0 0 2px rgba(0, 0, 0, 0.25),
    0 6px 14px rgba(0, 0, 0, 0.2),
    0 12px 30px rgba(0, 0, 0, 0.15),
    inset 0 2px 4px rgba(0, 0, 0, 0.25);
  z-index: 2;
}

.text-container {
  padding: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(0.9rem, 3vw, 1.4rem);
  text-align: center;
}

.text-container h1 {
  margin: 0;
  font-size: clamp(1.9rem, 6vw, 2.2rem);
  line-height: 1.2;
}

.text-container text {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.divider {
  width: 60%;
  height: 3px;
  background: var(--brown-medium);
  border-radius: 5px;
  opacity: 0.35;
}

.text-container p {
  margin: 0;
  font-size: clamp(1.2rem, 3vw, 1rem);
  line-height: 1.6;
  max-width: 100%;
}

.classroom-code {
  font-weight: 700;
}

.classroom-code span {
  letter-spacing: 0.08em;
}

.button-container {
  margin-top: 0.4rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  justify-content: center;
  width: 100%;
}

.cancel-button {
  width: min(420px, 100%);
  padding: 0.9rem 2rem;
  background: var(--red-danger);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  font-family: 'Inika', serif;
  font-size: clamp(1rem, 4vw, 1.35rem);
  font-weight: 700;
  cursor: pointer;
  box-shadow: -3px 3px 6px var(--brown-dark);
  transition:
    transform 0.1s,
    box-shadow 0.1s;
}

.cancel-button:hover {
  transform: translate(-2px, -2px);
  box-shadow: -5px 5px 0 var(--brown-dark);
}

.cancel-button:disabled {
  cursor: wait;
  opacity: 0.7;
  transform: none;
  box-shadow: -3px 3px 6px var(--brown-dark);
}

.cancel-button:active {
  transform: translate(3px, 3px);
  box-shadow: -1px 1px 0 var(--brown-dark);
}

.cancel-error {
  margin: 0;
  color: var(--red-danger);
  font-size: clamp(0.95rem, 3vw, 1.1rem);
  font-weight: 700;
}

.loading-container {
  margin-top: clamp(1.25rem, 4vw, 1rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.8rem;
}

.loading-container p {
  font-size: clamp(0.9rem, 3vw, 20px);
  color: var(--brown-dark);
  letter-spacing: clamp(1px, 0.4vw, 2px);
}

.loading-bar {
  display: flex;
  gap: clamp(3px, 1vw, 6px);
  padding: clamp(4px, 1vw, 6px);
  border: clamp(3px, 0.8vw, 4px) solid var(--brown-dark);
  border-radius: 8px;
  background: var(--beige-light);
}

.loading-block {
  width: clamp(12px, 3vw, 18px);
  height: clamp(12px, 3vw, 18px);
  background: var(--brown-dark);
  animation: block-loading 1.5s infinite;
}

@keyframes block-loading {
  0% {
    opacity: 0.2;
    transform: scale(0.9);
  }

  50% {
    opacity: 1;
    transform: scale(1.1);
  }

  100% {
    opacity: 0.2;
    transform: scale(0.9);
  }
}
</style>
