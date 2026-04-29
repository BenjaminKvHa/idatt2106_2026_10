<script setup lang="ts">
withDefaults(
    defineProps<{
      message?: string
    }>(),
    {
      message: 'Denne funksjonen er under utvikling.',
    },
)
</script>

<template>
  <p class="wip-notice">
    {{ message }}
  </p>
</template>

<style scoped>
.wip-notice {
  text-align: center;
  color: var(--brown-dark);
  font-weight: 600;
  opacity: 0.8;
}
</style>
