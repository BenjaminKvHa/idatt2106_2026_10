<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Logo from '@/components/misc/Logo.vue'

const route = useRoute()

const classroomCode = computed(() => {
  const code = typeof route.params.classroomCode === 'string' ? route.params.classroomCode : ''
  return code.toUpperCase()
})
</script>

<template>
  <main class="join-display-page">
    <Logo />

    <section class="join-display-panel" aria-labelledby="join-display-title">
      <p class="join-display-kicker">Bli med i klasserommet</p>
      <h1 id="join-display-title">Romkode</h1>
      <p class="join-display-code">{{ classroomCode }}</p>

      <div class="join-display-steps">
        <p>1. Logg inn eller lag en elevbruker.</p>
        <p>2. Velg å bli med i et klasserom.</p>
        <p>3. Skriv inn romkoden og send forespørsel.</p>
        <p>4. Vent til læreren godkjenner deg.</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.join-display-page {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: clamp(1.2rem, 3vw, 2.4rem);
  padding: clamp(1rem, 3vw, 2rem);
  color: var(--accent);
  background: var(--background-body);
  text-align: center;
  box-sizing: border-box;
}

.join-display-panel {
  width: min(68rem, 96vw);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: clamp(0.9rem, 2vw, 1.5rem);
  padding: clamp(1.4rem, 4vw, 3rem);
  border: clamp(5px, 0.6vw, 9px) solid var(--accent);
  border-radius: var(--border-radius);
  background: var(--background);
}

.join-display-kicker,
.join-display-panel h1,
.join-display-code,
.join-display-steps p {
  margin: 0;
}

.join-display-kicker {
  font-size: clamp(1.2rem, 2.4vw, 2rem);
  font-weight: var(--fw-bold);
}

.join-display-panel h1 {
  font-size: clamp(2rem, 4vw, 3.5rem);
}

.join-display-code {
  padding: clamp(0.4rem, 1vw, 0.8rem) clamp(1rem, 3vw, 2rem);
  border: clamp(5px, 0.6vw, 9px) solid var(--accent);
  border-radius: var(--border-radius-2);
  background: var(--background-body);
  font-size: clamp(4rem, 15vw, 12rem);
  font-weight: var(--fw-bold);
  line-height: 0.95;
}

.join-display-steps {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(0.8rem, 1.8vw, 1.2rem);
  width: min(56rem, 100%);
  font-size: clamp(1rem, 1.8vw, 1.45rem);
  font-weight: var(--fw-semibold);
  line-height: 1.25;
}

.join-display-steps p {
  padding: clamp(0.7rem, 1.6vw, 1rem);
  border: 3px solid var(--background-sec);
  border-radius: var(--border-radius-2);
  background: rgba(255, 255, 255, 0.18);
}

@media (max-width: 720px) {
  .join-display-steps {
    grid-template-columns: 1fr;
  }
}
</style>
