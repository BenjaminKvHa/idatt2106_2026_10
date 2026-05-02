<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const classroomCode = computed(() => {
  const code = typeof route.params.classroomCode === 'string' ? route.params.classroomCode : ''
  return code.toUpperCase()
})

const displayUrl = computed(() => {
  return router.resolve({
    path: `/teacher/classrooms/${encodeURIComponent(classroomCode.value)}/join-display`,
  }).href
})

function openDisplayView() {
  window.open(displayUrl.value, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div class="teacher-container-box join-classroom-view">
    <div class="join-header">
      <p class="join-eyebrow">Bli med i klasserommet</p>
      <h1>Romkode</h1>
      <p class="classroom-code">{{ classroomCode }}</p>
    </div>

    <section class="join-instructions" aria-label="Instruksjoner for elever">
      <h2>Slik blir elever med</h2>
      <ol>
        <li>Gå til NettDetektivene og logg inn eller lag en elevbruker.</li>
        <li>Velg å bli med i et klasserom.</li>
        <li>Skriv inn romkoden over og send forespørsel.</li>
        <li>Du godkjenner nye elever fra fanen Elever i lærervisningen.</li>
      </ol>
    </section>

    <p class="smartboard-helper">
      Trykk på knappen under for å åpne en egen tavlevisning du kan vise elevene når de skal bli
      med i klasserommet.
    </p>

    <button type="button" class="button-variant-1 smartboard-button" @click="openDisplayView">
      Åpne tavlevisning
    </button>
  </div>
</template>

<style scoped>
.join-classroom-view {
  display: flex;
  flex-direction: column;
  gap: var(--medium-gap);
  text-align: center;
}

.join-header,
.join-instructions {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.join-eyebrow,
.join-classroom-view h1,
.join-classroom-view h2,
.join-classroom-view p,
.join-classroom-view ol {
  margin: 0;
}

.join-eyebrow {
  font-weight: var(--fw-bold);
  letter-spacing: 0;
  opacity: 0.85;
}

.join-classroom-view h1 {
  font-size: clamp(1.8rem, 3vw, 2.6rem);
}

.classroom-code {
  align-self: center;
  padding: 0.3rem 1.2rem;
  border: var(--border-width) solid var(--accent);
  border-radius: var(--border-radius-2);
  background: var(--background-body);
  font-size: clamp(3rem, 8vw, 5.5rem);
  font-weight: var(--fw-bold);
  line-height: 1;
}

.join-instructions {
  text-align: left;
}

.smartboard-helper {
  max-width: 32rem;
  align-self: center;
  line-height: 1.35;
}

.join-instructions h2 {
  text-align: center;
}

.join-instructions ol {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding-left: 1.4rem;
  line-height: 1.35;
}

.smartboard-button {
  align-self: center;
  min-width: min(100%, 16rem);
  padding: 0.75rem 1.4rem;
  font: inherit;
  font-weight: var(--fw-bold);
}
</style>
