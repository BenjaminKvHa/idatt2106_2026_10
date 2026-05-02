<script setup lang="ts">
import TeacherPupilNotesPanel from './TeacherPupilNotesPanel.vue'
import TeacherPupilSummaryCard from './TeacherPupilSummaryCard.vue'
import type { ActivePanel, Pupil, StopName } from './types'

defineProps<{
  selectedPupil: Pupil | null
  editUsername: string
  activePanel: ActivePanel
  stopNames: StopName[]
  selectedStopName: StopName | ''
  selectedChapterName: string
  selectedStopNote: string
  notebookLoading: boolean
  notebookError: string
}>()

const emit = defineEmits<{
  'update:editUsername': [value: string]
  saveChanges: []
  removePupil: []
  openNotes: []
  selectStopNote: [stopName: StopName]
  approvePendingPupil: []
  rejectPendingPupil: []
}>()
</script>

<template>
  <section class="details-panel">
    <div v-if="selectedPupil" class="details-card">
      <TeacherPupilSummaryCard
        :pupil="selectedPupil"
        :edit-username="editUsername"
        :show-save-button="selectedPupil.status === 'APPROVED'"
        @update:edit-username="emit('update:editUsername', $event)"
        @save-changes="emit('saveChanges')"
      />

      <div v-if="selectedPupil.status === 'APPROVED'" class="button-grid">
        <button class="danger-btn inika" type="button" @click="emit('removePupil')">
          Fjern fra klasserom
        </button>

        <button class="main-btn secondary-btn inika" type="button" @click="emit('openNotes')">
          Elev-notater
        </button>
      </div>

      <TeacherPupilNotesPanel
        v-if="selectedPupil.status === 'APPROVED' && activePanel === 'NOTES'"
        :stop-names="stopNames"
        :selected-stop-name="selectedStopName"
        :selected-chapter-name="selectedChapterName"
        :selected-stop-note="selectedStopNote"
        :loading="notebookLoading"
        :error="notebookError"
        @select-stop="emit('selectStopNote', $event)"
      />

      <div v-else-if="selectedPupil.status === 'PENDING'" class="pending-actions-inside">
        <p class="pending-message inika">
          {{ selectedPupil.legalName }} venter på godkjenning for å bli med i klasserommet.
        </p>

        <button class="main-btn primary-btn inika approve-pupil" type="button" @click="emit('approvePendingPupil')">
          Godkjenn elev
        </button>

        <button class="danger-btn inika" type="button" @click="emit('rejectPendingPupil')">
          Avslå forespørsel
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.approve-pupil {
  color: var(--beige-light) !important;
}

.details-card {
  width: min(calc(30*16rem), 100%);
  border: 7px solid var(--brown-medium-100);
  border-radius: 38px;
  padding: 28px;
}
@media (max-width: 768px) {
  .details-card {
    width: 100%;
    border: var(--border-thin) solid var(--brown-medium-100);
    border-radius: 1rem;
    padding: .5rem;
  }
}

.button-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 24px;
  margin-top: 28px;
}

.main-btn,
.danger-btn {
  border: 4px solid var(--brown-medium-100);
  border-radius: 999px;
  padding: 13px 18px;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.primary-btn {
  background: var(--brown-medium-100);
  color: var(--beige-light-100);
}

.primary-btn,
.primary-btn * {
  color: var(--beige-light-100);
}

.secondary-btn {
  background: var(--beige-light-100);
}

.danger-btn {
  background: var(--red-danger-100);
}

.danger-btn,
.danger-btn * {
  color: black;
}

.main-btn:hover,
.danger-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 0 var(--brown-medium-100);
}

.pending-actions-inside {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  align-items: center;
  margin-top: 24px;
}

.pending-message {
  width: 100%;
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--red-danger-100);
}

@media (max-width: 1100px) {
  .details-card {
    width: 100%;
  }

  .button-grid {
    grid-template-columns: 1fr;
  }
}
</style>
