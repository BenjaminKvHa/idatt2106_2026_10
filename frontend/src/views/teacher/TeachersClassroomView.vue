<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useTeacherTopbar } from '@/composables/useTeacherTopbar'

const route = useRoute()
const { setTabs, clearTabs } = useTeacherTopbar()

onMounted(() => {
  const base = `/${route.params.teacherUsername}/classrooms/${route.params.classroomName}`
  setTabs([
    { label: 'Oversikt', to: base },
    { label: 'Elever', to: `${base}/pupils` },
    { label: 'Innstillinger', to: `${base}/edit-classroom` },
  ])
})

onUnmounted(() => {
  clearTabs()
})
</script>

<template>
  <main>This is a view</main>
</template>
