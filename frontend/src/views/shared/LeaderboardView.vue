<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PinboardLayout from '@/components/pupil/PinboardLayout.vue'
import StateDisplay from '@/components/ui/StateDisplay.vue'
import TopBar from '@/components/pupil/TopBar.vue'
import { get } from '@/utils/apiUtils'

type ClassroomLeaderboardEntry = {
  classroomCode: string
  classroomName: string
  totalXp: number
}

const endpoint = '/api/leaderboards/classrooms'

const leaderboard = ref<ClassroomLeaderboardEntry[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const currentPage = ref(1)
const itemsPerPage = 4

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(leaderboard.value.length / itemsPerPage))
})

const paginatedLeaderboard = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return leaderboard.value.slice(start, start + itemsPerPage)
})

function nextPage() {
  if (currentPage.value < totalPages.value) currentPage.value++
}

function previousPage() {
  if (currentPage.value > 1) currentPage.value--
}

async function loadLeaderboard() {
  loading.value = true
  error.value = null

  try {
    const fetchedLeaderboard = await get<ClassroomLeaderboardEntry[]>(endpoint)

    if (!Array.isArray(fetchedLeaderboard)) {
      throw new Error('Unexpected leaderboard response format')
    }

    leaderboard.value = [...fetchedLeaderboard].sort((a, b) => b.totalXp - a.totalXp)
    currentPage.value = 1
  } catch (fetchError) {
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load leaderboard'
  } finally {
    loading.value = false
  }
}

onMounted(loadLeaderboard)
</script>

<template>
  <div class="pupil-scene-screen">
    <TopBar title="Ledertavle" />

    <main class="leaderboard-page pupil-scene-page">
      <section class="pupil-pinboard-stage" aria-labelledby="leaderboard-title">
        <img class="pupil-pinboard-image" :src="boardBackground" alt="" aria-hidden="true" />
        <div class="pupil-board-blobby-wrapper" aria-hidden="true">
          <img class="pupil-board-blobby-image" :src="blobbyImage" alt="" />
        </div>

        <p v-else class="state-message">Ingen klasserom funnet...</p>
      </template>
    </div>

    <div v-if="!loading && !error && leaderboard.length > itemsPerPage" class="pagination">
      <button :disabled="currentPage === 1" @click="previousPage">Forrige</button>

      <span>Side {{ currentPage }} av {{ totalPages }}</span>

      <button :disabled="currentPage === totalPages" @click="nextPage">Neste</button>
    </div>
  </PinboardLayout>
</template>

<style scoped>

.board-content {
  position: absolute;
  inset: 13% 10% 18% 10%;
  display: flex;
  flex-direction: column;
  gap: clamp(0.45rem, 1.1vw, 0.7rem);
  overflow: hidden;
  padding: 5% 10% 0;
}

h1 {
  margin: 0;
  text-align: center;
  font-family: 'Inika', serif;
  font-size: clamp(2rem, 4vw, 3.5rem);
  color: var(--brown-dark);
}

.state-message {
  margin: 0;
  text-align: center;
}

.state-message.error {
  color: var(--red-danger);
}

.list-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 0.2rem 0.25rem;
}

ol {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: clamp(0.35rem, 0.8vw, 0.6rem);
}

li {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.75rem;

  min-height: clamp(2.6rem, 5.2vw, 3.55rem);
  padding: clamp(0.38rem, 0.8vw, 0.6rem) clamp(0.65rem, 1.2vw, 0.9rem);
  background: var(--beige-light);
  border-radius: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
}

.rank {
  font-weight: 800;
  color: var(--brown-dark);
  min-width: 2rem;
}

.classroom-name {
  font-weight: 600;
}

.xp-value {
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.pagination {
  position: fixed;
  bottom: 1.5rem;
  left: 50%;
  transform: translateX(-50%);

  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;

  z-index: 10;
}

.pagination button {
  border: var(--border-thin) solid var(--brown-dark);
  background: var(--beige-light);
  color: var(--brown-dark);
  border-radius: 999px;
  padding: 0.45rem 1.2rem;
  font-weight: 700;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pagination span {
  font-weight: 800;
  color: var(--brown-dark);
  background: rgba(255, 244, 220, 0.85);
  padding: 0.35rem 0.75rem;
  border-radius: 999px;
}

@media (max-width: 640px) {
  .board-content {
    inset: 13% 9% 19% 9%;
    padding: 5% 8% 0;
  }

  li {
    padding: 0.45rem 0.6rem;
    gap: 0.5rem;
  }

  .pagination {
    bottom: 0.75rem;
    gap: 0.5rem;
    font-size: 0.85rem;
  }

  .pagination button {
    padding: 0.35rem 0.7rem;
  }

  .pagination span {
    padding: 0.3rem 0.55rem;
  }
}
</style>
