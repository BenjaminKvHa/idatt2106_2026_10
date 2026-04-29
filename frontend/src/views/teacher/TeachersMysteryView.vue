<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Mystery from '@/components/teacher/mystery/Mystery.vue'
import { del, get, put } from '@/utils/apiUtils'

type ApprovedWeeklyMystery = {
  id: number
  fileObjectId: number
  pupilEmail?: string
  pupilLegalName?: string
  comment: string
  createdAt: string
}

type PendingWeeklyMystery = {
  id: number
  fileObjectId: number
  pupilEmail: string
  pupilLegalName: string
  comment: string
  createdAt: string
}

const route = useRoute()

const approvedMysteries = ref<ApprovedWeeklyMystery[]>([])
const pendingMysteries = ref<PendingWeeklyMystery[]>([])
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const activeMysteryId = ref<number | null>(null)

const classroomCode = computed(() =>
  typeof route.params.classroomCode === 'string' ? route.params.classroomCode : '',
)

async function loadMysteries() {
  loading.value = true
  error.value = ''

  try {
    if (!classroomCode.value) {
      throw new Error('Could not find classroom for this route')
    }

    const [approved, pending] = await Promise.all([
      get<ApprovedWeeklyMystery[]>(`/api/teacher/classrooms/${classroomCode.value}/weekly-mysteries/approved`),
      get<PendingWeeklyMystery[]>(`/api/teacher/classrooms/${classroomCode.value}/weekly-mysteries/pending`),
    ])

    approvedMysteries.value = approved
    pendingMysteries.value = pending
  } catch (fetchError) {
    approvedMysteries.value = []
    pendingMysteries.value = []
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load mysteries'
  } finally {
    loading.value = false
  }
}

async function acceptMystery(mysteryId: number) {
  if (!classroomCode.value || activeMysteryId.value !== null) return

  activeMysteryId.value = mysteryId
  actionError.value = ''

  try {
    await put(`/api/teacher/classrooms/${classroomCode.value}/weekly-mysteries/${mysteryId}/accept`, {})
    await loadMysteries()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to accept mystery'
  } finally {
    activeMysteryId.value = null
  }
}

async function deleteMystery(mysteryId: number) {
  if (!classroomCode.value || activeMysteryId.value !== null) return

  activeMysteryId.value = mysteryId
  actionError.value = ''

  try {
    await del(`/api/teacher/classrooms/${classroomCode.value}/weekly-mysteries/${mysteryId}`)
    await loadMysteries()
  } catch (requestError) {
    actionError.value = requestError instanceof Error ? requestError.message : 'Failed to delete mystery'
  } finally {
    activeMysteryId.value = null
  }
}

onMounted(async () => {
  await loadMysteries()
})

watch(
  () => route.params.classroomCode,
  async () => {
    await loadMysteries()
  },
)
</script>

<template>
  <div class="teacher-container-box mystery-view">
    <section class="mystery-section">
      <div class="section-header">
        <div>
          <p class="eyebrow">Klasserom {{ classroomCode }}</p>
          <h1>Ukens mysterium</h1>
          <p>Godkjenn eller fjern elevforslag for dette klasserommet.</p>
        </div>
      </div>
    </section>

    <p v-if="loading" class="state-message">Laster mysterier...</p>
    <p v-else-if="error" class="state-message error">{{ error }}</p>
    <p v-if="!loading && !error && actionError" class="state-message error">{{ actionError }}</p>

    <template v-else>
      <section class="mystery-section">
        <h2>Venter på godkjenning</h2>
        <div v-if="pendingMysteries.length === 0" class="empty-state">
          Ingen ventende mysterier akkurat nå.
        </div>
        <div v-else class="mystery-grid">
          <Mystery
            v-for="mystery in pendingMysteries"
            :key="mystery.id"
            :mystery="mystery"
            :busy="activeMysteryId === mystery.id"
            pending
            @accept="acceptMystery"
            @remove="deleteMystery"
          />
        </div>
      </section>

      <section class="mystery-section">
        <h2>Godkjente mysterier</h2>
        <div v-if="approvedMysteries.length === 0" class="empty-state">
          Ingen godkjente mysterier ennå.
        </div>
        <div v-else class="mystery-grid">
          <Mystery
            v-for="mystery in approvedMysteries"
            :key="mystery.id"
            :mystery="mystery"
            :busy="activeMysteryId === mystery.id"
            @remove="deleteMystery"
          />
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.mystery-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.mystery-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.section-header h1,
.section-header h2,
.section-header p,
.mystery-section h2,
.state-message,
.empty-state {
  margin: 0;
}

.eyebrow {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  opacity: 0.74;
  margin: 0 0 0.25rem;
}

.mystery-grid {
  display: grid;
  gap: 1rem;
}

.empty-state,
.state-message {
  color: var(--brown-dark);
}

.state-message.error {
  color: #9f1d1d;
}
</style>
