<script setup lang="ts">
defineProps<{
  mysterySuggestion: string
}>()
</script>

<template>
  <div class="extra-fields">
    <h2>Forslag til ukens mysterium</h2>

    <div class="read-only-box inter">
      {{ mysterySuggestion || 'Eleven har ikke sendt inn forslag enda.' }}
    </div>
  </div>
</template>

<style scoped>
.extra-fields {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.extra-fields h2 {
  margin: 0;
  font-size: 1.35rem;
}

.read-only-box {
  width: 100%;
  min-height: 90px;
  border: 3px solid var(--background-sec);
  border-radius: 14px;
  background: var(--background);
  padding: 14px 16px;
  font-size: 1rem;
}
</style>
