<script setup lang="ts">
import TeacherHelpButton from '@/components/teacher/TeacherHelpButton.vue'
import TeacherPupilPreview from './TeacherPupilPreview.vue'
import type { Pupil } from './types'

defineProps<{
  approvedPupils: Pupil[]
  pendingPupils: Pupil[]
  selectedPupilEmail: string
}>()

const emit = defineEmits<{
  selectPupil: [email: string]
}>()

function selectPupil(email: string) {
  emit('selectPupil', email)
}

const pupilsHelpSections = [
  {
    body: [
      'Her ser du elever som er godkjent i klasserommet, og elever som venter på godkjenning.',
      'Velg en elev for å se detaljer, endre brukernavn, fjerne eleven fra klasserommet eller lese elevens notater. Nye elever får ikke tilgang til elevsidene før du godkjenner dem.',
    ],
  },
]
</script>

<template>
  <aside class="pupil-list-panel">
    <div class="pupil-list-header">
      <h1 class="page-title">Elever</h1>
      <TeacherHelpButton title="Elever og forespørsler" :sections="pupilsHelpSections" />
    </div>

    <p v-if="approvedPupils.length === 0">Ingen elever</p>
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
  </aside>
</template>

<style scoped>
.pupil-list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--medium-gap);
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
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

</style>
