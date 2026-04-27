<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TopBar from '@/components/pupil/TopBar.vue'
import { get } from '@/utils/apiUtils'
import boardBackground from '@/assets/backgrounds/korktavle.png'

type ClassroomLeaderboardEntry = {
  classroomCode: string
  classroomName: string
  totalXp: number
}

const endpoint = '/api/leaderboards/classrooms'

const leaderboard = ref<ClassroomLeaderboardEntry[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

async function loadLeaderboard() {
  loading.value = true
  error.value = null

  try {
    const fetchedLeaderboard = await get<ClassroomLeaderboardEntry[]>(endpoint)
    if (!Array.isArray(fetchedLeaderboard)) {
      throw new Error('Unexpected leaderboard response format')
    }

    leaderboard.value = [...fetchedLeaderboard].sort((a, b) => b.totalXp - a.totalXp)
  } catch (fetchError) {
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load leaderboard'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadLeaderboard()
})
</script>

<template>
  <TopBar />
  <div class="page">
    <div class="board-card">
      <img class="board-image" :src="boardBackground" alt="Leaderboard board" />

      <div class="board-content">
        <h1>Leaderboard</h1>

        <p v-if="loading" class="state-message">Loading leaderboard...</p>
        <p v-else-if="error" class="state-message error">{{ error }}</p>

        <div v-else class="list-wrap">
          <ol>
            <li v-for="entry in leaderboard" :key="entry.classroomCode">
              <span class="classroom-name">{{ entry.classroomName }}</span>
              <span class="xp-value">{{ entry.totalXp }} XP</span>
            </li>
          </ol>
        </div>
        <p v-if="!leaderboard.length">
          Ingen klasserom funnet...
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  min-height: calc(100vh - 4rem);
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.board-card {
  position: relative;
  width: max(64vw, 52rem);
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
  padding-top: 15%;
  padding-left: 10%;
  padding-right: 10%;
}

h1 {
  margin: 0;
  text-align: center;
}

.state-message {
  margin: 0;
  text-align: center;
}

.state-message.error {
  color: #9f1d1d;
}

.list-wrap {
  overflow-y: auto;
  padding-right: 0.25rem;
}

ol {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.55rem 0.75rem;
  background: var(--beige-light);
  border-radius: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
}

.classroom-name {
  font-weight: 600;
}

.xp-value {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .board-content {
    inset: 9% 7% 10% 7%;
  }

  li {
    padding: 0.45rem 0.6rem;
  }
}
</style>
