<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTeacherTopbar } from '@/composables/useTeacherTopbar'

const route = useRoute()
const { setTabs, clearTabs } = useTeacherTopbar()

onMounted(() => {
  const classroomPath = `/teacher/classrooms/${route.params.classroomName}`
  setTabs([
    { label: 'Oversikt', to: `${classroomPath}/oversikt` },
    { label: 'Elever', to: `${classroomPath}/pupils` },
    { label: 'Innstillinger', to: `${classroomPath}/edit-classroom` },
  ])
})

onUnmounted(() => {
  clearTabs()
})
</script>
<template>
  <main>This is a view</main>
</template>
