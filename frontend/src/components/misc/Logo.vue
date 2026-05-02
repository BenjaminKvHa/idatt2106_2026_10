<script setup lang="ts">
defineOptions({
  name: 'AppLogo',
})
</script>

<template>
  <div class="logo-wrapper">
    <img src="../../assets/logo/nettdetektivene_logo.png" alt="Nettdetektivene" class="logo" />
  </div>
</template>

<style scoped>
.logo-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.logo {
  height: auto;
  display: block;
  display: block;
  width: 100%;
  max-width: 12rem;
}
</style>
