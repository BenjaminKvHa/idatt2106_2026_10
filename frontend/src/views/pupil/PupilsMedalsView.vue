<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import PinboardLayout from '@/components/pupil/PinboardLayout.vue'
import StateDisplay from '@/components/ui/StateDisplay.vue'
import TopBar from '@/components/pupil/TopBar.vue'
import medalsIcon from '@/assets/icons/homescreen/icon_medals.png'
import { get } from '@/utils/apiUtils'

type PupilMedal = {
  medalKey: string
  name: string
  description: string
  unlocked: boolean
  imageFileObjectId: number | null
}

const medals = ref<PupilMedal[]>([])
const loading = ref(true)
const errorMessage = ref('')

const unlockedCount = computed(() => medals.value.filter((medal) => medal.unlocked).length)

onMounted(async () => {
  try {
    medals.value = await get<PupilMedal[]>('/api/pupil/medals')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke laste medaljer'
  } finally {
    loading.value = false
  }
})

function medalImageUrl(medal: PupilMedal) {
  return medal.imageFileObjectId === null ? '' : `/api/objects/${medal.imageFileObjectId}`
}
</script>

<template>
  <div class="medals-page-bg pupil-scene-screen">
    <TopBar title="Medaljer" previous-page-url="/pupil" />

    <PinboardLayout heading-id="medals-title">
      <div class="board-content pupil-thin-scroll">
        <header class="medals-header">
          <img class="medals-icon" :src="medalsIcon" alt="" aria-hidden="true" />
          <h1 id="medals-title" class="sr-only">Medaljer</h1>
          <p v-if="!loading && !errorMessage && medals.length" class="medal-progress">
            {{ unlockedCount }} av {{ medals.length }} låst opp
          </p>
        </header>

        <StateDisplay
          :loading="loading"
          :error="errorMessage"
          :empty="medals.length === 0"
          loading-text="Laster medaljer..."
          empty-text="Ingen medaljer funnet enda."
        />

        <ul v-if="!loading && !errorMessage && medals.length" class="medal-grid">
          <li
            v-for="medal in medals"
            :key="medal.medalKey"
            class="medal-slot"
            :class="{
              'medal-slot--locked': !medal.unlocked,
            }"
          >
            <article class="medal-card">
              <span class="medal-status">{{ medal.unlocked ? 'Låst opp' : 'Låst' }}</span>
              <div class="medal-art">
                <img
                  v-if="medal.imageFileObjectId !== null"
                  :src="medalImageUrl(medal)"
                  :alt="medal.name"
                  class="medal-image"
                />
                <span v-else class="missing-image">?</span>
              </div>

              <div class="medal-copy">
                <h2>{{ medal.name }}</h2>
                <p>{{ medal.description }}</p>
              </div>
            </article>
          </li>
        </ul>
      </div>
    </PinboardLayout>
  </div>
</template>

<style scoped>
.medals-page-bg {
  min-height: 100vh;
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.medals-page {
  background: transparent;
}

.board-content {
  position: absolute;
  inset: 16.3% 11.3% 13.2% 12%;
  display: flex;
  flex-direction: column;
  gap: clamp(0.1rem, 0.45vw, 0.35rem);
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 0.45rem 1rem 0;
  box-sizing: border-box;
}

.medals-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: clamp(0.15rem, 0.55vw, 0.45rem);
  min-height: clamp(5.5rem, 10.5vw, 8.8rem);
}

.medals-icon {
  width: clamp(8.5rem, 15vw, 13rem);
  height: clamp(8.5rem, 15vw, 13rem);
  object-fit: contain;
  object-position: center;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.medal-progress {
  min-width: clamp(15rem, 24vw, 20rem);
  min-height: clamp(3.6rem, 5.2vw, 4.8rem);
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: clamp(0.55rem, 1vw, 0.8rem) clamp(2.6rem, 4vw, 3.6rem);
  background-image: url('@/assets/woodenLabel.png');
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  color: #fff7df;
  font-family: 'Inika', serif;
  font-size: clamp(1.2rem, 2.2vw, 1.75rem);
  font-weight: 900;
  line-height: 1;
  text-align: center;
  text-shadow:
    0 0.08rem 0 #4b2511,
    0 0.16rem 0.1rem rgba(34, 13, 3, 0.8);
  filter: drop-shadow(0 0.18rem 0.12rem rgba(43, 24, 12, 0.34));
}

.state-message {
  width: min(32rem, 100%);
  margin: auto;
  padding: 1rem;
  align-items: center;
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 0.75rem;
  background: var(--beige-light-80);
  color: var(--brown-dark);
  font-weight: 700;
  text-align: center;
}

.medal-grid {
  --medal-card-size: clamp(12.5rem, 18.5vw, 16rem);

  flex: 0 0 auto;
  margin: 0 auto;
  width: min(100%, 68rem);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, var(--medal-card-size)), 1fr));
  align-content: start;
  gap: clamp(0.65rem, 1.35vw, 1rem);
  list-style: none;
  overflow: visible;
  padding: 0 0.35rem 0.45rem;
  margin-top: clamp(-0.45rem, -0.8vw, -0.2rem);
}

.medal-slot {
  display: flex;
  justify-content: center;
  min-width: 0;
}

.medal-card {
  position: relative;
  width: min(100%, var(--medal-card-size));
  aspect-ratio: 1 / 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: clamp(0.1rem, 0.35vw, 0.25rem);
  overflow: hidden;
  border: 0;
  background-image: url('@/assets/medalBoard.png');
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  box-shadow: none;
  box-sizing: border-box;
  padding: 15% 14% 13%;
}

.medal-slot--locked .medal-card {
  filter: saturate(0.9);
}

.medal-art {
  align-items: center;
  display: flex;
  justify-content: center;
  width: clamp(5.25rem, 9.3vw, 7.55rem);
  height: clamp(4.65rem, 8.45vw, 6.75rem);
  flex: 0 0 auto;
}

.medal-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 0.2rem 0 var(--brown-dark-20));
}

.missing-image {
  align-items: center;
  justify-content: center;
  display: flex;
  width: 100%;
  height: 100%;
  border-radius: 1rem;
  background: rgba(255, 244, 220, 0.65);
  color: var(--brown-dark);
  font-size: 2rem;
  font-weight: 800;
}

.medal-copy {
  width: 100%;
  min-height: 0;
  text-align: center;
  color: #fff7df;
  text-shadow:
    0 0.07rem 0 #4b2511,
    0 0.14rem 0.1rem rgba(34, 13, 3, 0.8);
}

.medal-copy h2 {
  margin: 0;
  font-size: clamp(0.92rem, 1.25vw, 1.12rem);
  line-height: 1.1;
}

.medal-copy p {
  margin: 0.2rem 0 0;
  color: #fff7df;
  font-size: clamp(0.58rem, 0.72vw, 0.7rem);
  font-weight: 600;
  line-height: 1.1;
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.medal-status {
  position: absolute;
  top: 16.5%;
  right: 13.5%;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #fff7df;
  font-size: clamp(0.62rem, 0.86vw, 0.76rem);
  font-weight: 900;
  line-height: 1;
  padding: 0;
  text-shadow:
    0 0.07rem 0 #4b2511,
    0 0.14rem 0.1rem rgba(34, 13, 3, 0.85);
}

@media (max-width: 900px) {
  .medals-header {
    min-height: clamp(4.75rem, 13vw, 6.75rem);
  }

  .medals-icon {
    width: clamp(4.75rem, 13vw, 6.75rem);
    height: clamp(4.75rem, 13vw, 6.75rem);
  }

  .medal-grid {
    width: min(100%, 36rem);
  }
}

@media (max-width: 560px) {
  .board-content {
    inset: 16.3% 7% 13.2% 9%;
    padding: 0 0.25rem 0.35rem 0;
  }

  .medals-header {
    flex-direction: column;
    justify-content: center;
    gap: 0;
    min-height: 3.4rem;
  }

  .medals-icon {
    display: none;
  }

  .medal-progress {
    min-width: min(100%, 14rem);
    min-height: 3.4rem;
    font-size: 1.1rem;
    padding: 0.55rem 2.4rem;
  }

  .medal-grid {
    --medal-card-size: clamp(10.25rem, 40vw, 12rem);

    grid-template-columns: minmax(0, 1fr);
    width: min(100%, 13rem);
    padding-inline: 0.15rem;
  }

  .medal-art {
    width: clamp(4.2rem, 18vw, 5.5rem);
    height: clamp(3.75rem, 16vw, 4.9rem);
  }

  .medal-copy h2 {
    font-size: clamp(0.82rem, 3.9vw, 0.96rem);
  }

  .medal-copy p {
    font-size: clamp(0.52rem, 2.7vw, 0.62rem);
  }

  .medal-status {
    font-size: clamp(0.56rem, 2.8vw, 0.66rem);
  }
}
</style>
