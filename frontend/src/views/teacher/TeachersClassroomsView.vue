<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useClassrooms } from '@/composables/useClassrooms'

const router = useRouter()
const { classrooms, loading, error, fetchClassrooms } = useClassrooms()

onMounted(() => {
  fetchClassrooms()
})

const navigateToClassroom = (classroomCode: string) => {
  const classroomName = classrooms.value
    .find((c) => c.classroomCode === classroomCode)
    ?.classroomName.toLowerCase()
    .replace(/\s+/g, '-')
  if (classroomName) {
    router.push(`/teacher/classrooms/${classroomName}`)
  }
}
</script>

<template>
  <div class="teacher-content">
    <div v-if="loading" class="state-message">
      Laster klasserom...
    </div>

    <div v-else-if="error" class="state-message error" role="alert">
      {{ error }}
    </div>

    <div v-else-if="classrooms.length === 0" class="state-message">
      <p>Du har ingen klasserom ennå.</p>
      <p>Opprett ditt første klasserom ved å klikke "Nytt klasserom" i menyen.</p>
    </div>

    <div v-else class="card-grid">
      <div v-for="classroom in classrooms" :key="classroom.classroomCode" class="teacher-card">
        <div class="teacher-card-header">
          <div style="font-size: 1.2rem; font-weight: 700; margin-bottom: var(--thin-padding)">
            {{ classroom.classroomName }}
          </div>
          <div style="font-size: 0.85rem; opacity: 0.9">
            {{ classroom.classroomCode }}
          </div>
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
          <button
            type="button"
            class="button-variant-1"
            style="width: 100%; padding: 0.85rem 1rem; margin-top: var(--thin-padding)"
            @click="navigateToClassroom(classroom.classroomCode)"
          >
            Gå til klasserom
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
