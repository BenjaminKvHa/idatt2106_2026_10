<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue'
import type {
  FotografenTaskFormValues,
  ImageCategory,
  ImageVerificationTask,
  TaskAdminResponse,
  TaskDifficulty,
} from '@/components/teacher/mystery/types'

type JustificationEntry = {
  id: number
  text: string
}

const props = defineProps<{
  task?: TaskAdminResponse | null
  parsedTask?: ImageVerificationTask | null
  submitting?: boolean
  resetToken?: number
  title?: string
  lockedDifficulty?: TaskDifficulty | null
  hideDifficulty?: boolean
}>()

const emit = defineEmits<{
  submit: [values: FotografenTaskFormValues]
  cancelEdit: []
}>()

const difficulty = ref<TaskDifficulty>('EASY')
const taskDescription = ref('')
const correctCategory = ref<ImageCategory>('REAL')
const justificationEntries = ref<JustificationEntry[]>([])
const selectedCorrectJustificationId = ref<number | null>(null)
const correctDescription = ref('')
const falseDescription = ref('')
const selectedFile = ref<File | null>(null)
const selectedFilePreview = ref('')
const validationError = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
let nextJustificationId = 1

const difficultyOptions: TaskDifficulty[] = ['EASY', 'MEDIUM', 'HARD']
const categoryOptions: ImageCategory[] = ['REAL', 'AI_GENERATED', 'MANIPULATED']

watch(
  () => [props.task?.id, props.resetToken],
  () => {
    resetForm()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  revokePreview()
})

function revokePreview() {
  if (!selectedFilePreview.value) {
    return
  }

  URL.revokeObjectURL(selectedFilePreview.value)
  selectedFilePreview.value = ''
}

function createJustificationEntry(text = ''): JustificationEntry {
  const entry = {
    id: nextJustificationId,
    text,
  }

  nextJustificationId += 1
  return entry
}

function addJustification(text = '') {
  const entry = createJustificationEntry(text)
  justificationEntries.value = [...justificationEntries.value, entry]

  if (selectedCorrectJustificationId.value === null) {
    selectedCorrectJustificationId.value = entry.id
  }
}

function removeJustification(id: number) {
  const nextEntries = justificationEntries.value.filter((entry) => entry.id !== id)
  justificationEntries.value = nextEntries

  if (selectedCorrectJustificationId.value === id) {
    selectedCorrectJustificationId.value = nextEntries[0]?.id ?? null
  }
}

function resetForm() {
  revokePreview()
  validationError.value = ''
  selectedFile.value = null

  if (fileInput.value) {
    fileInput.value.value = ''
  }

  difficulty.value = props.lockedDifficulty ?? props.task?.difficulty ?? 'EASY'
  taskDescription.value = props.parsedTask?.taskDescription ?? ''
  correctCategory.value = props.parsedTask?.correctCategory ?? 'REAL'

  nextJustificationId = 1
  const entries = (props.parsedTask?.justifications?.length ? props.parsedTask.justifications : ['']).map((text) =>
    createJustificationEntry(text),
  )
  justificationEntries.value = entries
  selectedCorrectJustificationId.value =
    entries.find((entry) => entry.text === props.parsedTask?.correctJustification)?.id ?? entries[0]?.id ?? null

  correctDescription.value = props.parsedTask?.correctDescription ?? ''
  falseDescription.value = props.parsedTask?.falseDescription ?? ''
}

function updateSelectedFile(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null

  revokePreview()
  selectedFile.value = file

  if (file) {
    selectedFilePreview.value = URL.createObjectURL(file)
  }
}

function submitForm() {
  validationError.value = ''

  const normalizedTaskDescription = taskDescription.value.trim()
  const normalizedCorrectDescription = correctDescription.value.trim()
  const normalizedFalseDescription = falseDescription.value.trim()
  const justifications = justificationEntries.value.map((entry) => entry.text.trim())
  const selectedJustification = justificationEntries.value.find(
    (entry) => entry.id === selectedCorrectJustificationId.value,
  )
  const normalizedCorrectJustification = selectedJustification?.text.trim() ?? ''

  if (!normalizedTaskDescription) {
    validationError.value = 'Skriv en oppgavebeskrivelse.'
    return
  }

  if (justifications.length === 0) {
    validationError.value = 'Legg inn minst ett begrunnelsesvalg.'
    return
  }

  if (justifications.some((justification) => justification.length === 0)) {
    validationError.value = 'Fyll ut eller fjern tomme begrunnelser.'
    return
  }

  if (new Set(justifications).size !== justifications.length) {
    validationError.value = 'Begrunnelsene må være unike.'
    return
  }

  if (!normalizedCorrectJustification) {
    validationError.value = 'Velg hvilken begrunnelse som er riktig.'
    return
  }

  if (!normalizedCorrectDescription || !normalizedFalseDescription) {
    validationError.value = 'Fyll inn tilbakemelding for både riktig og feil svar.'
    return
  }

  if (!props.task && !selectedFile.value) {
    validationError.value = 'Velg et bilde for å opprette oppgaven.'
    return
  }

  emit('submit', {
    difficulty: difficulty.value,
    taskDescription: normalizedTaskDescription,
    correctCategory: correctCategory.value,
    correctJustification: normalizedCorrectJustification,
    justifications,
    correctDescription: normalizedCorrectDescription,
    falseDescription: normalizedFalseDescription,
    file: selectedFile.value,
  })
}
</script>

<template>
  <section class="task-form-card">
    <div class="form-header-row">
      <div>
        <h2>{{ title ?? (task ? 'Rediger Fotografen-oppgave' : 'Opprett Fotografen-oppgave') }}</h2>
      </div>
    </div>

    <p v-if="validationError" class="form-message error">{{ validationError }}</p>

    <div class="form-layout">
      <label v-if="!hideDifficulty" class="form-field">
        <span>Vanskelighetsgrad</span>
        <select v-model="difficulty" :disabled="submitting">
          <option v-for="option in difficultyOptions" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>

      <label class="form-field">
        <span>Beskrivelse</span>
        <textarea v-model="taskDescription" rows="3" :disabled="submitting" />
      </label>

      <label class="form-field">
        <span>Riktig kategori</span>
        <select v-model="correctCategory" :disabled="submitting">
          <option v-for="option in categoryOptions" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>

      <fieldset class="justifications-fieldset">
        <div class="justifications-header">
          <p class="fieldset-title">Begrunnelser</p>
          <button type="button" placeholder="Skriv begrunnelse her..." class="button-variant-1 add-button" :disabled="submitting" @click="addJustification()">
            Legg til begrunnelse
          </button>
        </div>

        <p class="helper-text">Skriv inn svaralternativene og marker hvilken begrunnelse som er riktig.</p>

        <div v-if="justificationEntries.length === 0" class="empty-justifications">
          Ingen begrunnelser ennå.
        </div>

        <div v-else class="justifications-list">
          <div v-for="entry in justificationEntries" :key="entry.id" class="justification-row">
            <label class="correct-choice-label">
              <input
                v-model="selectedCorrectJustificationId"
                type="radio"
                name="correctJustification"
                :value="entry.id"
                :disabled="submitting"
              />
              <span>Riktig</span>
            </label>

            <input v-model="entry.text" type="text" :disabled="submitting" class="justification-input" />

            <button
              type="button"
              class="button-variant-1 remove-button"
              :disabled="submitting"
              @click="removeJustification(entry.id)"
            >
              Fjern
            </button>
          </div>
        </div>
      </fieldset>

      <label class="form-field">
        <span>Tilbakemelding ved riktig svar</span>
        <textarea v-model="correctDescription" placeholder="Skriv tilbakemelding ved riktig svar her..." rows="3" :disabled="submitting"></textarea>
      </label>

      <label class="form-field">
        <span>Tilbakemelding ved feil svar</span>
        <textarea v-model="falseDescription" placeholder="Skriv tilbakemelding ved feil svar her..." rows="3" :disabled="submitting"></textarea>
      </label>

      <label class="form-field">
        <span>{{ task ? 'Bytt bilde (valgfritt)' : 'Bilde' }}</span>
        <input ref="fileInput" type="file" accept="image/*" :disabled="submitting" @change="updateSelectedFile" />
      </label>

      <div v-if="selectedFilePreview || parsedTask?.fileId" class="preview-panel">
        <img
          v-if="selectedFilePreview"
          :src="selectedFilePreview"
          :alt="task ? 'Eksisterende eller nytt oppgavebilde' : 'Valgt oppgavebilde'"
          class="preview-image"
        />
        <AuthenticatedImage
          v-else-if="parsedTask?.fileId"
          :src="`/api/objects/${parsedTask.fileId}`"
          :alt="task ? 'Eksisterende eller nytt oppgavebilde' : 'Valgt oppgavebilde'"
          class="preview-image"
          preview-image
        />
      </div>
    </div>

    <div class="form-actions">
      <button type="button" class="button-variant-2 submit-button" :disabled="submitting" @click="submitForm">
        {{ submitting ? 'Lagrer...' : task ? 'Oppdater oppgave' : 'Opprett oppgave' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.task-form-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  padding: 1.25rem;
  border-radius: 1.1rem;
  background: rgba(255, 248, 235, 0.88);
  border: 2px solid rgba(90, 64, 47, 0.16);
}

.form-header-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: flex-start;
}

.eyebrow,
.form-header-row h2,
.form-message {
  margin: 0;
}

.eyebrow {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  opacity: 0.74;
}

.secondary-button,
.submit-button {
  padding: 0.8rem 1rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.form-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.form-field span {
  font-weight: 700;
}

.form-field input,
.form-field textarea,
.form-field select {
  width: 100%;
  border-radius: 0.9rem;
  border: 2px solid rgba(90, 64, 47, 0.18);
  background: var(--beige-light-100);
  padding: 0.85rem 1rem;
  font: inherit;
  color: var(--brown-dark);
}

.form-field textarea {
  resize: vertical;
}

.preview-panel {
  display: flex;
}

.justifications-fieldset {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin: 0;
  padding: 1rem;
  border-radius: 1rem;
  border: 2px solid rgba(90, 64, 47, 0.18);
}

.justifications-header {
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
}

.fieldset-title {
  margin: 0;
  font-weight: 700;
}

.helper-text,
.empty-justifications {
  margin: 0;
  color: var(--brown-dark);
  opacity: 0.82;
}

.justifications-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.justification-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
}

.correct-choice-label {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 700;
}

.justification-input {
  min-width: 0;
}

.add-button,
.remove-button {
  padding: 0.7rem 0.9rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.preview-image {
  width: min(100%, 18rem);
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 1rem;
  border: 2px solid rgba(90, 64, 47, 0.18);
}

.form-message.error {
  color: #9f1d1d;
}

@media (max-width: 760px) {
  .justification-row {
    grid-template-columns: 1fr;
  }
}
</style>
