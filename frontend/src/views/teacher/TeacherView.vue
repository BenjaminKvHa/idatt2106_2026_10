<script setup lang="ts">
import TeacherMenu from '@/components/layout/TeacherMenu.vue'
</script>

<template>
  <div class="teacher-page">
    <aside class="teacher-sidebar">
      <TeacherMenu />
    </aside>

    <div class="teacher-main">
      <header class="teacher-navbar">
        <router-link to="/teacher/profile" class="overlay-button profile">
          Gå til profil
        </router-link>
      </header>

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

.teacher-navbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: var(--background-body);
  padding: 0 var(--medium-padding);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.teacher-content {
  min-height: 0;
  min-width: 0;
  overflow: auto;
  padding: var(--medium-padding);
}
</style>
