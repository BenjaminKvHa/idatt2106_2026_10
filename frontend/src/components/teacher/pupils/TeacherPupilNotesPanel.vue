<script setup lang="ts">
import type { StopName } from './types'

defineProps<{
  stopNames: StopName[]
  selectedStopName: StopName | ''
  selectedChapterName: string
  selectedStopNote: string
  loading: boolean
  error: string
}>()

const emit = defineEmits<{
  selectStop: [stopName: StopName]
}>()

function selectStop(stopName: StopName) {
  emit('selectStop', stopName)
}
</script>

<template>
  <div class="extra-fields inika">
    <h2>Elev-notater</h2>

    <div class="stop-button-grid">
      <button
        v-for="stopName in stopNames"
        :key="stopName"
        class="stop-note-btn inika"
        :class="{ active: selectedStopName === stopName }"
        type="button"
        @click="selectStop(stopName)"
      >
        {{ stopName }}
      </button>
    </div>

    <p v-if="loading" class="notes-state">Laster notater...</p>
    <p v-else-if="error" class="notes-state error-text">{{ error }}</p>

    <div v-else-if="selectedStopName" class="read-only-box inter">
      <h3>{{ selectedChapterName || selectedStopName }}</h3>
      <p>{{ selectedStopNote || 'Eleven har ikke skrevet inn noe.' }}</p>
    </div>
  </div>
</template>

<style scoped>
.extra-fields {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.extra-fields h2 {
  margin: 0;
  font-size: 1.35rem;
}

.stop-button-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stop-note-btn {
  border: 4px solid var(--accent);
  border-radius: 999px;
  padding: 13px 18px;
  font-size: 1.05rem;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
  background: var(--background);
}

.stop-note-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 0 var(--accent);
}

.stop-note-btn.active {
  background: var(--accent);
  color: var(--txt-sec);
}

.read-only-box {
  width: 100%;
  min-height: 90px;
  border: 3px solid var(--background-sec);
  border-radius: 14px;
  background: var(--background);
  padding: 14px 16px;
  font-size: 1rem;
}

.read-only-box h3 {
  margin: 0 0 8px;
  font-size: 1.15rem;
}

.read-only-box p {
  margin: 0;
  line-height: 1.35;
}

.notes-state {
  margin: 0;
}

.error-text {
  color: #9f1d1d;
}

@media (max-width: 1100px) {
  .stop-button-grid {
    grid-template-columns: 1fr;
  }
}
</style>
