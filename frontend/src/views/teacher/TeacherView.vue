<script setup lang="ts">
import TeacherMenu from '@/components/layout/TeacherMenu.vue'
import TeacherTopbar from '@/components/ui/TeacherTopbar.vue'
</script>

<template>
  <div class="teacher-page">
    <aside class="teacher-sidebar">
      <TeacherMenu />
    </aside>

    <div class="teacher-main">
      <TeacherTopbar />

      <main class="teacher-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.teacher-page {
  display: grid;
  grid-template-columns: minmax(220px, 20vw) 1fr;
  min-height: 100vh;
  min-width: 0;
}

.teacher-sidebar {
  position: sticky;
  top: 0;
  align-self: start;
  height: 100vh;
  z-index: 20;
}

.teacher-main {
  display: grid;
  grid-template-rows: auto 1fr;
  min-height: 0;
  min-width: 0;
}

.teacher-content {
  min-height: 0;
  min-width: 0;
  overflow: auto;
  padding: var(--medium-padding);
}
</style>
