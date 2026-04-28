<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import TopBar from '@/components/pupil/TopBar.vue'
import blobbyImage from '@/assets/icons/blobby/creaturePeaking.png'
import boardBackground from '@/assets/backgrounds/korktavle.png'
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
  <div class="pupil-scene-screen">
    <TopBar title="Medaljer" previous-page-url="/pupil" />

    <main class="medals-page pupil-scene-page pupil-scene-page--dimmed">
      <section class="pupil-pinboard-stage" aria-labelledby="medals-title">
        <img class="pupil-pinboard-image" :src="boardBackground" alt="" aria-hidden="true" />
        <div class="pupil-board-blobby-wrapper" aria-hidden="true">
          <img class="pupil-board-blobby-image" :src="blobbyImage" alt="" />
        </div>

        <div class="board-content">
          <header class="medals-header">
            <img class="medals-icon" :src="medalsIcon" alt="" aria-hidden="true" />
            <div>
              <h1 id="medals-title">Medaljer</h1>
              <p v-if="!loading && !errorMessage && medals.length" class="medal-progress">
                {{ unlockedCount }} av {{ medals.length }} låst opp
              </p>
            </div>
          </header>

          <p v-if="loading" class="state-message pupil-state-message" role="status">
            Laster medaljer...
          </p>
          <p
            v-else-if="errorMessage"
            class="state-message pupil-state-message pupil-error"
            role="alert"
          >
            {{ errorMessage }}
          </p>
          <p v-else-if="medals.length === 0" class="state-message pupil-state-message">
            Ingen medaljer funnet enda.
          </p>

          <ul v-else class="medal-grid pupil-thin-scroll">
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
      </section>
    </main>
  </div>
</template>

<style scoped>
.medals-page {
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
}

.board-content {
  position: absolute;
  inset: 20% 16% 20% 16%;
  display: flex;
  flex-direction: column;
  gap: clamp(0.65rem, 1.5vw, 1.25rem);
  min-height: 0;
}

.medals-header {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: clamp(0.75rem, 2vw, 1.5rem);
  min-height: clamp(3.5rem, 8vw, 7rem);
}

.medals-icon {
  width: clamp(3.8rem, 8vw, 7rem);
  height: clamp(3.8rem, 8vw, 7rem);
  object-fit: contain;
  object-position: center;
}

h1 {
  margin: 0;
  font-size: clamp(2rem, 4.8vw, 4.2rem);
  line-height: 1;
}

.medal-progress {
  margin: 0.3rem 0 0;
  color: var(--brown-dark);
  font-weight: 700;
  line-height: 1.2;
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
  flex: 1 1 auto;
  min-height: 0;
  margin: 0 auto;
  width: min(100%, 46rem);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(clamp(10rem, 16vw, 12rem), 1fr));
  align-content: start;
  gap: clamp(0.55rem, 1.25vw, 0.9rem);
  list-style: none;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 0.35rem 0.45rem 0;
}

.medal-slot {
  min-width: 0;
}

.medal-card {
  position: relative;
  min-height: clamp(8.75rem, 13.5vw, 10.25rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 0.25rem;
  overflow: hidden;
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1.1rem;
  background: var(--beige-light-80);
  box-shadow: 0 0.35rem 0 var(--brown-dark-20);
  padding: clamp(0.55rem, 1.2vw, 0.85rem);
}

.medal-slot--locked .medal-card {
  background: var(--beige-medium-60);
}

.medal-art {
  align-items: center;
  display: flex;
  justify-content: center;
  width: clamp(4.6rem, 9.2vw, 7.25rem);
  height: clamp(4.1rem, 8.2vw, 6.4rem);
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
  background: var(--beige-light-80);
  color: var(--brown-dark);
  font-size: 2rem;
  font-weight: 800;
}

.medal-copy {
  width: 100%;
  text-align: center;
  color: var(--brown-dark);
}

.medal-copy h2 {
  margin: 0;
  font-size: clamp(0.88rem, 1.3vw, 1.08rem);
  line-height: 1.1;
}

.medal-copy p {
  margin: 0.2rem 0 0;
  color: var(--brown-dark-80);
  font-size: clamp(0.62rem, 0.78vw, 0.74rem);
  font-weight: 600;
  line-height: 1.15;
}

.medal-status {
  position: absolute;
  top: 0.45rem;
  right: 0.45rem;
  border: 2px solid var(--brown-dark);
  border-radius: 999px;
  background: var(--beige-light-80);
  color: var(--brown-dark);
  font-size: clamp(0.62rem, 0.85vw, 0.72rem);
  font-weight: 800;
  line-height: 1;
  padding: 0.25rem 0.45rem;
}

@media (max-width: 900px) {
  .board-content {
    inset: 19% 16% 20% 16%;
  }

  .medal-grid {
    width: min(100%, 30rem);
  }
}

@media (max-width: 560px) {
  .board-content {
    inset: 8.75rem 3.25rem 6.5rem 3.25rem;
  }

  .medals-header {
    justify-content: flex-start;
  }

  .medal-grid {
    grid-template-columns: 1fr;
    width: 100%;
  }
}
</style>
