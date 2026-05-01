<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { X } from '@lucide/vue'

const emit = defineEmits<{ (e: 'close'): void }>()

function close() {
  emit('close')
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    close()
  }
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div class="teacher-help-backdrop" @click.self="close">
      <aside class="teacher-help-panel teacher-variant-1" role="dialog" aria-modal="true" aria-labelledby="teacher-help-title">
        <header class="teacher-help-header">
          <h2 id="teacher-help-title" class="inika">Hjelp for lærer</h2>
          <button type="button" class="teacher-help-close icon-btn-circle" aria-label="Lukk hjelp" @click="close">
            <X class="teacher-help-icon" />
          </button>
        </header>

        <div class="teacher-help-content">
          <section>
            <h3>Dette er lærervisningen</h3>
            <p>
              Til venstre ser du klasserommene du er lærer for, og knappen for å lage et nytt
              klasserom. Midten og høyre side viser innholdet for valgt klasserom.
            </p>
          </section>

          <section>
            <h3>Faner i et klasserom</h3>
            <ul>
              <li><strong>Ledertavle:</strong> se poeng og rangering for elevene.</li>
              <li><strong>Elever:</strong> godkjenn nye elever og se elevenes arbeid.</li>
              <li><strong>Rediger klasserom:</strong> endre navn, beskrivelse og lærertilgang.</li>
              <li><strong>Rediger oppgaver:</strong> lag og juster oppgaver for klassen.</li>
              <li><strong>Ukens mysterium:</strong> se og håndter bidrag fra elevene.</li>
            </ul>
          </section>

          <section>
            <h3>Teste elevflyten</h3>
            <p>
              For å teste et klasserom kan du logge ut av lærerbrukeren, lage en ny elevbruker og
              be om å bli med i klasserommet med romkoden du ser på lærersiden. Logg deretter inn
              som lærer igjen, godkjenn testeleven under Elever, og prøv elevens notatbok, kart og
              oppgaver. Da blir det enklere å forklare flyten til elevene senere.
            </p>
          </section>

          <section>
            <h3>Knappene nede til venstre</h3>
            <p>
              Bjellen viser varsler, lydknappen demper eller skrur på lyden, tannhjulet åpner
              innstillinger, og spørsmålstegnet åpner denne hjelpen.
            </p>
          </section>
        </div>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.teacher-help-backdrop {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: flex;
  justify-content: flex-end;
  background: var(--obscure);
}

.teacher-help-panel {
  width: min(31rem, calc(100vw - 1.2rem));
  height: calc(100dvh - 1.2rem);
  margin: 0.6rem;
  padding: var(--thick-padding);
  overflow: auto;
  box-shadow: -0.5rem 0 1.5rem rgba(0, 0, 0, 0.24);
}

.teacher-help-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--medium-gap);
}

.teacher-help-header h2 {
  margin: 0;
  font-size: clamp(1.35rem, 2vw, 1.8rem);
}

.teacher-help-close {
  --button-diameter: 42px;
}

.teacher-help-icon {
  width: 18px;
  height: 18px;
}

.teacher-help-content {
  display: flex;
  flex-direction: column;
  gap: var(--medium-gap);
  line-height: 1.45;
}

.teacher-help-content section {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.teacher-help-content h3,
.teacher-help-content p,
.teacher-help-content ul {
  margin: 0;
}

.teacher-help-content h3 {
  font-size: 1.05rem;
}

.teacher-help-content ul {
  padding-left: 1.2rem;
}

.teacher-help-content li + li {
  margin-top: 0.35rem;
}

@media (max-width: 640px) {
  .teacher-help-backdrop {
    justify-content: center;
  }

  .teacher-help-panel {
    width: calc(100vw - 0.8rem);
    height: calc(100dvh - 0.8rem);
    margin: 0.4rem;
    padding: var(--medium-padding);
  }
}
</style>
