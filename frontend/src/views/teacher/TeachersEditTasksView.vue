<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { get } from '@/utils/apiUtils'

type ChapterKey = 'Nyhetskvartalet' | 'Fotografen' | 'Datasenteret'

type TaskAdminResponse = {
  id: number
  difficulty: string
  chapterId: number | null
  classroomCode: string | null
  taskJson: string
  createdAt: string
}

type ImageVerificationTask = {
  taskDescription?: string
  fileId?: number
  correctCategory?: string
  correctJustification?: string
  justifications?: string[]
  correctDescription?: string
  falseDescription?: string
}

type NewsVerificationTask = {
  taskDescription?: string
  fileId_1?: number
  fileId_2?: number
  correctFileId?: number
  correctDescription?: string
  incorrectDescription?: string
}

const route = useRoute()

const CHAPTER_ENDPOINTS: Record<ChapterKey, string> = {
  Nyhetskvartalet: 'nyhetskvartalet',
  Fotografen: 'fotografen',
  Datasenteret: 'datasenteret',
}

const chapterNames: ChapterKey[] = ['Nyhetskvartalet', 'Fotografen', 'Datasenteret']

const activeChapter = ref<ChapterKey>('Nyhetskvartalet')
const loading = ref(true)
const error = ref('')
const tasksByChapter = ref<Record<ChapterKey, TaskAdminResponse[]>>({
  Nyhetskvartalet: [],
  Fotografen: [],
  Datasenteret: [],
})

const classroomCode = computed(() =>
  typeof route.params.classroomCode === 'string' ? route.params.classroomCode : '',
)

const visibleTasks = computed(() => tasksByChapter.value[activeChapter.value])

function selectChapter(chapter: ChapterKey) {
  activeChapter.value = chapter
}

function parseTaskJson(taskJson: string): Record<string, unknown> | null {
  try {
    return JSON.parse(taskJson) as Record<string, unknown>
  } catch {
    return null
  }
}

function getImageVerificationTask(taskJson: string): ImageVerificationTask | null {
  return parseTaskJson(taskJson) as ImageVerificationTask | null
}

function getNewsVerificationTask(taskJson: string): NewsVerificationTask | null {
  return parseTaskJson(taskJson) as NewsVerificationTask | null
}

async function loadTasks() {
  loading.value = true
  error.value = ''

  try {
    if (!classroomCode.value) {
      throw new Error('Could not find classroom for this route')
    }

    const [nyhetskvartaletTasks, fotografenTasks, datasenteretTasks] = await Promise.all([
      get<TaskAdminResponse[]>(`/api/admin/tasks/nyhetskvartalet?classroomCode=${classroomCode.value}`),
      get<TaskAdminResponse[]>(`/api/admin/tasks/fotografen?classroomCode=${classroomCode.value}`),
      get<TaskAdminResponse[]>(`/api/admin/tasks/datasenteret?classroomCode=${classroomCode.value}`),
    ])

    tasksByChapter.value = {
      Nyhetskvartalet: nyhetskvartaletTasks,
      Fotografen: fotografenTasks,
      Datasenteret: datasenteretTasks,
    }
  } catch (fetchError) {
    tasksByChapter.value = {
      Nyhetskvartalet: [],
      Fotografen: [],
      Datasenteret: [],
    }
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load tasks'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadTasks()
})

watch(
  () => route.params.classroomCode,
  async () => {
    await loadTasks()
  },
)
</script>

<template>
  <div class="teacher-edit-tasks-page-container">
    <div class="stop-navigation-container">
      <button
        v-for="chapter in chapterNames"
        :key="chapter"
        class="go-to-stop-btn"
        :class="{ 'active-go-to-stop-btn': activeChapter === chapter }"
        type="button"
        @click="selectChapter(chapter)"
      >
        {{ chapter }}
      </button>
    </div>

    <div class="tasks-panel">
      <p v-if="loading" class="state-message">Laster oppgaver...</p>
      <p v-else-if="error" class="state-message error">{{ error }}</p>

      <template v-else>
        <h1>{{ activeChapter }}</h1>

        <div v-if="visibleTasks.length === 0" class="empty-state">
          Ingen oppgaver funnet for dette kapittelet.
        </div>

        <div v-else class="tasks-container">
          <div v-for="task in visibleTasks" :key="task.id" class="task-container">
            <div class="task-meta-row">
              <strong>{{ task.difficulty }}</strong>
              <span>Oppgave #{{ task.id }}</span>
            </div>

            <div class="task-meta-secondary">
              <span>Kapittel-ID: {{ task.chapterId ?? 'Ukjent' }}</span>
              <span>Klasserom: {{ task.classroomCode ?? classroomCode }}</span>
            </div>

            <div v-if="activeChapter === 'Fotografen'" class="task-json-structured">
              <template v-if="getImageVerificationTask(task.taskJson)">
                <p><strong>Oppgavebeskrivelse:</strong> {{ getImageVerificationTask(task.taskJson)?.taskDescription || 'Ingen beskrivelse' }}</p>
                <p><strong>Fil-ID:</strong> {{ getImageVerificationTask(task.taskJson)?.fileId ?? 'Ukjent' }}</p>
                <p><strong>Riktig kategori:</strong> {{ getImageVerificationTask(task.taskJson)?.correctCategory || 'Ukjent' }}</p>
                <p><strong>Riktig begrunnelse:</strong> {{ getImageVerificationTask(task.taskJson)?.correctJustification || 'Ukjent' }}</p>
                <div v-if="getImageVerificationTask(task.taskJson)?.justifications?.length" class="task-list-block">
                  <strong>Begrunnelser:</strong>
                  <ul>
                    <li v-for="justification in getImageVerificationTask(task.taskJson)?.justifications" :key="justification">
                      {{ justification }}
                    </li>
                  </ul>
                </div>
                <p><strong>Ved riktig svar:</strong> {{ getImageVerificationTask(task.taskJson)?.correctDescription || 'Ingen tekst' }}</p>
                <p><strong>Ved feil svar:</strong> {{ getImageVerificationTask(task.taskJson)?.falseDescription || 'Ingen tekst' }}</p>
              </template>
              <pre v-else class="task-json">{{ task.taskJson }}</pre>
            </div>

            <div v-else-if="activeChapter === 'Nyhetskvartalet'" class="task-json-structured">
              <template v-if="getNewsVerificationTask(task.taskJson)">
                <p><strong>Oppgavebeskrivelse:</strong> {{ getNewsVerificationTask(task.taskJson)?.taskDescription || 'Ingen beskrivelse' }}</p>
                <p><strong>Fil-ID 1:</strong> {{ getNewsVerificationTask(task.taskJson)?.fileId_1 ?? 'Ukjent' }}</p>
                <p><strong>Fil-ID 2:</strong> {{ getNewsVerificationTask(task.taskJson)?.fileId_2 ?? 'Ukjent' }}</p>
                <p><strong>Riktig fil-ID:</strong> {{ getNewsVerificationTask(task.taskJson)?.correctFileId ?? 'Ukjent' }}</p>
                <p><strong>Ved riktig svar:</strong> {{ getNewsVerificationTask(task.taskJson)?.correctDescription || 'Ingen tekst' }}</p>
                <p><strong>Ved feil svar:</strong> {{ getNewsVerificationTask(task.taskJson)?.incorrectDescription || 'Ingen tekst' }}</p>
              </template>
              <pre v-else class="task-json">{{ task.taskJson }}</pre>
            </div>

            <pre v-else class="task-json">{{ task.taskJson }}</pre>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.teacher-edit-tasks-page-container {
  display: flex;
  gap: 1rem;
}

.stop-navigation-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.go-to-stop-btn {
  background-color: var(--beige-light);
  border: var(--border-thin) solid var(--brown-dark);
  color: var(--brown-dark);
  padding: 1rem 0.75rem;
  border-radius: 1rem;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
}

.go-to-stop-btn.active-go-to-stop-btn,
.go-to-stop-btn:hover {
  background-color: var(--background-sec);
}

.tasks-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
  background-color: var(--beige-light);
  padding: 1rem;
  border-radius: 1rem;
}

.tasks-panel h1,
.state-message,
.empty-state {
  margin: 0;
}

.tasks-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.task-container {
  background-color: var(--background-sec);
  padding: 1rem;
  border-radius: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.task-meta-row,
.task-meta-secondary {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.task-meta-secondary {
  font-size: 0.9rem;
  opacity: 0.8;
}

.task-json {
  margin: 0;
  padding: 0.75rem;
  border-radius: 0.5rem;
  background: var(--background);
  border: 1px solid rgba(85, 50, 27, 0.2);
  white-space: pre-wrap;
  word-break: break-word;
}

.task-json-structured {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0.75rem;
  border-radius: 0.5rem;
  background: var(--background);
  border: 1px solid rgba(85, 50, 27, 0.2);
}

.task-json-structured p {
  margin: 0;
}

.task-list-block {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.task-list-block ul {
  margin: 0;
  padding-left: 1.25rem;
}

.state-message {
  color: var(--brown-dark);
}

.state-message.error {
  color: #9f1d1d;
}

@media (max-width: 900px) {
  .teacher-edit-tasks-page-container {
    flex-direction: column;
  }

  .stop-navigation-container {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
