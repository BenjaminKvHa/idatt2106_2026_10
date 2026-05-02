<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeacherHelpButton from '@/components/teacher/TeacherHelpButton.vue'
import { del, get, post, put } from '@/utils/apiUtils'

type ClassroomDetailsResponse = {
  classroomCode: string
  classroomName: string
  classroomDescription: string | null
}

type TeacherLeaveStatusResponse = {
  canLeave: boolean
}

type ClassroomTeacherResponse = {
  email: string
  legalName: string
}

type InviteTeacherRequest = {
  email: string
}

type UpdateClassroomRequest = {
  name: string
  description: string
}

const route = useRoute()
const router = useRouter()

const classroom = ref<ClassroomDetailsResponse | null>(null)
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const canLeave = ref(false)
const deleting = ref(false)
const leaving = ref(false)
const teachers = ref<ClassroomTeacherResponse[]>([])
const loadingTeachers = ref(false)
const teachersError = ref('')
const inviteTeacherEmail = ref('')
const invitingTeacher = ref(false)
const editOpen = ref(false)
const savingClassroom = ref(false)
const editName = ref('')
const editDescription = ref('')
const leaveClassroomPopupOpen = ref(false)
const deleteClassroomPopupOpen = ref(false)

const editClassroomHelpSections = [
  {
    body: [
      'Her kan du endre navn og beskrivelse for klasserommet. Du kan også legge til andre lærere med e-postadresse.',
      'Bruk "Forlat klasserom" hvis du ikke lenger skal være lærer her, og "Slett klasserom" kun når klasserommet ikke skal brukes mer.',
    ],
  },
]

function syncEditForm() {
  editName.value = classroom.value?.classroomName ?? ''
  editDescription.value = classroom.value?.classroomDescription ?? ''
}

function openEditCard() {
  syncEditForm()
  actionError.value = ''
  editOpen.value = true
}

function closeEditCard() {
  syncEditForm()
  actionError.value = ''
  editOpen.value = false
}

async function loadClassroom() {
  loading.value = true
  error.value = ''
  actionError.value = ''
  teachersError.value = ''

  try {
    const classroomCodeSlug =
      typeof route.params.classroomCode === 'string' ? route.params.classroomCode : ''
    if (!classroomCodeSlug) {
      throw new Error('Could not find classroom slug for this url')
    }

    const response = await get<{
      classroomCode: string
      classroomName: string
      classroomDescription: string | null
    }>(`/api/classrooms/${classroomCodeSlug}/leaderboard`)

    classroom.value = {
      classroomCode: response.classroomCode,
      classroomName: response.classroomName,
      classroomDescription: response.classroomDescription,
    }
    syncEditForm()

    const leaveStatus = await get<TeacherLeaveStatusResponse>(
      `/api/classrooms/${classroomCodeSlug}/teachers/me/can-leave`,
    )
    canLeave.value = leaveStatus.canLeave

    await loadTeachers(classroomCodeSlug)
  } catch (fetchError) {
    classroom.value = null
    canLeave.value = false
    teachers.value = []
    error.value = fetchError instanceof Error ? fetchError.message : 'Failed to load classroom'
  } finally {
    loading.value = false
  }
}

async function saveClassroomDetails() {
  if (!classroom.value || savingClassroom.value) return

  const normalizedName = editName.value.trim()
  if (!normalizedName) {
    actionError.value = 'Klasseromsnavn kan ikke være tomt.'
    return
  }

  savingClassroom.value = true
  actionError.value = ''

  try {
    const payload: UpdateClassroomRequest = {
      name: normalizedName,
      description: editDescription.value.trim(),
    }

    const updated = await put<ClassroomDetailsResponse>(
      `/api/classrooms/${classroom.value.classroomCode}`,
      payload,
    )
    classroom.value = updated
    syncEditForm()
    editOpen.value = false
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to update classroom'
  } finally {
    savingClassroom.value = false
  }
}

async function loadTeachers(classroomCode: string) {
  loadingTeachers.value = true
  teachersError.value = ''

  try {
    teachers.value = await get<ClassroomTeacherResponse[]>(
      `/api/classrooms/${classroomCode}/teachers`,
    )
  } catch (fetchError) {
    teachers.value = []
    teachersError.value =
      fetchError instanceof Error ? fetchError.message : 'Failed to load teachers'
  } finally {
    loadingTeachers.value = false
  }
}

async function inviteTeacher() {
  if (!classroom.value || invitingTeacher.value || inviteTeacherEmail.value.trim().length === 0)
    return

  invitingTeacher.value = true
  actionError.value = ''

  try {
    const payload: InviteTeacherRequest = {
      email: inviteTeacherEmail.value.trim(),
    }

    await post(`/api/classrooms/${classroom.value.classroomCode}/teachers`, payload)
    inviteTeacherEmail.value = ''
    await loadTeachers(classroom.value.classroomCode)
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to invite teacher'
  } finally {
    invitingTeacher.value = false
  }
}

function openDeleteClassroomPopup() {
  if (!classroom.value || deleting.value) return
  actionError.value = ''
  deleteClassroomPopupOpen.value = true
}

function closeDeleteClassroomPopup() {
  if (deleting.value) return
  deleteClassroomPopupOpen.value = false
}

async function deleteClassroom() {
  if (!classroom.value || deleting.value) return

  deleting.value = true
  actionError.value = ''

  try {
    await del(`/api/classrooms/${classroom.value.classroomCode}`)
    deleteClassroomPopupOpen.value = false
    await router.push('/teacher/classrooms')
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to delete classroom'
  } finally {
    deleting.value = false
  }
}

function openLeaveClassroomPopup() {
  if (!classroom.value || leaving.value) return
  actionError.value = ''
  leaveClassroomPopupOpen.value = true
}

function closeLeaveClassroomPopup() {
  if (leaving.value) return
  leaveClassroomPopupOpen.value = false
}

async function leaveClassroom() {
  if (!classroom.value || leaving.value) return

  leaving.value = true
  actionError.value = ''

  try {
    await del(`/api/classrooms/${classroom.value.classroomCode}/teachers/me`)
    leaveClassroomPopupOpen.value = false
    await router.push('/teacher/classrooms')
  } catch (requestError) {
    actionError.value =
      requestError instanceof Error ? requestError.message : 'Failed to leave classroom'
  } finally {
    leaving.value = false
  }
}

onMounted(async () => {
  await loadClassroom()
})

watch(
  () => route.params.classroomCode,
  async () => {
    await loadClassroom()
  },
)
</script>

<template>
  <div class="teacher-container-box edit-classroom-view">
    <p v-if="loading" class="state-message">Laster klasserom...</p>
    <p v-else-if="error" class="state-message error">{{ error }}</p>

    <template v-else-if="classroom">
      <div class="page-header">
        <div>
          <h1>{{ classroom.classroomName }}</h1>
          <p v-if="classroom.classroomDescription">{{ classroom.classroomDescription }}</p>
          <p><b>Romkode:</b> {{ classroom.classroomCode }}</p>
        </div>
        <TeacherHelpButton title="Administrer klasserom" :sections="editClassroomHelpSections" />
      </div>

      <button
        v-if="!editOpen"
        type="button"
        class="action-button edit-classroom-btn"
        @click="openEditCard"
      >
        Rediger klasserom
      </button>

      <section v-if="editOpen" class="edit-card">
        <h2>Rediger klasserom</h2>

        <label class="form-field">
          <span>Klasseromsnavn</span>
          <input v-model="editName" type="text" class="teacher-input" :disabled="savingClassroom" />
        </label>

        <label class="form-field">
          <span>Beskrivelse</span>
          <textarea
            v-model="editDescription"
            rows="3"
            class="teacher-textarea"
            :disabled="savingClassroom"
          />
        </label>

        <div class="edit-actions">
          <button
            type="button"
            class="action-button"
            :disabled="savingClassroom || editName.trim().length === 0"
            @click="saveClassroomDetails"
          >
            {{ savingClassroom ? 'Lagrer...' : 'Lagre endringer' }}
          </button>

          <button
            type="button"
            class="action-button secondary-button"
            :disabled="savingClassroom"
            @click="closeEditCard"
          >
            Avbryt
          </button>
        </div>
      </section>

      <section class="teacher-section">
        <div class="teacher-section-header">
          <h3>Lærere i klasserommet</h3>
        </div>

        <p v-if="loadingTeachers" class="helper-text">Laster lærere...</p>
        <p v-else-if="teachersError" class="helper-text error-text">{{ teachersError }}</p>
        <ul v-else-if="teachers.length > 0" class="teacher-list">
          <li v-for="teacher in teachers" :key="teacher.email" class="teacher-list-item">
            <span class="teacher-name">{{ teacher.legalName }}</span>
            <span class="teacher-email">{{ teacher.email }}</span>
          </li>
        </ul>
        <p v-else class="helper-text">Ingen lærere funnet for dette klasserommet.</p>
      </section>

      <section class="teacher-section">
        <div class="invite-row">
          <input
            v-model="inviteTeacherEmail"
            type="email"
            class="teacher-input"
            placeholder="other.teacher@example.com"
          />
          <button
            type="button"
            class="action-button"
            :disabled="invitingTeacher || inviteTeacherEmail.trim().length === 0"
            @click="inviteTeacher"
          >
            {{ invitingTeacher ? 'Inviterer...' : 'Legg til lærer' }}
          </button>
        </div>
      </section>

      <p v-if="actionError" class="helper-text error-text">
        {{
          (() => {
            let out = actionError
            switch (actionError) {
              case 'Request failed with status 404':
                out = 'Fant ikke lærer'
                break
              case 'Request failed with status 400':
                out = 'Epost feil formatert'
                break
            }
            return out
          })()
        }}
      </p>

      <div class="action-grid">
        <button
          v-if="canLeave"
          type="button"
          class="action-button destructive-button"
          :disabled="leaving"
          @click="openLeaveClassroomPopup"
        >
          {{ leaving ? 'Forlater...' : 'Forlat klasserom' }}
        </button>
        <button
          type="button"
          class="action-button destructive-button"
          :disabled="deleting"
          @click="openDeleteClassroomPopup"
        >
          {{ deleting ? 'Sletter...' : 'Slett klasserom' }}
        </button>
      </div>
    </template>

    <div
      v-if="leaveClassroomPopupOpen && classroom"
      class="teacher-confirm-overlay"
      @click.self="closeLeaveClassroomPopup"
    >
      <section class="teacher-confirm-popup" role="dialog" aria-modal="true">
        <h2>Forlat klasserom?</h2>
        <p>
          Er du sikker på at du vil forlate
          <strong>{{ classroom.classroomName }}</strong
          >?
        </p>

        <div class="teacher-confirm-actions">
          <button
            type="button"
            class="teacher-confirm-button teacher-confirm-button-secondary cancel-leave-classroom"
            :disabled="leaving"
            @click="closeLeaveClassroomPopup"
          >
            Avbryt
          </button>
          <button
            type="button"
            class="teacher-confirm-button teacher-confirm-button-danger confirm-leave-classroom"
            :disabled="leaving"
            @click="leaveClassroom"
          >
            {{ leaving ? 'Forlater...' : 'Bekreft' }}
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="deleteClassroomPopupOpen && classroom"
      class="teacher-confirm-overlay"
      @click.self="closeDeleteClassroomPopup"
    >
      <section class="teacher-confirm-popup" role="dialog" aria-modal="true">
        <h2>Slett klasserom?</h2>
        <p>
          Er du sikker på at du vil slette
          <strong>{{ classroom.classroomName }}</strong
          >? Dette kan ikke angres.
        </p>

        <div class="teacher-confirm-actions">
          <button
            type="button"
            class="teacher-confirm-button teacher-confirm-button-secondary cancel-delete-classroom"
            :disabled="deleting"
            @click="closeDeleteClassroomPopup"
          >
            Avbryt
          </button>
          <button
            type="button"
            class="teacher-confirm-button teacher-confirm-button-danger confirm-delete-classroom"
            :disabled="deleting"
            @click="deleteClassroom"
          >
            {{ deleting ? 'Sletter...' : 'Bekreft' }}
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.edit-classroom-btn {
  width: max-content;
  height: max-content;
}
.edit-classroom-view {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: var(--medium-gap);
  align-items: flex-start;
}

.edit-classroom-view h1,
.edit-classroom-view h2,
.edit-classroom-view p {
  margin: 0;
}

.teacher-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.edit-card {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  padding: 1rem;
  border-radius: 1rem;
  background: var(--beige-medium-80);
  border: 2px solid var(--brown-dark-20);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field span {
  font-weight: 700;
  color: var(--brown-dark);
}

.teacher-section-header h3 {
  margin: 0;
}

.teacher-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.teacher-list-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
  padding: 0.9rem 1rem;
  border-radius: 1rem;
  background: var(--beige-medium-80);
  /* border: 1px solid rgba(85, 50, 27, 0.18); */
  border: var(--border-wide) solid var(--brown-dark);
}

.teacher-name {
  font-weight: 700;
  color: var(--brown-dark);
}

.teacher-email {
  color: var(--brown-dark);
  word-break: break-word;
  text-align: right;
}

.invite-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
}

.teacher-input {
  width: 100%;
  box-sizing: border-box;
  min-height: 4.25rem;
  border-radius: 999px;
  border: 3px solid var(--brown-dark);
  background: var(--beige-light);
  padding: 0.75rem 1rem;
  font: inherit;
  color: var(--brown-dark);
}

.teacher-textarea {
  width: 100%;
  box-sizing: border-box;
  border-radius: 1rem;
  border: 3px solid var(--brown-dark);
  background: var(--beige-light);
  padding: 0.75rem 1rem;
  font: inherit;
  color: var(--brown-dark);
  resize: vertical;
}

.edit-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
  gap: 1rem;
}

.action-button {
  min-height: 4.25rem;
  border-radius: 999px;
  border: 3px solid var(--brown-dark);
  background: var(--beige-light);
  padding: 0.75rem 1rem;
  font: inherit;
  font-weight: 700;
  font-size: 1rem;
  color: var(--brown-dark);
  cursor: pointer;
}
.action-button:focus,
.action-button:hover {
  color: var(--beige-light);
  background-color: var(--brown-dark);
}

.secondary-button {
  background: transparent;
}

.destructive-button {
  background: var(--red-danger);
}

.action-button:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.helper-text,
.state-message {
  color: var(--brown-dark);
}

.error-text {
  color: var(--warning-sec);
}

.state-message {
  text-align: center;
}

.state-message.error {
  color: var(--warning-sec);
}

.teacher-confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgb(60 42 31 / 45%);
}

.teacher-confirm-popup {
  width: min(520px, calc(100vw - 32px));
  padding: 2rem;
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 24px;
  background: var(--beige-light);
  box-shadow: 0 16px 30px var(--brown-dark-20);
  text-align: center;
}

.teacher-confirm-popup h2 {
  margin: 0 0 0.75rem;
  font-size: 2rem;
  color: var(--brown-dark);
}

.teacher-confirm-popup p {
  margin: 0;
  color: var(--brown-dark);
  font-size: 1.15rem;
  font-weight: 700;
  line-height: 1.35;
}

.teacher-confirm-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.75rem;
}

.teacher-confirm-button {
  min-width: 140px;
  padding: 0.85rem 1.4rem;
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 18px;
  color: var(--brown-dark);
  font-size: 1rem;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 12px var(--brown-dark-20);
}

.teacher-confirm-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.teacher-confirm-button-secondary {
  background: var(--beige-medium);
}

.teacher-confirm-button-danger {
  background: var(--red-danger);
}

@media (max-width: 640px) {
  .invite-row {
    grid-template-columns: 1fr;
  }

  .teacher-list-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .teacher-email {
    text-align: left;
  }
}
</style>
