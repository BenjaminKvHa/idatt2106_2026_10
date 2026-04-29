<script setup lang="ts">
withDefaults(
  defineProps<{
    loading?: boolean
    error?: string | null
    empty?: boolean
    loadingText?: string
    emptyText?: string
  }>(),
  {
    loading: false,
    error: null,
    empty: false,
    loadingText: 'Laster...',
    emptyText: 'Ingen data funnet.',
  },
)
</script>

<template>
  <p v-if="loading" class="state-display state-display--loading" role="status">
    {{ loadingText }}
  </p>
  <p v-else-if="error" class="state-display state-display--error" role="alert">
    {{ error }}
  </p>
  <p v-else-if="empty" class="state-display state-display--empty">
    {{ emptyText }}
  </p>
</template>

<style scoped>
.state-display {
  margin: 0;
  text-align: center;
}

.state-display--error {
  color: var(--red-danger, #c62828);
}
</style>
