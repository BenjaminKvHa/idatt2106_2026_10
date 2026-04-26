<script setup lang="ts">
type LeaderboardEntry = {
  username: string | null
  totalXp: number
}

const props = defineProps<{
  entries: LeaderboardEntry[]
}>()

function getDisplayName(username: string | null): string {
  if (!username || username.trim().length === 0) {
    return 'Uten kallenavn'
  }

  return username
}
</script>

<template>
  <div v-if="props.entries.length === 0" class="empty-state">
    Ingen elever på ledertavlen ennå.
  </div>

  <ol v-else class="leaderboard-list">
    <li v-for="(entry, index) in props.entries" :key="`${entry.username}-${index}`" class="leaderboard-item">
      <span class="place">#{{ index + 1 }}</span>
      <span class="username">{{ getDisplayName(entry.username) }}</span>
      <span class="xp">{{ entry.totalXp }} XP</span>
    </li>
  </ol>
</template>

<style scoped>
.empty-state {
  text-align: center;
  padding: 1rem;
  border-radius: 1rem;
  background: rgba(255, 248, 235, 0.82);
  color: var(--brown-dark);
  border: 1px solid rgba(85, 50, 27, 0.18);
}

.leaderboard-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.leaderboard-item {
  display: grid;
  grid-template-columns: 4.5rem minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.75rem;
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  background: rgba(255, 248, 235, 0.82);
  border: 1px solid rgba(85, 50, 27, 0.18);
  color: var(--brown-dark);
}

.place {
  font-weight: 800;
}

.username {
  font-weight: 700;
}

.xp {
  white-space: nowrap;
  font-weight: 700;
}

@media (max-width: 640px) {
  .leaderboard-item {
    grid-template-columns: 1fr auto;
  }

  .place {
    grid-column: 1 / -1;
  }
}
</style>
