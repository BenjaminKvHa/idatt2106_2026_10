<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import LeaderboardOfPupils from '@/components/ui/LeaderboardOfPupils.vue'
import { get } from '@/utils/apiUtils'

type ClassroomPupilLeaderboardEntry = {
  username: string | null
  legalName: string | null
  totalXp: number
}

type ClassroomPupilLeaderboardResponse = {
  classroomCode: string
  classroomName: string
  classroomDescription: string | null
  entries: ClassroomPupilLeaderboardEntry[]
}

const route = useRoute()

const leaderboard = ref<ClassroomPupilLeaderboardResponse | null>(null)
const loading = ref(true)
const error = ref('')

const sortedEntries = computed(() => {
  if (!leaderboard.value) {
    return []
  }

  return [...leaderboard.value.entries].sort((a, b) => b.totalXp - a.totalXp)
})

async function loadLeaderboard() {
  loading.value = true
  error.value = ''

  try {
    const classroomCodeSlug =
      typeof route.params.classroomCode === 'string' ? route.params.classroomCode : ''
    if (!classroomCodeSlug) {
      throw new Error('Could not find classroom for this route')
    }

    leaderboard.value = await get<ClassroomPupilLeaderboardResponse>(
      `/api/classrooms/${classroomCodeSlug}/leaderboard`,
    )
  } catch (fetchError) {
    leaderboard.value = null
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load leaderboard'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadLeaderboard()
})

watch(
  () => route.params.classroomCode,
  async () => {
    await loadLeaderboard()
  },
)
</script>

<template>
  <div class="teacher-container-box leaderboard-view">
    <p v-if="loading" class="state-message">Laster ledertavle...</p>
    <p v-else-if="error" class="state-message error">{{ error }}</p>

    <template v-else-if="leaderboard">
      <h1>{{ leaderboard.classroomName }}</h1>
      <p v-if="leaderboard.classroomDescription">{{ leaderboard.classroomDescription }}</p>
      <p><b>Romkode:</b> {{ leaderboard.classroomCode }}</p>
      <h2>Ledertavle</h2>
      <LeaderboardOfPupils :entries="sortedEntries" />
    </template>
  </div>
</template>

<style scoped>
.leaderboard-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.leaderboard-view h1,
.leaderboard-view h2,
.leaderboard-view p {
  margin: 0;
}

.state-message {
  text-align: center;
  color: var(--brown-dark);
}

.state-message.error {
  color: #9f1d1d;
}
</style>
