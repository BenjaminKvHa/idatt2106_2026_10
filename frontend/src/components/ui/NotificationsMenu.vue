<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { get } from '@/utils/apiUtils'

type TeacherClassroomOverview = {
  classroomCode: string
  classroomName: string
  totalScore: number
  globalRank: number
}

type TeacherNotification = {
  id: number
  classroomCode: string
  pupilLegalName: string
  text: string
  createdAt: string
  statusNotifications: 'UNREAD' | 'READ'
}

const props = defineProps<{
  classroomCode: string | null
}>()

const emit = defineEmits<{ (e: 'close'): void }>()

const classrooms = ref<TeacherClassroomOverview[]>([])
const selectedClassroomCode = ref('')
const notifications = ref<TeacherNotification[]>([])
const classroomsLoading = ref(false)
const loading = ref(false)
const classroomsError = ref('')
const error = ref('')

let pollingHandle: number | null = null
let disposed = false
let requestInFlight = false

function close() {
  emit('close')
}

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

async function loadClassrooms() {
  classroomsLoading.value = true
  classroomsError.value = ''

  try {
    const response = await get<TeacherClassroomOverview[]>('/api/teacher/classrooms')
    if (disposed) {
      return
    }

    classrooms.value = response

    const matchingInitialClassroom = props.classroomCode && response.some((classroom) => classroom.classroomCode === props.classroomCode)
      ? props.classroomCode
      : null

    selectedClassroomCode.value = matchingInitialClassroom ?? response[0]?.classroomCode ?? ''
  } catch (fetchError) {
    if (disposed) {
      return
    }

    classrooms.value = []
    selectedClassroomCode.value = ''
    classroomsError.value = fetchError instanceof Error ? fetchError.message : 'Kunne ikke hente klasserom'
  } finally {
    if (!disposed) {
      classroomsLoading.value = false
    }
  }
}

async function loadNotifications(backgroundRefresh = false) {
  if (requestInFlight) {
    return
  }

  if (!selectedClassroomCode.value) {
    notifications.value = []
    error.value = 'Velg et klasserom for å se varsler.'
    loading.value = false
    return
  }

  requestInFlight = true
  if (!backgroundRefresh) {
    loading.value = true
    error.value = ''
  }

  try {
    const response = await get<TeacherNotification[]>(`/api/teacher/classrooms/${selectedClassroomCode.value}/notifications`)
    if (disposed) {
      return
    }

    notifications.value = response
  } catch (fetchError) {
    if (disposed) {
      return
    }

    if (!backgroundRefresh || notifications.value.length === 0) {
      notifications.value = []
      error.value = fetchError instanceof Error ? fetchError.message : 'Kunne ikke hente varsler'
    }
  } finally {
    requestInFlight = false
    if (disposed) {
      return
    }

    if (!backgroundRefresh) {
      loading.value = false
    }
  }
}

function stopPolling() {
  if (pollingHandle !== null) {
    window.clearInterval(pollingHandle)
    pollingHandle = null
  }
}

function startPolling() {
  stopPolling()
  if (!selectedClassroomCode.value) {
    return
  }

  pollingHandle = window.setInterval(async () => {
    await loadNotifications(true)
  }, 10000)
}

onMounted(async () => {
  disposed = false
  await loadClassrooms()
  await loadNotifications()
  startPolling()
})

watch(
  () => props.classroomCode,
  async () => {
    if (props.classroomCode && classrooms.value.some((classroom) => classroom.classroomCode === props.classroomCode)) {
      selectedClassroomCode.value = props.classroomCode
    }
  },
)

watch(
  () => selectedClassroomCode.value,
  async () => {
    await loadNotifications()
    startPolling()
  },
)

onBeforeUnmount(() => {
  disposed = true
  stopPolling()
})
</script>

<template>
  <BaseModal panel-class="teacher-variant-1 notifications-modal-box" @close="close">
    <h2>Varsler</h2>
    <div class="classroom-picker">
      <label for="notifications-classroom">Klasserom</label>
      <select
        id="notifications-classroom"
        v-model="selectedClassroomCode"
        :disabled="classroomsLoading || classrooms.length === 0"
      >
        <option v-if="classrooms.length === 0" value="">Ingen klasserom</option>
        <option v-for="classroom in classrooms" :key="classroom.classroomCode" :value="classroom.classroomCode">
          {{ classroom.classroomName }} ({{ classroom.classroomCode }})
        </option>
      </select>
    </div>

    <p v-if="classroomsError" class="state-message error">{{ classroomsError }}</p>
    <p v-else-if="classroomsLoading" class="state-message">Laster klasserom...</p>
    <p v-else-if="loading" class="state-message">Laster varsler...</p>
    <p v-else-if="error" class="state-message error">{{ error }}</p>

    <div v-else-if="notifications.length === 0" class="state-message">
      Ingen varsler ennå.
    </div>

    <ul v-else class="notifications-list">
      <li v-for="notification in notifications" :key="notification.id" class="notification-card">
        <div class="notification-top-row">
          <span class="notification-name">{{ notification.pupilLegalName }}</span>
          <span class="notification-status" :class="notification.statusNotifications.toLowerCase()">
            {{ notification.statusNotifications === 'UNREAD' ? 'Ulest' : 'Lest' }}
          </span>
        </div>
        <p class="notification-text">{{ notification.text }}</p>
        <p class="notification-time">{{ formatTimestamp(notification.createdAt) }}</p>
      </li>
    </ul>
  </BaseModal>
</template>

<style scoped>
:global(.modal-window.notifications-modal-box) {
  width: 90vw;
  max-width: 680px;
  min-width: 320px;
  flex: 0 0 auto;
}


.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.modal-header h2,
.notification-text,
.notification-time,
.state-message {
  margin: 0;
}

.close-button {
  border: 2px solid var(--brown-medium-100);
  border-radius: 999px;
  background: var(--beige-light-100);
  padding: 0.45rem 0.9rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.classroom-picker {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.classroom-picker label {
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--brown-dark-100);
}

.classroom-picker select {
  padding: 0.7rem 0.9rem;
  border-radius: 12px;
  border: 2px solid var(--brown-medium-100);
  background: var(--beige-light-100);
  color: var(--brown-dark-100);
  font: inherit;
}

.notifications-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.notification-card {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  background: var(--beige-light-100);
  border: 2px solid rgba(85, 50, 27, 0.15);
}

.notification-top-row {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

.notification-name {
  font-weight: 700;
}

.notification-status {
  font-size: 0.85rem;
  font-weight: 700;
  border-radius: 999px;
  padding: 0.2rem 0.6rem;
}

.notification-status.unread {
  background: #f7d987;
}

.notification-status.read {
  background: #d6e6c6;
}

.notification-time {
  font-size: 0.85rem;
  opacity: 0.8;
}

.state-message {
  color: var(--brown-dark-100);
}

.state-message.error {
  color: #9f1d1d;
}
</style>
