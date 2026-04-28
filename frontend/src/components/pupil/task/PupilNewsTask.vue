<script setup lang="ts">
import type { ValidationResult } from '@/types/chapter'

const props = defineProps<{
  taskDescription?: string
  fileIds: number[]
  imageUrls: string[]
  selectedFileId: number | null
  result: ValidationResult | null
  submitting?: boolean
  selectButtonText?: string
  showConfirmButton?: boolean
  confirmButtonText?: string
}>()

const emit = defineEmits<{
  selectFile: [fileId: number]
  submit: []
}>()

function cardClass(fileId: number): string {
  if (props.selectedFileId !== fileId) return ''
  if (!props.result) return 'selected'

  return props.result.correct ? 'selected correct' : 'selected wrong'
}
</script>

<template>
  <div class="pupil-news-task">
    <div v-if="taskDescription" class="task-wrapper">
      <p>{{ taskDescription }}</p>
    </div>

    <div class="picture-wrapper">
      <div
        v-for="(fileId, idx) in fileIds"
        :key="fileId"
        class="picture-card"
        :class="cardClass(fileId)"
      >
        <div class="picture-placeholder">
          <img
            v-if="imageUrls[idx]"
            :src="imageUrls[idx]"
            :alt="`Bilde ${Number(idx) + 1}`"
            class="picture"
          />
          <span v-else>Laster…</span>
        </div>

        <p
          v-if="result && selectedFileId === fileId"
          class="answer-label"
          :class="result.correct ? 'correct-label' : 'wrong-label'"
        >
          {{ result.correct ? 'Riktig svar' : 'Ditt valg' }}
        </p>

        <button
          type="button"
          class="fake-button"
          :disabled="!!result || submitting"
          @click="emit('selectFile', fileId)"
        >
          {{ selectButtonText ?? 'Velg dette bildet' }}
        </button>
      </div>
    </div>

    <button
      v-if="showConfirmButton && selectedFileId !== null && !result"
      type="button"
      class="confirm-button pupil-orange-button"
      :disabled="submitting"
      @click="emit('submit')"
    >
      {{ confirmButtonText ?? 'Bekreft svar' }}
    </button>
  </div>
</template>

<style scoped>
.pupil-news-task {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  gap: 1.25rem;
}

.task-wrapper {
  position: relative;
  width: min(100%, 800px);
  padding: 1.4rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
  box-shadow: 0 8px 0 var(--brown-dark);
}

.task-wrapper p {
  margin: 0;
}

.picture-wrapper {
  display: flex;
  gap: 2rem;
  margin-top: 1rem;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}

.picture-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  flex: 1 1 300px;
  max-width: 520px;
  width: 100%;
  padding: 0;
  border-radius: 30px;
  background: rgb(255 255 255 / 35%);
  border: 3px solid rgb(91 62 43 / 25%);
  box-shadow: 0 10px 20px rgb(0 0 0 / 10%);
  overflow: hidden;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background 0.2s ease;
}

.picture-card:hover {
  transform: translateY(-4px);
}

.picture-card.selected {
  box-shadow: 0 0 0 5px var(--orange-medium);
}

.picture-card.correct {
  box-shadow: 0 0 0 6px var(--green-confirm);
  background: rgb(120 190 120 / 22%);
}

.picture-card.wrong {
  box-shadow: 0 0 0 6px #d95b43;
  background: rgb(217 91 67 / 18%);
}

.picture-placeholder {
  width: 100%;
  min-height: 360px;
  background-color: var(--beige-light);
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 0;
  box-sizing: border-box;
}

.picture {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
}

.answer-label {
  margin: 0;
  padding: 0.45rem 1.2rem;
  border: 3px solid var(--brown-dark);
  border-radius: 999px;
  font-weight: 700;
}

.correct-label {
  background: var(--green-confirm);
}

.wrong-label {
  background: #d95b43;
  color: white;
}

.fake-button,
.confirm-button {
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--orange-medium);
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 6px 0 var(--brown-dark);
  transition:
    transform 0.2s ease,
    filter 0.2s ease,
    box-shadow 0.2s ease;
}

.fake-button {
  width: calc(100% - 2rem);
  margin-bottom: 1rem;
}

.fake-button:hover:not(:disabled),
.confirm-button:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-3px);
  box-shadow: 0 9px 0 var(--brown-dark);
}

.fake-button:active:not(:disabled),
.confirm-button:active:not(:disabled) {
  transform: translateY(3px);
  box-shadow: 0 3px 0 var(--brown-dark);
}

.fake-button:disabled,
.confirm-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
  transform: none;
  filter: none;
  box-shadow: 0 6px 0 var(--brown-dark);
}

@media (max-width: 768px) {
  .picture-wrapper {
    gap: 1rem;
  }

  .task-wrapper {
    padding: 1rem;
    border-width: 3px;
    border-radius: 24px;
  }

  .fake-button,
  .confirm-button {
    border-width: 3px;
    border-radius: 24px;
  }
}
</style>
