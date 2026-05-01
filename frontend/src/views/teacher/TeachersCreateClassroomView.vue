<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import TeacherHelpButton from '@/components/teacher/TeacherHelpButton.vue'
import { post } from '@/utils/apiUtils'
import type { CreateClassroomRequest, CreateClassroomResponse } from '@/types/classroom'

const router = useRouter()

const form = ref({
  name: '',
  description: '',
})

const loading = ref(false)
const error = ref('')
const successMessage = ref('')

const createClassroomHelpSections = [
  {
    body: [
      'Her oppretter du et nytt klasserom. Gi klasserommet et tydelig navn, for eksempel "5A". Beskrivelsen er kun for lærere og kan brukes til korte notater om klassen. Etter oppretting får klasserommet en romkode som elever bruker for å be om å bli med.',
    ],
  },
]

const handleSubmit = async () => {
  error.value = ''
  successMessage.value = ''

  if (!form.value.name.trim()) {
    error.value = 'Klasserommets navn er påkrevd'
    return
  }

  if (!form.value.description.trim()) {
    error.value = 'Beskrivelse er påkrevd'
    return
  }

  loading.value = true

  try {
    const request: CreateClassroomRequest = {
      name: form.value.name,
      description: form.value.description,
    }

    const response = await post<CreateClassroomResponse>('/api/classrooms', request)

    successMessage.value = `Klasserom "${response.classroomName}" opprettet! Klasserommets kode: ${response.classroomCode}`

    form.value.name = ''
    form.value.description = ''

    setTimeout(() => {
      router.push(`/teacher/classrooms`)
    }, 2000)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Feil ved oppretting av klasserom'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="teacher-content form-page-container">
    <div class="form-card">
      <div class="form-card-header">
        <h2 class="form-title">Opprett nytt klasserom</h2>
        <TeacherHelpButton title="Nytt klasserom" :sections="createClassroomHelpSections" />
      </div>

      <div v-if="error" class="message error" role="alert">
        {{ error }}
      </div>

      <div v-if="successMessage" class="message success" role="status">
        {{ successMessage }}
      </div>

      <form v-if="!successMessage" @submit.prevent="handleSubmit" class="form-wrapper">
        <div class="form-field">
          <label for="classroom-name">Klasserommets navn</label>
          <input
            id="classroom-name"
            v-model="form.name"
            type="text"
            placeholder="Tulleberg 5A"
            :disabled="loading"
          />
        </div>

        <div class="form-field">
          <label for="classroom-description">Beskrivelse</label>
          <textarea
            id="classroom-description"
            v-model="form.description"
            placeholder="Beskrivelse av klasserommet..."
            rows="4"
            :disabled="loading"
          ></textarea>
        </div>

        <button type="submit" class="button-variant-1 form-button create-classroom" :disabled="loading">
          {{ loading ? 'Oppretter...' : 'Opprett klasserom' }}
        </button>
      </form>
    </div>
  </div>
</template>
<style scoped>
.form-card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--medium-gap);
    margin-bottom: var(--thick-padding);
}

.form-card-header .form-title {
    margin: 0;
    text-align: left;
}

.create-classroom:focus {
    color: var(--txt-sec);
    background-color: var(--accent);
}
</style>
