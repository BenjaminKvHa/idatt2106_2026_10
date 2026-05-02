<script setup lang="ts">
import { onMounted } from 'vue'
import { useClassrooms } from '@/composables/useClassrooms'
import StateDisplay from '@/components/ui/StateDisplay.vue'

const { classrooms, loading, error, fetchClassrooms } = useClassrooms()

onMounted(() => {
  fetchClassrooms()
})

</script>

<template>
  <StateDisplay
    :loading="loading"
    :error="error"
    loading-text="Laster klasserom..."
  />

  <div v-if="!loading && !error && classrooms.length === 0" class="state-message">
    <p>Du har ingen klasserom ennå.</p>
    <p>Opprett ditt første klasserom ved å klikke "Nytt klasserom" i menyen.</p>
  </div>

  <div v-if="!loading && !error && classrooms.length > 0" class="card-grid">
    <div v-for="classroom in classrooms" :key="classroom.classroomCode" class="teacher-card">
      <div class="teacher-card-header">
        <div class="card-classroom-name">{{ classroom.classroomName }}</div>
        <div class="card-classroom-code">{{ classroom.classroomCode }}</div>
      </div>
      <div class="teacher-card-body">
        <div class="row">
          <span>Poeng:</span>
          <span class="value">{{ classroom.totalScore }}</span>
        </div>
        <div class="row">
          <span>Rangering:</span>
          <span class="value">#{{ classroom.globalRank }}</span>
        </div>
        <RouterLink :to="`/teacher/classrooms/${classroom.classroomCode}/leaderboard`">
          <button
            type="button"
            class="button-variant-1 teacher-menu-button"
          >
            Gå til klasserom
          </button>
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>

.card-classroom-name {
  font-size: 1.2rem;
  font-weight: var(--fw-bold);
  overflow-wrap: anywhere;
}

.card-classroom-code {
  font-size: 0.8rem;
  opacity: 0.9;
  letter-spacing: 0.08em;
}

.teacher-menu-button {
  width: 100%;
  padding: 0.85rem 1rem;
}

.teacher-card-body {
  min-width: 0;
}

.row {
  gap: 0.75rem;
}

.row span {
  min-width: 0;
}

.value {
  overflow-wrap: anywhere;
}

@media (max-width: 640px) {
  .teacher-card {
    min-height: 0;
  }

  .teacher-card-header,
  .teacher-card-body {
    padding: 0.9rem;
  }

  .card-classroom-name {
    font-size: 1.05rem;
  }

  .teacher-menu-button {
    padding: 0.75rem 0.85rem;
  }
}

</style>
