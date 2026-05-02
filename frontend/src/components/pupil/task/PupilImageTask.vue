<script setup lang="ts">
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue';
import type { ImageCategory, ValidationResult } from '@/types/chapter'

defineProps<{
  taskDescription?: string
  imageUrl?: string
  categories: { value: ImageCategory; label: string }[]
  justifications: string[]
  selectedCategory: ImageCategory | null
  selectedJustification: string | null
  result: ValidationResult | null
  submitting?: boolean
  showSubmitButton?: boolean
  submitText?: string
}>()

const emit = defineEmits<{
  selectCategory: [value: ImageCategory]
  selectJustification: [value: string]
  submit: []
}>()

function categoryClass(
  value: ImageCategory,
  selectedCategory: ImageCategory | null,
  result: ValidationResult | null,
): string {
  const selected = selectedCategory === value
  if (!selected) return ''
  if (!result) return 'selected'

  const ok = result.categoryCorrect ?? result.correct
  return ok ? 'selected pupil-correct' : 'selected pupil-wrong'
}

function justificationClass(
  value: string,
  selectedJustification: string | null,
  result: ValidationResult | null,
): string {
  const selected = selectedJustification === value
  if (!selected) return ''
  if (!result) return 'selected'

  const ok = result.justificationCorrect ?? result.correct
  return ok ? 'selected pupil-correct' : 'selected pupil-wrong'
}
</script>

<template>
  <div class="pupil-image-task">
    <p v-if="taskDescription" class="task-text">{{ taskDescription }}</p>

    <div
      class="image-card"
      :class="{
        'pupil-correct': result && result.correct,
        'pupil-wrong': result && result.correct === false,
      }"
    >
      <AuthenticatedImage v-if="imageUrl" :src="imageUrl" alt="Oppgavebilde" class="task-image" preview-on-click />
      <div v-else class="image-placeholder">Laster bilde…</div>
    </div>

    <div class="choices">
      <button
        v-for="cat in categories"
        :key="cat.value"
        type="button"
        :disabled="!!result"
        :class="categoryClass(cat.value, selectedCategory, result)"
        @click="emit('selectCategory', cat.value)"
      >
        {{ cat.label }}
      </button>
    </div>

    <div v-if="selectedCategory !== null" class="reason-box">
      <p>Hvorfor?</p>

      <button
        v-for="justification in justifications"
        :key="justification"
        type="button"
        :disabled="!!result"
        :class="justificationClass(justification, selectedJustification, result)"
        @click="emit('selectJustification', justification)"
      >
        {{ justification }}
      </button>
    </div>

    <button
      v-if="showSubmitButton"
      type="button"
      class="submit-button btn-orange"
      :disabled="submitting"
      @click="emit('submit')"
    >
      {{ submitText ?? 'Svar' }}
    </button>
  </div>
</template>

<style scoped>
.pupil-image-task {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
  width: 100%;
}

.task-text {
  text-align: center;
  margin: 0;
  font-size: 20px;
  color: var(--brown-dark)
}

.image-card {
  display: flex;
  justify-content: center;
  width: 100%;
  border-radius: 24px;
  border: 3px solid transparent;
  transition: all 0.2s ease;
}

.image-card.correct { border-color: var(--green-confirm)}
.image-card.wrong { border-color: var(--red-danger)}

.task-image {
  width: 100%;
  max-width: 620px;
  height: auto;
  background: var(--beige-light-100);
  border-radius: 14px;
  border: 2px solid var(--beige-light-60);
  margin: 0 auto;
}

.image-placeholder {
  width: 460px;
  max-width: 100%;
  height: 280px;
  background: whitesmoke;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  border: 2px solid var(--beige-light-60);
  font-size: 20px;
  font-weight: 600;
}

.choices {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.choices button {
  padding: 10px 16px;
  border-radius: 20px;
  border: 2px solid var(--brown-dark);
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.choices button:hover:not(:disabled) {
  background: var(--blue-medium-40);
  border-color: var(--blue-medium-100);
  color: var(--brown-dark);
  transform: scale(1.05);
}

.choices button.selected {
  background: var(--blue-medium-60);
  border-color: var(--blue-medium-100);
  color: white;
  transform: scale(1.08);
  box-shadow: 0 0 0 3px var(--blue-medium-100);
}

.reason-box {
  text-align: center;
}

.reason-box p {
  margin-bottom: 10px;
  font-weight: 600;
}

.reason-box button {
  margin: 5px;
  padding: 8px 14px;
  border-radius: 16px;
  border: 2px solid var(--beige-light-60);
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reason-box button:hover:not(:disabled) {
  background: white;
}

.reason-box button.selected {
  background: var(--blue-light);
  border-color: var(--blue-dark);
  transform: scale(1.05);
  color: var(--brown-dark);
}

.submit-button {
  width: 100%;
}
</style>
