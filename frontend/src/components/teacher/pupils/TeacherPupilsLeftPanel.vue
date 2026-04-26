<script setup lang="ts">
import TeacherPupilPreview from './TeacherPupilPreview.vue'
import type { Pupil } from './types'

defineProps<{
  approvedPupils: Pupil[]
  pendingPupils: Pupil[]
  selectedPupilEmail: string
}>()

const emit = defineEmits<{
  selectPupil: [email: string]
  resetData: []
}>()

function selectPupil(email: string) {
  emit('selectPupil', email)
}

function resetData() {
  emit('resetData')
}
</script>

<template>
  <aside class="pupil-list-panel">
    <h1 class="page-title">Elever</h1>

    <div class="list-section">
      <TeacherPupilPreview
        v-for="pupil in approvedPupils"
        :key="pupil.email"
        :pupil="pupil"
        :selected="pupil.email === selectedPupilEmail"
        @select="selectPupil"
      />
    </div>

    <div v-if="pendingPupils.length > 0" class="pending-section">
      <h2 class="pending-title">Venter på godkjenning:</h2>

      <TeacherPupilPreview
        v-for="pupil in pendingPupils"
        :key="pupil.email"
        :pupil="pupil"
        :selected="pupil.email === selectedPupilEmail"
        @select="selectPupil"
      />
    </div>

    <button class="reset-btn" type="button" @click="resetData">Nullstill testdata</button>
  </aside>
</template>

<style scoped>
.page-title {
  margin: 0 0 18px;
  font-size: 2.2rem;
  font-weight: 700;
}

.list-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pending-section {
  margin-top: 26px;
}

.pending-title {
  margin: 0 0 10px;
  font-size: 1.1rem;
  font-weight: 700;
}

.reset-btn {
  margin-top: 24px;
  border: 3px solid var(--accent);
  border-radius: 999px;
  background: var(--background);
  padding: 9px 14px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
}
</style>
