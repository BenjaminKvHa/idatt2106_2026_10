<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import TeacherFotografenTaskForm from '@/components/teacher/mystery/TeacherFotografenTaskForm.vue'
import TeacherMysteryTaskCard from '@/components/teacher/mystery/TeacherMysteryTaskCard.vue'
import TeacherNyhetskvartaletTaskForm from '@/components/teacher/mystery/TeacherNyhetskvartaletTaskForm.vue'
import {
  CHAPTER_ID_BY_TASK_KEY,
  CHAPTER_LABELS,
  DATACENTER_FORMAT_LABELS,
  parseDatacenterTask,
  parseImageVerificationTask,
  parseNewsVerificationTask,
  type ChapterTaskKey,
  type DatacenterTaskFormat,
  type FotografenTaskFormValues,
  type ImageVerificationTask,
  type NewsVerificationTask,
  type NyhetskvartaletTaskFormValues,
  type TaskAdminResponse,
} from '@/components/teacher/mystery/types'
import { get, patchForm, postForm } from '@/utils/apiUtils'

type VisibleTaskCard = {
  task: TaskAdminResponse
  imageTask: ImageVerificationTask | null
  newsTask: NewsVerificationTask | null
  format: DatacenterTaskFormat | null
}

const route = useRoute()

const activeChapter = ref<ChapterTaskKey>('nyhetskvartalet')
const datacenterCreateFormat = ref<DatacenterTaskFormat>('NEWS_VERIFICATION')
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const actionSuccess = ref('')
const submitting = ref(false)
const tasksByChapter = ref<Record<ChapterTaskKey, TaskAdminResponse[]>>({
  nyhetskvartalet: [],
  fotografen: [],
  datasenteret: [],
})
const formResetTokenByChapter = ref<Record<ChapterTaskKey, number>>({
  nyhetskvartalet: 0,
  fotografen: 0,
  datasenteret: 0,
})
const selectedTaskIdByChapter = ref<Record<ChapterTaskKey, number | null>>({
  nyhetskvartalet: null,
  fotografen: null,
  datasenteret: null,
})

const classroomCode = computed(() =>
  typeof route.params.classroomCode === 'string' ? route.params.classroomCode : '',
)

const chapterOrder: ChapterTaskKey[] = ['nyhetskvartalet', 'fotografen', 'datasenteret']
const datacenterFormatOrder: DatacenterTaskFormat[] = ['NEWS_VERIFICATION', 'IMAGE_VERIFICATION']

const visibleTaskCards = computed<VisibleTaskCard[]>(() => {
  if (activeChapter.value === 'fotografen') {
    return tasksByChapter.value.fotografen.map((task) => ({
      task,
      imageTask: parseImageVerificationTask(task.taskJson),
      newsTask: null,
      format: null,
    }))
  }

  if (activeChapter.value === 'nyhetskvartalet') {
    return tasksByChapter.value.nyhetskvartalet.map((task) => ({
      task,
      imageTask: null,
      newsTask: parseNewsVerificationTask(task.taskJson),
      format: null,
    }))
  }

  return tasksByChapter.value.datasenteret.map((task) => {
    const parsedTask = parseDatacenterTask(task.taskJson)

    return {
      task,
      imageTask: parsedTask?.imageTask ?? null,
      newsTask: parsedTask?.newsTask ?? null,
      format: parsedTask?.format ?? null,
    }
  })
})

const selectedFotografenTask = computed(() => {
  const taskId = selectedTaskIdByChapter.value.fotografen
  if (taskId === null) {
    return null
  }

  const task = tasksByChapter.value.fotografen.find((entry) => entry.id === taskId)
  if (!task) {
    return null
  }

  return {
    task,
    parsedTask: parseImageVerificationTask(task.taskJson),
  }
})

const selectedNyhetsTask = computed(() => {
  const taskId = selectedTaskIdByChapter.value.nyhetskvartalet
  if (taskId === null) {
    return null
  }

  const task = tasksByChapter.value.nyhetskvartalet.find((entry) => entry.id === taskId)
  if (!task) {
    return null
  }

  return {
    task,
    parsedTask: parseNewsVerificationTask(task.taskJson),
  }
})

const selectedDatacenterTask = computed(() => {
  const taskId = selectedTaskIdByChapter.value.datasenteret
  if (taskId === null) {
    return null
  }

  const task = tasksByChapter.value.datasenteret.find((entry) => entry.id === taskId)
  if (!task) {
    return null
  }

  const parsedTask = parseDatacenterTask(task.taskJson)
  if (!parsedTask?.format) {
    return null
  }

  return {
    task,
    parsedTask,
    format: parsedTask.format,
    imageTask: parsedTask.imageTask ?? null,
    newsTask: parsedTask.newsTask ?? null,
  }
})

const activeDatacenterFormat = computed<DatacenterTaskFormat>(() => {
  return selectedDatacenterTask.value?.format ?? datacenterCreateFormat.value
})

function clearMessages() {
  actionError.value = ''
  actionSuccess.value = ''
}

function buildMetadataPart(metadata: Record<string, unknown>): Blob {
  return new Blob([JSON.stringify(metadata)], {
    type: 'application/json',
  })
}

function setActiveChapter(chapter: ChapterTaskKey) {
  activeChapter.value = chapter
  clearMessages()
}

function setDatacenterCreateFormat(format: DatacenterTaskFormat) {
  datacenterCreateFormat.value = format
  clearMessages()
}

function toggleTaskEditor(taskId: number) {
  clearMessages()

  const currentTaskId = selectedTaskIdByChapter.value[activeChapter.value]
  selectedTaskIdByChapter.value[activeChapter.value] = currentTaskId === taskId ? null : taskId
}

function clearTaskEditor(chapter: ChapterTaskKey) {
  selectedTaskIdByChapter.value[chapter] = null
  clearMessages()
}

async function loadTasks() {
  loading.value = true
  error.value = ''

  try {
    if (!classroomCode.value) {
      throw new Error('Could not find classroom for this route')
    }

    const encodedClassroomCode = encodeURIComponent(classroomCode.value)
    const [nyhetskvartaletTasks, fotografenTasks, datasenteretTasks] = await Promise.all([
      get<TaskAdminResponse[]>(`/api/admin/tasks/nyhetskvartalet?classroomCode=${encodedClassroomCode}`),
      get<TaskAdminResponse[]>(`/api/admin/tasks/fotografen?classroomCode=${encodedClassroomCode}`),
      get<TaskAdminResponse[]>(`/api/admin/tasks/datasenteret?classroomCode=${encodedClassroomCode}`),
    ])

    tasksByChapter.value = {
      nyhetskvartalet: nyhetskvartaletTasks,
      fotografen: fotografenTasks,
      datasenteret: datasenteretTasks,
    }
  } catch (fetchError) {
    tasksByChapter.value = {
      nyhetskvartalet: [],
      fotografen: [],
      datasenteret: [],
    }
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load tasks'
  } finally {
    loading.value = false
  }
}

async function submitFotografenTask(values: FotografenTaskFormValues) {
  submitting.value = true
  clearMessages()

  try {
    const formData = new FormData()
    const editingTask = selectedFotografenTask.value?.task ?? null

    formData.append(
      'metadata',
      buildMetadataPart(
        editingTask
          ? {
              difficulty: values.difficulty,
              taskDescription: values.taskDescription,
              correctCategory: values.correctCategory,
              correctJustification: values.correctJustification,
              justifications: values.justifications,
              correctDescription: values.correctDescription,
              falseDescription: values.falseDescription,
            }
          : {
              chapterId: CHAPTER_ID_BY_TASK_KEY.fotografen,
              difficulty: values.difficulty,
              taskDescription: values.taskDescription,
              correctCategory: values.correctCategory,
              correctJustification: values.correctJustification,
              justifications: values.justifications,
              correctDescription: values.correctDescription,
              falseDescription: values.falseDescription,
            },
      ),
    )

    if (values.file) {
      formData.append('file', values.file)
    }

    const response = editingTask
      ? await patchForm<TaskAdminResponse>(`/api/admin/tasks/fotografen/${editingTask.id}`, formData)
      : await postForm<TaskAdminResponse>('/api/admin/tasks/fotografen', formData)

    selectedTaskIdByChapter.value.fotografen = null
    if (!editingTask) {
      formResetTokenByChapter.value.fotografen += 1
    }
    actionSuccess.value = editingTask
      ? `Fotografen-oppgave #${response.id} ble oppdatert.`
      : `Fotografen-oppgave #${response.id} ble opprettet.`

    await loadTasks()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to save task'
  } finally {
    submitting.value = false
  }
}

async function submitNyhetsTask(values: NyhetskvartaletTaskFormValues) {
  submitting.value = true
  clearMessages()

  try {
    const formData = new FormData()
    const editingTask = selectedNyhetsTask.value?.task ?? null

    formData.append(
      'metadata',
      buildMetadataPart(
        editingTask
          ? {
              difficulty: values.difficulty,
              taskDescription: values.taskDescription,
              correctFileSlot: values.correctFileSlot,
              correctDescription: values.correctDescription,
              incorrectDescription: values.incorrectDescription,
            }
          : {
              chapterId: CHAPTER_ID_BY_TASK_KEY.nyhetskvartalet,
              difficulty: values.difficulty,
              taskDescription: values.taskDescription,
              correctFileSlot: values.correctFileSlot,
              correctDescription: values.correctDescription,
              incorrectDescription: values.incorrectDescription,
            },
      ),
    )

    if (values.file1) {
      formData.append('file1', values.file1)
    }

    if (values.file2) {
      formData.append('file2', values.file2)
    }

    const response = editingTask
      ? await patchForm<TaskAdminResponse>(`/api/admin/tasks/nyhetskvartalet/${editingTask.id}`, formData)
      : await postForm<TaskAdminResponse>('/api/admin/tasks/nyhetskvartalet', formData)

    selectedTaskIdByChapter.value.nyhetskvartalet = null
    if (!editingTask) {
      formResetTokenByChapter.value.nyhetskvartalet += 1
    }
    actionSuccess.value = editingTask
      ? `Nyhetskvartalet-oppgave #${response.id} ble oppdatert.`
      : `Nyhetskvartalet-oppgave #${response.id} ble opprettet.`

    await loadTasks()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to save task'
  } finally {
    submitting.value = false
  }
}

async function submitDatacenterImageTask(values: FotografenTaskFormValues) {
  submitting.value = true
  clearMessages()

  try {
    const formData = new FormData()
    const editingTask = selectedDatacenterTask.value?.task ?? null

    formData.append(
      'metadata',
      buildMetadataPart(
        editingTask
          ? {
              difficulty: values.difficulty,
              taskDescription: values.taskDescription,
              correctCategory: values.correctCategory,
              correctJustification: values.correctJustification,
              justifications: values.justifications,
              correctDescription: values.correctDescription,
              falseDescription: values.falseDescription,
            }
          : {
              taskDescription: values.taskDescription,
              correctCategory: values.correctCategory,
              correctJustification: values.correctJustification,
              justifications: values.justifications,
              correctDescription: values.correctDescription,
              falseDescription: values.falseDescription,
            },
      ),
    )

    if (values.file) {
      formData.append('file', values.file)
    }

    const response = editingTask
      ? await patchForm<TaskAdminResponse>(`/api/admin/tasks/datasenteret/image/${editingTask.id}`, formData)
      : await postForm<TaskAdminResponse>('/api/admin/tasks/datasenteret/image', formData)

    selectedTaskIdByChapter.value.datasenteret = null
    if (!editingTask) {
      formResetTokenByChapter.value.datasenteret += 1
    }
    actionSuccess.value = editingTask
      ? `Datasenteret fotografen-oppgave #${response.id} ble oppdatert.`
      : `Datasenteret fotografen-oppgave #${response.id} ble opprettet.`

    await loadTasks()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to save task'
  } finally {
    submitting.value = false
  }
}

async function submitDatacenterNewsTask(values: NyhetskvartaletTaskFormValues) {
  submitting.value = true
  clearMessages()

  try {
    const formData = new FormData()
    const editingTask = selectedDatacenterTask.value?.task ?? null

    formData.append(
      'metadata',
      buildMetadataPart(
        editingTask
          ? {
              difficulty: values.difficulty,
              taskDescription: values.taskDescription,
              correctFileSlot: values.correctFileSlot,
              correctDescription: values.correctDescription,
              incorrectDescription: values.incorrectDescription,
            }
          : {
              taskDescription: values.taskDescription,
              correctFileSlot: values.correctFileSlot,
              correctDescription: values.correctDescription,
              incorrectDescription: values.incorrectDescription,
            },
      ),
    )

    if (values.file1) {
      formData.append('file1', values.file1)
    }

    if (values.file2) {
      formData.append('file2', values.file2)
    }

    const response = editingTask
      ? await patchForm<TaskAdminResponse>(`/api/admin/tasks/datasenteret/news/${editingTask.id}`, formData)
      : await postForm<TaskAdminResponse>('/api/admin/tasks/datasenteret/news', formData)

    selectedTaskIdByChapter.value.datasenteret = null
    if (!editingTask) {
      formResetTokenByChapter.value.datasenteret += 1
    }
    actionSuccess.value = editingTask
      ? `Datasenteret nyhetsoppgave #${response.id} ble oppdatert.`
      : `Datasenteret nyhetsoppgave #${response.id} ble opprettet.`

    await loadTasks()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to save task'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadTasks()
})

watch(
  () => route.params.classroomCode,
  async () => {
    selectedTaskIdByChapter.value = {
      nyhetskvartalet: null,
      fotografen: null,
      datasenteret: null,
    }
    formResetTokenByChapter.value = {
      nyhetskvartalet: 0,
      fotografen: 0,
      datasenteret: 0,
    }
    datacenterCreateFormat.value = 'NEWS_VERIFICATION'
    clearMessages()
    await loadTasks()
  },
)
</script>

<template>
  <div class="teacher-container-box edit-tasks-view">
    <section class="page-header">
      <div>
        <h1>Rediger oppgaver</h1>
        <p class="header-copy">Opprett, rediger og kvalitetssikre oppgaver for Nyhetskvartalet, Fotografen og Datasenteret.</p>
      </div>
    </section>

    <section class="task-admin-section">
      <div class="task-admin-header">
        <div>
          <h2>Kapitteloppgaver</h2>
          <p>Velg kapittel, opprett nye oppgaver, eller rediger eksisterende oppgaver direkte her.</p>
        </div>

        <div class="chapter-tabs">
          <button
            v-for="chapter in chapterOrder"
            :key="chapter"
            type="button"
            :class="['chapter-tab', activeChapter === chapter ? 'button-variant-2' : 'button-variant-1']"
            @click="setActiveChapter(chapter)"
          >
            {{ CHAPTER_LABELS[chapter] }}
          </button>
        </div>
      </div>

      <hr class="hr-brown" />

      <p v-if="actionSuccess" class="status-banner success">{{ actionSuccess }}</p>
      <p v-if="actionError" class="status-banner error">{{ actionError }}</p>

      <div class="task-admin-layout">
        <div class="task-form-panel">
          <TeacherNyhetskvartaletTaskForm
            v-if="activeChapter === 'nyhetskvartalet'"
            :task="selectedNyhetsTask?.task ?? null"
            :parsed-task="selectedNyhetsTask?.parsedTask ?? null"
            :submitting="submitting"
            :reset-token="formResetTokenByChapter.nyhetskvartalet"
            @submit="submitNyhetsTask"
            @cancel-edit="clearTaskEditor('nyhetskvartalet')"
          />

          <TeacherFotografenTaskForm
            v-else-if="activeChapter === 'fotografen'"
            :task="selectedFotografenTask?.task ?? null"
            :parsed-task="selectedFotografenTask?.parsedTask ?? null"
            :submitting="submitting"
            :reset-token="formResetTokenByChapter.fotografen"
            @submit="submitFotografenTask"
            @cancel-edit="clearTaskEditor('fotografen')"
          />

          <div v-else class="datacenter-form-shell">
            <div v-if="!selectedDatacenterTask" class="datacenter-format-tabs">
              <button
                v-for="format in datacenterFormatOrder"
                :key="format"
                type="button"
                :class="['chapter-tab', activeDatacenterFormat === format ? 'button-variant-2' : 'button-variant-1']"
                @click="setDatacenterCreateFormat(format)"
              >
                {{ DATACENTER_FORMAT_LABELS[format] }}
              </button>
            </div>

            <p v-else class="datacenter-format-note">
              Redigerer {{ DATACENTER_FORMAT_LABELS[activeDatacenterFormat] }} i Datasenteret.
            </p>

            <TeacherNyhetskvartaletTaskForm
              v-if="activeDatacenterFormat === 'NEWS_VERIFICATION'"
              :task="selectedDatacenterTask?.task ?? null"
              :parsed-task="selectedDatacenterTask?.newsTask ?? null"
              :submitting="submitting"
              :reset-token="formResetTokenByChapter.datasenteret"
              :title="selectedDatacenterTask ? 'Redigerer datasenteret nyhetsoppgave' : 'Datasenteret nyhetsoppgave'"
              :locked-difficulty="'HARD'"
              hide-difficulty
              @submit="submitDatacenterNewsTask"
              @cancel-edit="clearTaskEditor('datasenteret')"
            />

            <TeacherFotografenTaskForm
              v-else
              :task="selectedDatacenterTask?.task ?? null"
              :parsed-task="selectedDatacenterTask?.imageTask ?? null"
              :submitting="submitting"
              :reset-token="formResetTokenByChapter.datasenteret"
              :title="selectedDatacenterTask ? 'Redigerer datasenteret fotografen-oppgave' : 'Datasenteret fotografen-oppgave'"
              :locked-difficulty="'HARD'"
              hide-difficulty
              @submit="submitDatacenterImageTask"
              @cancel-edit="clearTaskEditor('datasenteret')"
            />
          </div>
        </div>

        <div class="task-list-panel">
          <div class="list-header-row">
            <div>
              <h3>Eksisterende oppgaver</h3>
            </div>
          </div>

          <p v-if="loading" class="state-message">Laster oppgaver...</p>
          <p v-else-if="error" class="state-message error">{{ error }}</p>
          <div v-else-if="visibleTaskCards.length === 0" class="empty-state">
            Ingen oppgaver funnet for {{ CHAPTER_LABELS[activeChapter] }}.
          </div>
          <div v-else class="task-list">
            <TeacherMysteryTaskCard
              v-for="card in visibleTaskCards"
              :key="card.task.id"
              :chapter="activeChapter"
              :task="card.task"
              :image-task="card.imageTask"
              :news-task="card.newsTask"
              :format="card.format"
              :selected="selectedTaskIdByChapter[activeChapter] === card.task.id"
              @edit="toggleTaskEditor"
            />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.edit-tasks-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: min(78rem, 100%);
}

.page-header,
.task-admin-header,
.list-header-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: flex-start;
}

.page-header h1,
.page-header p,
.task-admin-header h2,
.task-admin-header p,
.list-header-row h3,
.list-header-row p,
.status-banner,
.state-message,
.empty-state,
.datacenter-format-note {
  margin: 0;
}

.header-copy {
  max-width: 42rem;
}

.chapter-tab {
  padding: 0.8rem 1rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.task-admin-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.chapter-tabs,
.datacenter-format-tabs {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.task-admin-layout {
  display: grid;
  grid-template-columns: minmax(0, 29rem) minmax(0, 1fr);
  gap: 1rem;
  align-items: start;
}

.task-form-panel,
.task-list-panel,
.datacenter-form-shell {
  min-width: 0;
}

.datacenter-form-shell,
.task-list-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.datacenter-format-note {
  color: var(--brown-dark);
  font-weight: 700;
}

.task-list {
  display: grid;
  gap: 1rem;
}

.status-banner {
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  background: rgba(255, 248, 235, 0.85);
  color: var(--brown-dark);
}

.status-banner.success {
  border: 2px solid rgba(40, 135, 71, 0.2);
  color: #155724;
}

.status-banner.error,
.state-message.error {
  color: #9f1d1d;
}

.state-message,
.empty-state {
  color: var(--brown-dark);
}

@media (max-width: 1100px) {
  .task-admin-layout {
    grid-template-columns: 1fr;
  }
}
</style>
