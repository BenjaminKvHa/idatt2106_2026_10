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

type MeProfile = {
  classroomCode: string | null
}

type RankedClassroomLeaderboardEntry = ClassroomLeaderboardEntry & {
  rank: number
}

const endpoint = '/api/leaderboards/classrooms'

const leaderboard = ref<RankedClassroomLeaderboardEntry[]>([])
const currentClassroomCode = ref<string | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

const currentPage = ref(1)
const itemsPerPage = 5

const totalPages = computed(() => {
  return Math.max(1, Math.ceil(leaderboard.value.length / itemsPerPage))
})

const paginatedLeaderboard = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  return leaderboard.value.slice(start, start + itemsPerPage)
})

const visibleRangeLabel = computed(() => {
  if (!leaderboard.value.length) return ''

  const start = (currentPage.value - 1) * itemsPerPage + 1
  const end = Math.min(currentPage.value * itemsPerPage, leaderboard.value.length)
  return `${start}-${end}`
})

const currentClassroom = computed(() => {
  if (!currentClassroomCode.value) return null
  return leaderboard.value.find((entry) => entry.classroomCode === currentClassroomCode.value) ?? null
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
    const [fetchedLeaderboard, meProfile] = await Promise.all([
      get<ClassroomLeaderboardEntry[]>(endpoint),
      get<MeProfile>('/api/me').catch(() => null),
    ])

    if (!Array.isArray(fetchedLeaderboard)) {
      throw new Error('Unexpected leaderboard response format')
    }

    currentClassroomCode.value = meProfile?.classroomCode ?? null
    leaderboard.value = [...fetchedLeaderboard]
      .sort((a, b) => b.totalXp - a.totalXp)
      .map((entry, index) => ({ ...entry, rank: index + 1 }))
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
  <div class="leaderboard-page-bg pupil-scene-screen">
    <TopBar title="Ledertavle" />

    <PinboardLayout heading-id="leaderboard-title">
      <div class="board-content">
        <h1 id="leaderboard-title" class="leaderboard-title-label">
          <span class="leaderboard-title-text">Ledertavle</span>
          <span v-if="visibleRangeLabel" class="leaderboard-range">{{ visibleRangeLabel }}</span>
        </h1>

        <StateDisplay
          :loading="loading"
          :error="error"
          :empty="leaderboard.length === 0"
          loading-text="Laster ledertavle..."
          empty-text="Ingen klasserom funnet..."
        />

        <div v-if="!loading && !error && leaderboard.length" class="list-wrap pupil-thin-scroll">
          <ol>
            <li
              v-for="entry in paginatedLeaderboard"
              :key="entry.classroomCode"
              :class="{ 'current-page-classroom': entry.classroomCode === currentClassroomCode }"
            >
              <span class="rank"> {{ entry.rank }}. </span>

              <span class="classroom-name">
                {{ entry.classroomName }}
              </span>

              <span class="xp-value"> {{ entry.totalXp }} XP </span>
            </li>
          </ol>
        </div>

        <div v-if="currentClassroom" class="current-classroom-summary" aria-label="Din klasse på ledertavlen">
          <span class="current-label">Din kl.</span>
          <span class="rank">{{ currentClassroom.rank }}.</span>
          <span class="classroom-name">{{ currentClassroom.classroomName }}</span>
          <span class="xp-value">{{ currentClassroom.totalXp }} XP</span>
        </div>
      </div>

      <div v-if="!loading && !error && leaderboard.length > itemsPerPage" class="pagination">
        <button :disabled="currentPage === 1" @click="previousPage">Forrige</button>

        <span class="page-large">Side {{ currentPage }} av {{ totalPages }}</span>
        <span class="page-small">{{ currentPage }}/{{ totalPages }}</span>

        <button :disabled="currentPage === totalPages" @click="nextPage">Neste</button>
      </div>
    </PinboardLayout>
  </div>
</template>

<style scoped>
.page-small {
  display: none;
}
.page-large {
  display: block;
}
.leaderboard-page-bg {
  min-height: 100vh;
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
}

.leaderboard-page {
  background: transparent;
}

.board-content {
  --leaderboard-title-label-height: clamp(7.3rem, 10vw, 8.5rem);
  --leaderboard-entry-label-height: clamp(5.1rem, 6.35vw, 6rem);
  --leaderboard-current-label-height: clamp(5.1rem, 6.35vw, 6rem);
  --leaderboard-title-to-list-overlap: clamp(-2.1rem, -2.3vw, -1.45rem);
  --leaderboard-entry-overlap: clamp(-2rem, -2.25vw, -1.35rem);
  --leaderboard-current-overlap: clamp(-1.75rem, -2vw, -1.2rem);

  position: absolute;
  inset: 13% 10% 18% 10%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  overflow: hidden;
  padding: 4% 8% 0;
}

h1 {
  margin: 0;
}

.leaderboard-title-label {
  width: min(760px, 74%);
  min-height: var(--leaderboard-title-label-height);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: clamp(0.7rem, 1.2vw, 1rem);
  padding: clamp(1.2rem, 1.85vw, 1.55rem) clamp(4rem, 6.4vw, 5.8rem);
  background-image: url('@/assets/woodenLabel.png');
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  filter: drop-shadow(0 0.25rem 0.15rem rgba(43, 24, 12, 0.35));
}

.leaderboard-title-text,
.leaderboard-range {
  text-align: center;
  font-family: 'Inika', serif;
  font-size: clamp(2.1rem, 4vw, 3.7rem);
  color: #fff7df;
  line-height: 1;
  text-shadow:
    0 0.12rem 0 #4b2511,
    0 0.22rem 0.12rem rgba(34, 13, 3, 0.75);
}

.leaderboard-range {
  font-size: clamp(1.15rem, 2.1vw, 1.75rem);
  transform: translateY(0.18rem);
}

.state-message {
  margin: 0;
  text-align: center;
}

.state-message.error {
  color: var(--red-danger);
}

.list-wrap {
  flex: 0 1 auto;
  width: min(100%, 1050px);
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 0 0.2rem 0.05rem;
  margin-top: var(--leaderboard-title-to-list-overlap);
}

ol {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0;
}

ol li + li {
  margin-top: var(--leaderboard-entry-overlap);
}

li {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: clamp(0.65rem, 1.2vw, 1rem);

  min-height: var(--leaderboard-entry-label-height);
  padding: clamp(1rem, 1.45vw, 1.25rem) clamp(3.6rem, 5.7vw, 5.4rem);
  background-image: url('@/assets/woodenLabel.png');
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  border: 0;
  color: #fff7df;
  font-size: clamp(1.05rem, 1.6vw, 1.45rem);
  text-shadow:
    0 0.08rem 0 #4b2511,
    0 0.16rem 0.1rem rgba(34, 13, 3, 0.8);
  filter: drop-shadow(0 0.18rem 0.12rem rgba(43, 24, 12, 0.34));
}

.current-page-classroom {
  filter:
    drop-shadow(0 0.18rem 0.12rem rgba(43, 24, 12, 0.34))
    drop-shadow(0 0 0.18rem rgba(255, 247, 176, 0.95))
    drop-shadow(0 0 0.75rem rgba(255, 216, 55, 0.9))
    drop-shadow(0 0 1.15rem rgba(255, 178, 20, 0.55));
}

.rank {
  font-weight: 800;
  color: #fff7df;
  min-width: 2rem;
}

.classroom-name {
  font-weight: 800;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.xp-value {
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.current-classroom-summary {
  width: min(100%, 1050px);
  display: grid;
  grid-template-columns: auto auto minmax(0, 1fr) auto;
  align-items: center;
  gap: clamp(0.45rem, 0.8vw, 0.7rem);
  position: relative;
  isolation: isolate;
  margin-top: var(--leaderboard-current-overlap);
  min-height: var(--leaderboard-current-label-height);
  padding: clamp(1rem, 1.45vw, 1.25rem) clamp(3.6rem, 5.7vw, 5.4rem);
  background-image: url('@/assets/woodenLabel.png');
  background-repeat: no-repeat;
  background-position: center;
  background-size: 100% 100%;
  color: #fff7df;
  font-size: clamp(1.05rem, 1.55vw, 1.35rem);
  text-shadow:
    0 0.08rem 0 #4b2511,
    0 0.16rem 0.1rem rgba(34, 13, 3, 0.8);
  filter:
    drop-shadow(0 0.18rem 0.12rem rgba(43, 24, 12, 0.34))
    drop-shadow(0 0 0.3rem rgba(255, 243, 190, 0.65));
}

.current-classroom-summary::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -1;
  background: rgba(35, 13, 3, 0.34);
  mask-image: url('@/assets/woodenLabel.png');
  mask-repeat: no-repeat;
  mask-position: center;
  mask-size: 100% 100%;
  -webkit-mask-image: url('@/assets/woodenLabel.png');
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-position: center;
  -webkit-mask-size: 100% 100%;
}

.current-label {
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  white-space: nowrap;
  font-size: 0.92em;
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

@media (max-width: 768px) {
  .board-content {
    --leaderboard-title-label-height: 4.7rem;
    --leaderboard-entry-label-height: 4.45rem;
    --leaderboard-current-label-height: 4.45rem;
    --leaderboard-title-to-list-overlap: -1rem;
    --leaderboard-entry-overlap: -0.95rem;
    --leaderboard-current-overlap: -0.75rem;
    
    padding: 0% 0% 0;
    inset: 6% 4% 6% !important;
  }
  .page-small {
    display: block;
  }
  .page-large {
    display: none;
  }

  .leaderboard-title-label {
    width: min(100%, 22rem);
    padding: 0.9rem 3rem;
  }

  .leaderboard-title-label span {
    font-size: clamp(1.75rem, 8.5vw, 2.25rem);
  }

  li {
    padding: 0.85rem 2.6rem;
    gap: 0.5rem;
    font-size: 1rem;
  }

  .current-classroom-summary {
    grid-template-columns: auto auto minmax(0, 1fr) auto;
    padding: 0.85rem 2.6rem;
    font-size: 0.92rem;
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
