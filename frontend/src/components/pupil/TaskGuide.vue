<script setup lang="ts">
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
  <div class="task-guide-overlay">
    <div class="task-guide-card">
      <div class="task-guide-text">
        <slot />
      </div>

      <button class="task-guide-button" @click="confirm">
        {{ buttonText }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.task-guide-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgb(0 0 0 / 30%);
  backdrop-filter: blur(3px);
}

.task-guide-card {
  width: min(900px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  padding: clamp(1.2rem, 2.4vw, 2.2rem);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: clamp(0.8rem, 2vw, 1.3rem);
  background: linear-gradient(180deg, #fff3d6 0%, #f4d7a2 100%);
  border: 3px solid var(--brown-dark);
  border-radius: 34px;
  box-shadow: 0 8px 24px rgb(0 0 0 / 18%);
}

.task-guide-text {
  width: 100%;
  text-align: center;
  font-size: clamp(1rem, 1.8vw, 1.65rem);
  line-height: 1.35;
}

.task-guide-text h2 {
  margin: 0 0 0.7rem;
  font-size: clamp(1.6rem, 3vw, 2.8rem);
}

.task-guide-text p {
  margin: 0;
  display: block;
}

.task-guide-text img {
  display: block;
  width: auto;
  max-width: min(300px, 65%);
  max-height: 24vh;
  margin: 0.8rem auto 0;
  object-fit: contain;
}

.task-guide-button {
  position: relative;
  z-index: 2;
  padding: 0.65rem 2rem;
  border: 2px solid var(--brown-dark);
  border-radius: 24px;
  background: linear-gradient(180deg, #9df13c 0%, #78d927 100%);
  font-size: clamp(1.1rem, 2vw, 1.6rem);
  font-weight: 850;
  cursor: pointer;
}
</style>
