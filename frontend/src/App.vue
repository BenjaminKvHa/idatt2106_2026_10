<script setup lang="ts">
import { RouterView } from 'vue-router'
import { getJwtDataWithRefresh } from './utils/jwtUtils';
import router from './router';
import { onMounted } from 'vue'

onMounted(() => {
  const tokenData = getJwtDataWithRefresh();
  if (!tokenData && (window.location.href.includes("/pupil") || window.location.href.includes("/teacher"))) {
    router.push("/")
  }
});

</script>

<template>
  <RouterView />
</template>

<style scoped></style>
