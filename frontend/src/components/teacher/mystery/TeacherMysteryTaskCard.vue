<script setup lang="ts">
import { computed } from 'vue'
import {
  CHAPTER_LABELS,
  DATACENTER_FORMAT_LABELS,
  formatTimestamp,
  getCorrectFileSlot,
  type ChapterTaskKey,
  type DatacenterTaskFormat,
  type ImageVerificationTask,
  type NewsVerificationTask,
  type TaskAdminResponse,
} from '@/components/teacher/mystery/types'

const props = defineProps<{
  chapter: ChapterTaskKey
  task: TaskAdminResponse
  imageTask?: ImageVerificationTask | null
  newsTask?: NewsVerificationTask | null
  format?: DatacenterTaskFormat | null
  selected?: boolean
}>()

const emit = defineEmits<{
  edit: [id: number]
}>()

const correctFileSlot = computed(() => {
  if (!props.newsTask) {
    return null
  }

  return getCorrectFileSlot(props.newsTask)
})
</script>

<template>
  <article :class="['task-card', { selected: selected }]">
    <div class="task-card-header">
      <button type="button" class="button-variant-1 task-edit-button" @click="emit('edit', task.id)">
        {{ selected ? 'Valgt for redigering' : 'Rediger' }}
      </button>
    </div>

    <div class="task-meta-row">
      <span>{{ CHAPTER_LABELS[chapter] }}</span>
      <span v-if="format">{{ DATACENTER_FORMAT_LABELS[format] }}</span>
      <span>Vanskelighetsgrad: {{ task.difficulty }}</span>
      <span>Opprettet: {{ formatTimestamp(task.createdAt) }}</span>
    </div>

    <template v-if="imageTask">
      <div class="image-task-layout">
        <img
          v-if="imageTask.fileId"
          :src="`/api/objects/${imageTask.fileId}`"
          alt="Oppgavebilde"
          class="single-task-image"
        />

        <div class="task-details">
          <p><strong>Beskrivelse:</strong> {{ imageTask.taskDescription || 'Ingen beskrivelse' }}</p>
          <p><strong>Riktig kategori:</strong> {{ imageTask.correctCategory || 'Ukjent' }}</p>
          <p><strong>Riktig begrunnelse:</strong> {{ imageTask.correctJustification || 'Mangler' }}</p>

          <div v-if="imageTask.justifications?.length" class="task-list-block">
            <strong>Begrunnelser</strong>
            <ul>
              <li v-for="justification in imageTask.justifications" :key="justification">
                {{ justification }}
              </li>
            </ul>
          </div>

          <p><strong>Ved riktig svar:</strong> {{ imageTask.correctDescription || 'Ingen tekst' }}</p>
          <p><strong>Ved feil svar:</strong> {{ imageTask.falseDescription || 'Ingen tekst' }}</p>
        </div>
      </div>
    </template>

    <template v-else-if="newsTask">
      <div class="task-details">
        <p><strong>Beskrivelse:</strong> {{ newsTask.taskDescription || 'Ingen beskrivelse' }}</p>
        <p><strong>Ved riktig svar:</strong> {{ newsTask.correctDescription || 'Ingen tekst' }}</p>
        <p><strong>Ved feil svar:</strong> {{ newsTask.incorrectDescription || 'Ingen tekst' }}</p>
      </div>

      <div class="news-image-grid">
        <div class="image-panel">
          <img
          v-if="newsTask.fileId_1"
          :src="`/api/objects/${newsTask.fileId_1}`"
          alt="Nyhetsoppgave fil 1"
          class="news-task-image"
          />
          <div class="image-label-row">
            <span v-if="correctFileSlot === 'FILE_1'" class="answer-chip">Riktig</span>
          </div>
        </div>

        <div class="image-panel">
          <img
          v-if="newsTask.fileId_2"
          :src="`/api/objects/${newsTask.fileId_2}`"
          alt="Nyhetsoppgave fil 2"
          class="news-task-image"
          />
          <div class="image-label-row">
            <span v-if="correctFileSlot === 'FILE_2'" class="answer-chip">Riktig</span>
          </div>
        </div>
      </div>
    </template>

    <pre v-else class="task-json">{{ task.taskJson }}</pre>
  </article>
</template>

<style scoped>
.task-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1rem;
  border-radius: 1rem;
  border: 2px solid rgba(90, 64, 47, 0.16);
  background: rgba(255, 248, 235, 0.9);
}

.task-card.selected {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(90, 64, 47, 0.12);
}

.task-card-header,
.task-meta-row,
.image-label-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
}

.task-card-header h3,
.task-card-header p,
.task-details p,
.task-meta-row,
.task-list-block ul,
.task-json {
  margin: 0;
}

.task-edit-button {
  padding: 0.7rem 1rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.task-meta-row {
  font-size: 0.92rem;
  opacity: 0.82;
}

.task-details {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.image-task-layout {
  display: grid;
  grid-template-columns: minmax(12rem, 16rem) minmax(0, 1fr);
  gap: 1rem;
}

.single-task-image,
.news-task-image {
  width: 100%;
  border-radius: 0.9rem;
  border: 2px solid rgba(90, 64, 47, 0.18);
  background: var(--background);
  object-fit: cover;
}

.single-task-image {
  aspect-ratio: 1;
}

.news-task-image {
  aspect-ratio: 4 / 3;
}

.task-list-block {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.task-list-block ul {
  padding-left: 1.2rem;
}

.news-image-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.image-panel {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.answer-chip {
  padding: 0.25rem 0.65rem;
  border-radius: 999px;
  background: var(--accent);
  color: var(--background);
  font-size: 0.85rem;
  font-weight: 700;
}

.task-json {
  padding: 0.85rem;
  border-radius: 0.75rem;
  background: var(--background);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 760px) {
  .image-task-layout,
  .news-image-grid {
    grid-template-columns: 1fr;
  }
}
</style>
