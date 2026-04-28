<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import TopBar from '@/components/pupil/TopBar.vue'
import { get } from '@/utils/apiUtils'
import boardBackground from '@/assets/backgrounds/korktavle.png'
import leaderboardBg from '@/assets/backgrounds/hjemskjerm-bg.png'

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
  <TopBar title="Ledertavle" />

  <div class="page" :style="{ backgroundImage: `url(${leaderboardBg})` }">
    <div class="board-card">
      <img class="board-image" :src="boardBackground" alt="Leaderboard board" />

      <div class="board-content">
        <h1>Ledertavle</h1>

        <p v-if="loading" class="state-message">Laster ledertavle...</p>
        <p v-else-if="error" class="state-message error">{{ error }}</p>

        <template v-else>
          <div v-if="leaderboard.length" class="list-wrap">
            <ol>
              <li v-for="(entry, index) in paginatedLeaderboard" :key="entry.classroomCode">
                <span class="rank"> {{ (currentPage - 1) * itemsPerPage + index + 1 }}. </span>

                <span class="classroom-name">
                  {{ entry.classroomName }}
                </span>

                <span class="xp-value"> {{ entry.totalXp }} XP </span>
              </li>
            </ol>
          </div>

          <p v-else class="state-message">Ingen klasserom funnet...</p>
        </template>
      </div>
    </div>

    <div v-if="!loading && !error && leaderboard.length > itemsPerPage" class="pagination">
      <button :disabled="currentPage === 1" @click="previousPage">Forrige</button>

      <span>Side {{ currentPage }} av {{ totalPages }}</span>

      <button :disabled="currentPage === totalPages" @click="nextPage">Neste</button>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 4rem);
  align-items: center;
  justify-content: center;
  padding: 1.5rem;

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.board-card {
  position: relative;
  width: min(92vw, 70rem);
  margin: auto 0;
}

.board-image {
  display: block;
  width: 100%;
  height: auto;
}

.board-content {
  position: absolute;
  inset: 8% 8% 8% 8%;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  overflow: hidden;

  padding-top: 12%;
  padding-left: 9%;
  padding-right: 9%;
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
  font-family: 'Inter', sans-serif;
}

.state-message.error {
  color: #9f1d1d;
}

.list-wrap {
  overflow: hidden;
}

ol {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

li {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 0.75rem;

  padding: 0.65rem 0.9rem;
  background: var(--beige-light);
  border-radius: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
  font-family: 'Inter', sans-serif;
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

  font-family: 'Inter', sans-serif;
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
  .board-card {
    width: 98vw;
  }

  .board-content {
    inset: 9% 7% 10% 7%;
    padding-top: 16%;
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
