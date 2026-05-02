<template>
  <article class="clickable-card teacher-card">
    <div class="card-header teacher-card-header" @click.self="emit('header-click')">
      <div class="header-title teacher-card-title">
        <span>{{ title }}</span>
      </div>
      <div class="header-actions teacher-card-header-actions">
        <slot name="header-actions" />
      </div>
    </div>

    <div class="card-body teacher-card-body">
      <slot />
    </div>
  </article>
</template>

<script setup lang="ts">
const props = defineProps({
  title: {
    type: String,
    default: 'test',
  },
})

const emit = defineEmits<{
  (e: 'header-click'): void
}>()
</script>

<style scoped>
.clickable-card {
  width: 100%;
  max-width: 340px;
}

.header-title {
  letter-spacing: 0.03em;
  text-transform: capitalize;
}

.card-body {
  min-height: 140px;
}
</style>
