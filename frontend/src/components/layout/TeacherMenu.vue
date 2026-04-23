<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Logo from '@/components/misc/Logo.vue'
import OptionsMenu from '@/components/ui/OptionsMenu.vue'
import gearIcon from '@/assets/icons/profile-cog.svg'
import { useClassrooms } from '@/composables/useClassrooms'

const settingsOpen = ref(false)
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

const navigateToClassrooms = () => {
  router.push(`/teacher/classrooms`)
}

const navigateToNewClassroom = () => {
  router.push(`/teacher/classrooms/new`)
}

const navigateToClassroom = (classroomCode: string) => {
  const classroom = classrooms.value.find((c) => c.classroomCode === classroomCode)
  if (classroom) {
    const classroomName = classroom.classroomName.toLowerCase().replace(/\s+/g, '-')
    router.push(`/teacher/classrooms/${classroomName}/oversikt`)
  }
}

const isClassroomActive = (classroomCode: string) => {
  return route.query.classroomCode === classroomCode ||
    route.path.includes(`/classrooms/`) && !route.path.includes('/classrooms/new') &&
    classrooms.value.some(
      (c) =>
        c.classroomCode === classroomCode &&
        c.classroomName.toLowerCase().replace(/\s+/g, '-') ===
          route.params.classroomName,
    )
}
</script>

<template>
    <div class="teacher-menu teacher-variant-2">
        <Logo />

        <section class="teacher-card teacher-card-menu">
            <button
                type="button"
                :class="['teacher-card-title', isClassroomsActive ? 'button-variant-2' : 'button-variant-1']"
                @click="navigateToClassrooms"
            >
                Klasserom
            </button>
            <div class="teacher-card-body teacher-card-menu-body">
                <button
                    v-for="classroom in classrooms"
                    :key="classroom.classroomCode"
                    type="button"
                    :class="['button-variant-1', 'teacher-card-item', isClassroomActive(classroom.classroomCode) && 'button-variant-2']"
                    @click="navigateToClassroom(classroom.classroomCode)"
                >
                    {{ classroom.classroomName }}
                </button>
                <div v-if="classrooms.length === 0" class="empty-message">
                    Ingen klasserom
                </div>
            </div>
        </section>

        <button
            type="button"
            :class="['button-variant-1', 'teacher-menu-button', isNewClassroomActive && 'button-variant-2']"
            @click="navigateToNewClassroom"
        >
            Nytt klasserom
        </button>

        <div class="teacher-menu-bottom">
            <button type="button" class="icon-btn-circle" aria-label="Varsler"></button>
            <button
                type="button"
                class="icon-btn-circle"
                aria-label="Innstillinger"
                @click="settingsOpen = true"
            >
                <img :src="gearIcon" alt="" class="teacher-menu-icon" aria-hidden="true" />
            </button>
        </div>
    </div>

    <OptionsMenu v-if="settingsOpen" @close="settingsOpen = false" />
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