<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { getJwtData } from './utils/jwtUtils';
import router from './router';

const tokenData = getJwtData();

if (!tokenData && (window.location.href.includes("/pupil") || window.location.href.includes("/teacher"))) {
  router.push("/")
}
</script>

<template>
  <RouterView />
</template>

<style scoped></style>
