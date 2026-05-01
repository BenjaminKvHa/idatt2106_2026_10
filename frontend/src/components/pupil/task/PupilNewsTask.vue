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
          v-if="!result"
          type="button"
          class="fake-button"
          :disabled="submitting"
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
  width: min(100%, 800px);
  padding: 1.35rem 2rem;
  background: linear-gradient(180deg, #fff3d6 0%, #f4d7a2 100%);
  border: 3px solid var(--brown-dark);
  border-radius: 30px;
  box-sizing: border-box;
  box-shadow:
    0 6px 18px rgb(0 0 0 / 13%),
    inset 0 2px 0 rgb(255 255 255 / 45%);
}

.task-wrapper p {
  margin: 0;
  color: #1f160f;
  font-weight: 750;
}

.picture-wrapper {
  display: flex;
  gap: 1.4rem;
  margin-top: 1rem;
  width: 100%;
  justify-content: center;
  align-items: flex-start;
  flex-wrap: nowrap;
}

.picture-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 0 1 calc(50% - 0.7rem);
  max-width: 430px;
  min-width: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
}

.picture-card:hover .picture-placeholder {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgb(0 0 0 / 14%);
}

.picture-placeholder {
  width: 100%;
  background-color: #fff6dc;
  border: 3px solid var(--brown-dark);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-sizing: border-box;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.picture-card.selected .picture-placeholder {
  border: 4px solid #7b5740;
  box-shadow:
    0 0 0 6px rgb(91 62 43 / 18%),
    0 12px 26px rgb(0 0 0 / 18%);
}

.picture-card.selected.correct .picture-placeholder {
  border: 4px solid var(--green-confirm);
  box-shadow:
    0 0 0 6px rgb(130 223 48 / 28%),
    0 12px 26px rgb(0 0 0 / 18%);
}

.picture-card.selected.wrong .picture-placeholder {
  border: 4px solid #d95b43;
  box-shadow:
    0 0 0 6px rgb(217 91 67 / 24%),
    0 12px 26px rgb(0 0 0 / 18%);
}

.picture {
  width: 100%;
  height: auto;
  display: block;
  object-fit: contain;
}

.answer-label {
  align-self: center;
  width: auto;
  max-width: calc(100% - 2rem);
  margin: 1rem 0 0;
  padding: 0.55rem 1.6rem;
  border: 2px solid var(--brown-dark);
  border-radius: 999px;
  font-weight: 850;
  text-align: center;
}

.correct-label {
  background: var(--green-confirm);
  color: #1f160f;
}

.wrong-label {
  background: #d95b43;
  color: white;
  border-radius: 999px;
}

.fake-button,
.confirm-button {
  width: calc(100% - 2rem);
  max-width: 360px;
  padding: 0.9rem 1.8rem;
  margin-top: 1.2rem;
  border-radius: 24px;
  border: 2px solid var(--brown-dark);
  background: linear-gradient(180deg, #fff6dc 0%, #f1d7a5 100%);
  color: var(--brown-dark);
  cursor: pointer;
  font-weight: 850;
  box-shadow: 0 5px 14px rgb(0 0 0 / 14%);
  transition:
    transform 0.15s ease,
    filter 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease,
    color 0.15s ease;
}

.picture-card.selected .fake-button {
  background: linear-gradient(180deg, #6f5139 0%, #4a301f 100%);
  color: #fff6dc;
  border-color: #2a1b12;
}

.fake-button:hover:not(:disabled),
.confirm-button:hover:not(:disabled) {
  filter: brightness(1.04);
  transform: translateY(-1px);
  box-shadow: 0 7px 18px rgb(0 0 0 / 16%);
}

.fake-button:active:not(:disabled),
.confirm-button:active:not(:disabled) {
  transform: translateY(1px);
  box-shadow: 0 3px 9px rgb(0 0 0 / 12%);
}

.fake-button:disabled,
.confirm-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
  transform: none;
  filter: none;
}

@media (max-width: 900px) {
  .picture-wrapper {
    flex-wrap: wrap;
  }

  .picture-card {
    flex: 1 1 100%;
    max-width: 520px;
  }
}

@media (max-width: 768px) {
  .picture-wrapper {
    gap: 1rem;
  }

  .task-wrapper {
    padding: 1rem;
    border-width: 2px;
    border-radius: 24px;
  }

  .picture-placeholder {
    border-width: 2px;
  }

  .fake-button,
  .confirm-button {
    border-radius: 20px;
  }
}
</style>
