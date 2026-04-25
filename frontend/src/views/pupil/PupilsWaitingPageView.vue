<script setup lang="ts">
import {useRoute, useRouter} from 'vue-router'
import { onMounted, onUnmounted } from 'vue'
import { get } from '@/utils/apiUtils'

const router = useRouter()
const route = useRoute()

const classroomCode =
  typeof route.query.classroomCode === 'string'
    ? route.query.classroomCode
    : ''

let interval: number | null = null

async function checkStatus() {
  if (!classroomCode) return

  try {
    const data = await get<{
      membershipConfirmedByTeacher: boolean
    }>(`/api/pupil/classrooms/${classroomCode}`)

    if (data.membershipConfirmedByTeacher) {
      router.push({
        path: '/join-classroom/welcome-guide',
        query: { classroomCode },
      })
    }
  } catch (e) {
    console.error('Kunne ikke sjekke status', e)
  }
}

onMounted(() => {
  checkStatus()
  interval = window.setInterval(checkStatus, 3000)
})

onUnmounted(() => {
  if (interval) clearInterval(interval)
})

function goBack(): void {
  router.push('/join-classroom/enter-classroom-code');
}
</script>

<template>
  <div class="page">
    <div class="center-container">
      <div class="waiting-container">
        <div class="text-container">
          <h1>Venter på godkjenning</h1>
          <p>
            Du venter nå på at en lærer skal godkjenne forespørselen din om å
            bli med i klasserommet.
          </p>
          <p>
            I klasserommet skal dere kjempe om å få flere poeng enn andre klasserom, og
          kjempe mot dem om å være de beste nett-detektivene.
          </p>
          <p>
            Hvis det tar veldig lang tid å slippe inn i klasserommet må du kontakte læreren din.
          </p>
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
        <button class="cancel-button" @click="goBack">Kanseller forespørsel</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(1rem, 4vw, 3rem);
  overflow-y: auto;
  background-image: url('/src/assets/welcomeBackground.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.center-container {
  width: min(760px, 100%);
  padding: clamp(1.25rem, 4vw, 2rem);
  display: flex;
  flex-direction: column;
  text-align: center;
  background: var(--beige-light);
  border: clamp(4px, 1vw, 7px) solid var(--brown-dark);
  border-radius: clamp(24px, 5vw, 40px);
}

.text-container {
  padding: clamp(0.5rem, 3vw, 2rem);
  display: flex;
  flex-direction: column;
  text-align: center;
}

.text-container h1 {
  margin: 0 0 2rem;
  font-size: clamp(2rem, 6vw, 50px);
  line-height: 1.1;
}

.text-container p {
  margin: 0;
  font-size: clamp(1rem, 3vw, 25px);
  line-height: 1.6;
  max-width: 600px;
}

.button-container {
  margin-top: clamp(1.5rem, 5vw, 40px);
  display: flex;
  justify-content: center;
  width: 100%;
}

.cancel-button {
  width: min(420px, 100%);
  padding: 1rem 1.5rem;
  border-radius: 38px;
  border: clamp(3px, 0.8vw, 5px) solid var(--brown-dark);
  background-color: var(--red-danger);
  font-size: clamp(1rem, 3vw, 1.2rem);
  font-weight: 700;
  cursor: pointer;
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
