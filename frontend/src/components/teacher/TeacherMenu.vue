<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Logo from '@/components/misc/Logo.vue'
import OptionsMenu from '@/components/ui/OptionsMenu.vue'
import NotificationsMenu from '@/components/ui/NotificationsMenu.vue'
import { BellIcon, Settings } from '@lucide/vue'
import { useClassrooms } from '@/composables/useClassrooms.ts'

const settingsOpen = ref(false)
const notificationsOpen = ref(false)
const route = useRoute()
const router = useRouter()
const { classrooms, fetchClassrooms } = useClassrooms()

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
</script>

<template>
    <div class="teacher-menu teacher-variant-2">
        <Logo />

        <section class="teacher-card teacher-card-menu">
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
                    :class="['button-variant-1 teacher-card-item-var-1', isClassroomActive(classroom.classroomCode) && 'button-variant-2 teacher-card-item-var-2']"
                >
                    {{ classroom.classroomName }}
                </RouterLink>
                <div v-if="classrooms.length === 0" class="empty-message">
                    Ingen klasserom
                </div>
            </div>
        </section>

        <RouterLink
            type="button"
            :class="['button-variant-1', 'teacher-menu-button', isNewClassroomActive && 'button-variant-2']"
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
                aria-label="Innstillinger"
                @click="settingsOpen = true"
            >
                <Settings class="teacher-menu-icon" />
            </button>
        </div>
    </div>

    <OptionsMenu v-if="settingsOpen" @close="settingsOpen = false" />
    <NotificationsMenu v-if="notificationsOpen" :classroom-code="activeClassroomCode" @close="notificationsOpen = false" />
</template>

<style scoped>
.teacher-card-menu {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
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
    padding: 0.85rem 1rem;
    text-align: center;
}

.teacher-menu-bottom {
    margin-top: auto;
    display: flex;
    gap: var(--thin-gap);
    justify-content: center;
    padding-top: var(--medium-padding);
}

.teacher-menu-icon {
    width: 18px;
    height: 18px;
    display: block;
}
</style>
