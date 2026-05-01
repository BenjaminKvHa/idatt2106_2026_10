<script setup lang="ts">
import BaseModal from '@/components/ui/BaseModal.vue'

withDefaults(
  defineProps<{
    buttonText?: string
  }>(),
  {
    buttonText: 'Jeg er klar!',
  },
)

const emit = defineEmits<{
  (e: 'close'): void
}>()

function confirm(): void {
  emit('close')
}
</script>

<template>
  <BaseModal
    panel-class=""
    :close-on-backdrop="false"
    :close-on-escape="false"
    :show-close-button="false"
    @close="confirm"
  >
    <div class="center-container">
      <div class="guide-text">
        <slot />
      </div>
      <button class="confirm-button" @click="confirm">
        {{ buttonText }}
      </button>
    </div>
  </BaseModal>
</template>

<style scoped>
.center-container {
  gap: 2rem;
  padding: 1rem 1rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--orange-medium);
  border-radius: 40px;
  border: 5px solid var(--brown-dark);
  width: min(80vw, 1000px);
  box-shadow: -8px 8px 4px var(--brown-dark-60);
  box-sizing: border-box;
}

.confirm-button{
  margin-top: auto;
  padding: 0.5rem 1rem;
  background: var(--green-confirm);
  border: 5px solid var(--brown-dark);
  border-radius: 40px;
  font-size: 30px;
  min-width: 30%;
}

.guide-text{
  text-align: center;
  padding-top: 1.5rem;
  width: 90%;
  white-space: pre-wrap;
  font-size: 30px;
}

.guide-text h2 {
  margin:0;
}

.guide-text p {
  margin: 0;
}
</style>
