<script setup lang="ts">
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue'

defineOptions({
  name: 'TeacherMysteryCard',
})

type MysteryCard = {
  id: number
  fileObjectId: number
  comment: string
  createdAt: string
  pupilLegalName?: string
  pupilEmail?: string
}

const props = defineProps<{
  mystery: MysteryCard
  pending?: boolean
  busy?: boolean
}>()

const emit = defineEmits<{
  accept: [id: number]
  remove: [id: number]
}>()

function formatTimestamp(value: string): string {
  const parsedDate = new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(parsedDate)
}

</script>

<template>
  <article class="mystery-card">
    <AuthenticatedImage 
    :src="`/api/objects/${props.mystery.fileObjectId}`" 
    alt="Ukens mysterium" 
    class="mystery-image" 
    preview-on-click
    />

    <div class="mystery-content">
      <div class="mystery-top-row">
        <div>
          <p v-if="props.pending && props.mystery.pupilLegalName" class="mystery-author">
            {{ props.mystery.pupilLegalName }}
          </p>
          <p v-if="props.pending && props.mystery.pupilEmail" class="mystery-email">
            {{ props.mystery.pupilEmail }}
          </p>
          <p v-else class="mystery-label">Godkjent mysterium</p>
        </div>

        <span v-if="props.pending" class="mystery-badge">Venter</span>
      </div>

      <p class="mystery-comment">{{ props.mystery.comment }}</p>
      <p class="mystery-time">{{ formatTimestamp(props.mystery.createdAt) }}</p>

      <div class="mystery-actions">
        <button
          v-if="props.pending"
          type="button"
          class="mystery-button approve-button"
          :disabled="props.busy"
          @click="emit('accept', props.mystery.id)"
        >
          Godkjenn
        </button>
        <button
          type="button"
          class="mystery-button decline-button"
          :disabled="props.busy"
          @click="emit('remove', props.mystery.id)"
        >
          {{ props.pending ? 'Avslå' : 'Slett' }}
        </button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.mystery-card {
  display: grid;
  grid-template-columns: 14rem minmax(0, 1fr);
  gap: 1rem;
  padding: 1rem;
  border-radius: 1.25rem;
  border: 2px solid rgba(85, 50, 27, 0.18);
  background: rgba(255, 248, 235, 0.82);
}

.mystery-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 1rem;
  border: 2px solid var(--accent);
  background: var(--background);
}

.mystery-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 0;
}

.mystery-top-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.mystery-author,
.mystery-email,
.mystery-label,
.mystery-comment,
.mystery-time {
  margin: 0;
  color: var(--brown-dark);
}

.mystery-author {
  font-weight: 700;
}

.mystery-email,
.mystery-time {
  font-size: 0.9rem;
  opacity: 0.8;
}

.mystery-comment {
  line-height: 1.4;
}

.mystery-badge {
  border-radius: 999px;
  padding: 0.3rem 0.7rem;
  background: #f6d58e;
  font-weight: 700;
  color: var(--brown-dark);
  white-space: nowrap;
}

.mystery-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: auto;
}

.mystery-button {
  border: 3px solid var(--accent);
  border-radius: 999px;
  background: var(--background);
  padding: 0.55rem 0.9rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.approve-button {
  background: var(--accent);
  color: var(--background);
}

.decline-button {
  background: var(--red-danger);
  color: var(--brown-dark);
}

.mystery-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

@media (max-width: 720px) {
  .mystery-card {
    grid-template-columns: 1fr;
  }

  .mystery-image {
    max-width: 18rem;
  }

  .mystery-top-row {
    flex-direction: column;
  }
}
</style>
