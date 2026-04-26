<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import { get } from '@/utils/apiUtils'

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

const notifications = ref<TeacherNotification[]>([])
const loading = ref(false)
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

async function loadNotifications(backgroundRefresh = false) {
  if (requestInFlight) {
    return
  }

  if (!props.classroomCode) {
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
    const response = await get<TeacherNotification[]>(`/api/teacher/classrooms/${props.classroomCode}/notifications`)
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
  pollingHandle = window.setInterval(async () => {
    await loadNotifications(true)
  }, 2000)
}

onMounted(async () => {
  disposed = false
  await loadNotifications()
  startPolling()
})

watch(
  () => props.classroomCode,
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
    <div class="modal-header">
      <h2>Varsler</h2>
      <button type="button" class="close-button" @click="close">Lukk</button>
    </div>

    <p v-if="loading" class="state-message">Laster varsler...</p>
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
.notifications-modal-box {
  min-width: 320px;
  max-width: 680px;
  width: 90vw;
  gap: var(--medium-gap);
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
  border: 2px solid var(--accent);
  border-radius: 999px;
  background: var(--background);
  padding: 0.45rem 0.9rem;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
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
  background: var(--background);
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
  color: var(--txt);
}

.state-message.error {
  color: #9f1d1d;
}
</style>
