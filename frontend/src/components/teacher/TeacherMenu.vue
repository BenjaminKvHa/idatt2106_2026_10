<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import Logo from '@/components/misc/Logo.vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import OptionsMenu from '@/components/ui/OptionsMenu.vue'
import NotificationsMenu from '@/components/ui/NotificationsMenu.vue'
import TeacherHelpPanel from '@/components/teacher/TeacherHelpPanel.vue'
import { BellIcon, CircleQuestionMark, Settings, Volume2, VolumeX } from '@lucide/vue'
import { useClassrooms } from '@/composables/useClassrooms.ts'
import { useAudio } from '@/composables/useAudio'

const settingsOpen = ref(false)
const notificationsOpen = ref(false)
const classroomsPickerOpen = ref(false)
const helpOpen = ref(false)
const route = useRoute()
const { classrooms, fetchClassrooms } = useClassrooms()
const { isMuted, toggleMute } = useAudio()

onMounted(() => {
  fetchClassrooms()
})

const isClassroomsActive = computed(() => {
  return route.path.includes('/classrooms') && !route.path.includes('/classrooms/new')
})

const isNewClassroomActive = computed(() => {
  return route.path.endsWith('/classrooms/new')
})

const isClassroomActive = (classroomCode: string) => {
  return route.path.includes(`/classrooms/${classroomCode}`)
}

const activeClassroomCode = computed(() => {
  const classroomCodeSlug = typeof route.params.classroomCode === 'string' ? route.params.classroomCode : ''
  if (!classroomCodeSlug) {
    return null
  }

  return classroomCodeSlug
})

const activeClassroomName = computed(() => {
  const classroomCode = activeClassroomCode.value
  if (!classroomCode) {
    return 'Velg klasserom'
  }

  return classrooms.value.find((classroom) => classroom.classroomCode === classroomCode)?.classroomName ?? classroomCode
})

function closeClassroomsPicker() {
  classroomsPickerOpen.value = false
}
</script>

<template>
    <div class="teacher-menu teacher-variant-2">
        <Logo />

        <section class="teacher-card teacher-card-menu desktop-classroom-list">
            <RouterLink
                type="button"
                :class="['teacher-card-title', isClassroomsActive ? 'button-variant-2' : 'button-variant-1']"
                :to="`/teacher/classrooms`"
            >
                Klasserom
            </RouterLink>
            <div class="teacher-card-body teacher-card-menu-body">
                <RouterLink
                    v-for="classroom in classrooms"
                    :key="classroom.classroomCode"
                    type="button"
                    :to="`/teacher/classrooms/${classroom.classroomCode}/leaderboard`"
                    :class="['button-variant-1 inika teacher-card-item-var-1', isClassroomActive(classroom.classroomCode) && 'button-variant-2 teacher-card-item-var-2']"
                >
                    {{ classroom.classroomName }}
                </RouterLink>
                <div v-if="classrooms.length === 0" class="empty-message">
                    Ingen klasserom
                </div>
            </div>
        </section>

        <button
            type="button"
            :class="['button-variant-1', 'teacher-menu-button', 'teacher-classroom-picker-button', isClassroomsActive && 'button-variant-2']"
            @click="classroomsPickerOpen = true"
        >
            <span class="picker-title inika">Klasserom</span>
            <span class="picker-current">{{ activeClassroomName }}</span>
        </button>

        <RouterLink
            type="button"
            :class="['button-variant-1', 'teacher-menu-button', 'inika', isNewClassroomActive && 'button-variant-2']"
            :to="`/teacher/classrooms/new`"
        >
            Nytt klasserom
        </RouterLink>

        <div class="teacher-menu-bottom">
            <button 
            type="button" 
            class="icon-btn-circle" 
            aria-label="Varsler"
            @click="notificationsOpen = true"
            >
                <BellIcon class="teacher-menu-icon" />
            </button>
            <button
                type="button"
                class="icon-btn-circle"
                :aria-label="isMuted ? 'Skru på lyd' : 'Demp lyd'"
                @click="toggleMute"
            >
                <VolumeX v-if="isMuted" class="teacher-menu-icon" />
                <Volume2 v-else class="teacher-menu-icon" />
            </button>
            <button
                type="button"
                class="icon-btn-circle"
                aria-label="Innstillinger"
                @click="settingsOpen = true"
            >
                <Settings class="teacher-menu-icon" />
            </button>
            <button
                type="button"
                class="icon-btn-circle"
                aria-label="Hjelp"
                @click="helpOpen = true"
            >
                <CircleQuestionMark class="teacher-menu-icon" />
            </button>
        </div>
    </div>

    <OptionsMenu v-if="settingsOpen" @close="settingsOpen = false" />
    <NotificationsMenu v-if="notificationsOpen" :classroom-code="activeClassroomCode" @close="notificationsOpen = false" />
    <TeacherHelpPanel v-if="helpOpen" @close="helpOpen = false" />
    <BaseModal v-if="classroomsPickerOpen" panel-class="teacher-variant-1 classroom-picker-modal" @close="closeClassroomsPicker">
        <h2 class="classroom-picker-title inika">Velg klasserom</h2>

        <RouterLink
            class="button-variant-1 classroom-picker-link inika"
            to="/teacher/classrooms"
            @click="closeClassroomsPicker"
        >
            Alle klasserom
        </RouterLink>

        <div v-if="classrooms.length > 0" class="classroom-picker-list">
            <RouterLink
                v-for="classroom in classrooms"
                :key="classroom.classroomCode"
                :to="`/teacher/classrooms/${classroom.classroomCode}/leaderboard`"
                :class="['classroom-picker-link', 'inika', isClassroomActive(classroom.classroomCode) ? 'button-variant-2' : 'button-variant-1']"
                @click="closeClassroomsPicker"
            >
                {{ classroom.classroomName }}
            </RouterLink>
        </div>

        <p v-else class="classroom-picker-empty">Ingen klasserom</p>
    </BaseModal>
</template>

<style scoped>
.teacher-card-menu {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
}

.teacher-classroom-picker-button {
  display: none;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.2rem;
}

.picker-title,
.picker-current {
  display: block;
}

.picker-current {
  font-size: 0.82rem;
  font-weight: 600;
}

.teacher-card-menu-body {
  min-height: 0;
  overflow: auto;
}

.empty-message {
    text-align: center;
    color: var(--background-sec);
    font-size: 0.85rem;
    padding: var(--thin-padding);
}


.teacher-menu {
    display: flex;
    flex-direction: column;
    border-radius: 0;
    border: none;
    outline-width: var(--border-width);
    outline-color: var(--accent);
    outline-style: solid;
    padding: var(--medium-padding);
    justify-content: flex-start;
    margin: 0;
    height: 100%;
    box-sizing: border-box;
    overflow: hidden;
    min-height: 0;
}

.teacher-menu-button {
    width: 100%;
    min-height: 3.5rem;
    padding: 0.85rem 1rem;
    text-align: center;
    font-weight: 600;
    line-height: 1.15;
    white-space: normal;
    text-wrap: balance;
}

.teacher-menu-bottom {
    margin-top: auto;
    display: flex;
    justify-content: center;
    padding-top: var(--medium-padding);
    gap: var(--thin-gap);
    flex-wrap: wrap;

    > .icon-btn-circle:hover {
        box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.25);
    }
}

.teacher-menu-icon {
  width: 18px;
  height: 18px;
  display: block;
}

.classroom-picker-modal {
  width: min(92vw, 24rem);
  gap: 0.9rem;
}

.classroom-picker-title,
.classroom-picker-empty {
  margin: 0;
  text-align: center;
}

.classroom-picker-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.classroom-picker-link {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 3.25rem;
  padding: 0.75rem 1rem;
  text-align: center;
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 1024px) {
  .teacher-menu {
    padding: 0.85rem;
  }

  .teacher-menu-button {
    min-height: 3.25rem;
    padding: 0.7rem 0.8rem;
    font-size: 0.95rem;
  }
}

@media (max-width: 768px) {
  .teacher-menu {
    padding: 0.6rem;
    gap: 0.6rem;
  }

  .desktop-classroom-list {
    display: none;
  }

  .teacher-classroom-picker-button {
    display: flex;
  }

  .teacher-menu-button {
    min-height: 3rem;
    padding: 0.6rem;
    font-size: 0.88rem;
  }

  .teacher-menu-bottom {
    padding-top: 0.6rem;
    gap: 0.45rem;
  }
}

@media (max-width: 640px) {
  .teacher-menu-button {
    border-width: 4px;
    border-radius: 1.2rem;
    font-size: 0.82rem;
  }

  .classroom-picker-link {
    min-height: 3rem;
    font-size: 0.9rem;
  }
}
</style>
