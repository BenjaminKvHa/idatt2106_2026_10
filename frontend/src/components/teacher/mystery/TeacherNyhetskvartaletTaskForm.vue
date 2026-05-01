<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import AuthenticatedImage from '@/components/ui/AuthenticatedImage.vue'
import type {
  CorrectFileSlot,
  NewsVerificationTask,
  NyhetskvartaletTaskFormValues,
  TaskAdminResponse,
  TaskDifficulty,
} from '@/components/teacher/mystery/types'
import { getCorrectFileSlot } from '@/components/teacher/mystery/types'

const props = defineProps<{
  task?: TaskAdminResponse | null
  parsedTask?: NewsVerificationTask | null
  submitting?: boolean
  resetToken?: number
  title?: string
  lockedDifficulty?: TaskDifficulty | null
  hideDifficulty?: boolean
}>()

const emit = defineEmits<{
  submit: [values: NyhetskvartaletTaskFormValues]
  cancelEdit: []
}>()

const difficulty = ref<TaskDifficulty>('EASY')
const taskDescription = ref('')
const correctFileSlot = ref<CorrectFileSlot>('FILE_1')
const correctDescription = ref('')
const incorrectDescription = ref('')
const selectedFile1 = ref<File | null>(null)
const selectedFile2 = ref<File | null>(null)
const selectedFile1Preview = ref('')
const selectedFile2Preview = ref('')
const validationError = ref('')
const file1Input = ref<HTMLInputElement | null>(null)
const file2Input = ref<HTMLInputElement | null>(null)

const difficultyOptions: TaskDifficulty[] = ['EASY', 'MEDIUM', 'HARD']
const correctFileOptions: CorrectFileSlot[] = ['FILE_1', 'FILE_2']

watch(
  () => [props.task?.id, props.resetToken],
  () => {
    resetForm()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  revokePreview('FILE_1')
  revokePreview('FILE_2')
})

function revokePreview(slot: CorrectFileSlot) {
  if (slot === 'FILE_1' && selectedFile1Preview.value) {
    URL.revokeObjectURL(selectedFile1Preview.value)
    selectedFile1Preview.value = ''
  }

  if (slot === 'FILE_2' && selectedFile2Preview.value) {
    URL.revokeObjectURL(selectedFile2Preview.value)
    selectedFile2Preview.value = ''
  }
}

function resetForm() {
  revokePreview('FILE_1')
  revokePreview('FILE_2')
  validationError.value = ''
  selectedFile1.value = null
  selectedFile2.value = null

  if (file1Input.value) {
    file1Input.value.value = ''
  }

  if (file2Input.value) {
    file2Input.value.value = ''
  }

  difficulty.value = props.lockedDifficulty ?? props.task?.difficulty ?? 'EASY'
  taskDescription.value = props.parsedTask?.taskDescription ?? ''
  correctFileSlot.value = getCorrectFileSlot(props.parsedTask ?? {}) ?? 'FILE_1'
  correctDescription.value = props.parsedTask?.correctDescription ?? ''
  incorrectDescription.value = props.parsedTask?.incorrectDescription ?? ''
}

function updateSelectedFile(slot: CorrectFileSlot, event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0] ?? null

  revokePreview(slot)

  if (slot === 'FILE_1') {
    selectedFile1.value = file
    if (file) {
      selectedFile1Preview.value = URL.createObjectURL(file)
    }
    return
  }

  selectedFile2.value = file
  if (file) {
    selectedFile2Preview.value = URL.createObjectURL(file)
  }
}

function submitForm() {
  validationError.value = ''

  const normalizedTaskDescription = taskDescription.value.trim()
  const normalizedCorrectDescription = correctDescription.value.trim()
  const normalizedIncorrectDescription = incorrectDescription.value.trim()

  if (!normalizedTaskDescription) {
    validationError.value = 'Skriv en oppgavebeskrivelse.'
    return
  }

  if (!normalizedCorrectDescription || !normalizedIncorrectDescription) {
    validationError.value = 'Fyll inn tilbakemelding for både riktig og feil svar.'
    return
  }

  if (!props.task && (!selectedFile1.value || !selectedFile2.value)) {
    validationError.value = 'Velg begge bildene for å opprette oppgaven.'
    return
  }

  emit('submit', {
    difficulty: difficulty.value,
    taskDescription: normalizedTaskDescription,
    correctFileSlot: correctFileSlot.value,
    correctDescription: normalizedCorrectDescription,
    incorrectDescription: normalizedIncorrectDescription,
    file1: selectedFile1.value,
    file2: selectedFile2.value,
  })
}
</script>

<template>
  <section class="task-form-card">
    <div class="form-header-row">
      <div>
        <h2>{{ title ?? (task ? 'Rediger Nyhetskvartalet-oppgave' : 'Opprett Nyhetskvartalet-oppgave') }}</h2>
      </div>
    </div>

    <div class="form-layout">
      <label v-if="!hideDifficulty" class="form-field">
        <span>Vanskelighetsgrad</span>
        <select v-model="difficulty" :disabled="submitting">
          <option v-for="option in difficultyOptions" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>

      <label class="form-field">
        <span>Beskrivelse</span>
        <textarea placeholder="Skriv beskrivelse her..." v-model="taskDescription" rows="3" :disabled="submitting" />
      </label>

      <label class="form-field">
        <span>Riktig fil</span>
        <select v-model="correctFileSlot" :disabled="submitting">
          <option v-for="option in correctFileOptions" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>

      <label class="form-field">
        <span>Tilbakemelding ved riktig svar</span>
        <textarea placeholder="Skriv tilbakemelding ved riktig svar her..." v-model="correctDescription" rows="3" :disabled="submitting" />
      </label>

      <label class="form-field">
        <span>Tilbakemelding ved feil svar</span>
        <textarea v-model="incorrectDescription" placeholder="Skriv tilbakemelding ved feil svar her..." rows="3" :disabled="submitting"></textarea>
      </label>

      <label class="form-field">
        <span>{{ task ? 'Bytt fil 1 (valgfritt)' : 'Fil 1' }}</span>
        <input ref="file1Input" type="file" accept="image/*" :disabled="submitting" @change="updateSelectedFile('FILE_1', $event)" />
      </label>

      <label class="form-field">
        <span>{{ task ? 'Bytt fil 2 (valgfritt)' : 'Fil 2' }}</span>
        <input ref="file2Input" type="file" accept="image/*" :disabled="submitting" @change="updateSelectedFile('FILE_2', $event)" />
      </label>

      <div class="preview-grid">
        <div v-if="selectedFile1Preview || parsedTask?.fileId_1" class="preview-panel">
          <strong>Fil 1</strong>
          <img
            v-if="selectedFile1Preview"
            :src="selectedFile1Preview"
            alt="Fil 1"
            class="preview-image"
          />
          <AuthenticatedImage
            v-else-if="parsedTask?.fileId_1"
            :src="`/api/objects/${parsedTask.fileId_1}`"
            alt="Fil 1"
            class="preview-image"
            preview-on-click
          />
        </div>

        <div v-if="selectedFile2Preview || parsedTask?.fileId_2" class="preview-panel">
          <strong>Fil 2</strong>
          <img
            v-if="selectedFile2Preview"
            :src="selectedFile2Preview"
            alt="Fil 2"
            class="preview-image"
          />
          <AuthenticatedImage
            v-else-if="parsedTask?.fileId_2"
            :src="`/api/objects/${parsedTask.fileId_2}`"
            alt="Fil 2"
            class="preview-image"
            preview-on-click
          />
        </div>
      </div>
    </div>

    <div class="form-actions">
      <button type="button" class="button-variant-2 submit-button" :disabled="submitting" @click="submitForm">
        {{ submitting ? 'Lagrer...' : task ? 'Oppdater oppgave' : 'Opprett oppgave' }}
      </button>
    </div>
    <p v-if="validationError" class="form-message error">{{ validationError }}</p>
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
  background: var(--background);
  padding: 0.85rem 1rem;
  font: inherit;
  color: var(--brown-dark);
}

.form-field textarea {
  resize: vertical;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.preview-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 1rem;
  border: 2px solid rgba(90, 64, 47, 0.18);
}

.form-message.error {
  color: #9f1d1d;
}

@media (max-width: 760px) {
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
