<script setup lang="ts">
import TeacherMenu from '@/components/teacher/TeacherMenu.vue'
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
  height: 100vh;
  min-width: 0;
}

.teacher-sidebar {
  position: sticky;
  top: 0;
  align-self: start;
  z-index: 20;
  height: 100vh;
}

.teacher-main {
  display: grid;
  grid-template-rows: auto 1fr;
  min-height: 0;
  min-width: 0;
  flex-grow: 1;
}

.teacher-content {
  min-height: 0;
  min-width: 0;
  overflow: auto;
  padding: var(--medium-padding);
}

@media (max-width: 1200px) {
  .teacher-page {
    grid-template-columns: minmax(180px, 16rem) 1fr;
  }
}

@media (max-width: 900px) {
  .teacher-page {
    grid-template-columns: minmax(140px, 10rem) 1fr;
  }

  .teacher-content {
    padding: var(--thin-padding);
  }
}

@media (max-width: 640px) {
  .teacher-page {
    grid-template-columns: minmax(110px, 7rem) 1fr;
  }
}
</style>
