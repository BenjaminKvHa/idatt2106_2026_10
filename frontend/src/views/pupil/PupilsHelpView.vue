<script setup lang="ts">
import TopBar from '@/components/pupil/TopBar.vue'
import blobbyImage from '@/assets/icons/blobby/creaturePeaking.png'
import boardBackground from '@/assets/backgrounds/korktavle.png'
</script>

<template>
  <div class="pupil-scene-screen">
    <TopBar title="Hjelp" />

    <main class="help-page pupil-scene-page pupil-scene-page--dimmed">
      <section class="pupil-pinboard-stage" aria-labelledby="help-title">
        <img class="pupil-pinboard-image" :src="boardBackground" alt="" aria-hidden="true" />
        <div class="pupil-board-blobby-wrapper" aria-hidden="true">
          <img class="pupil-board-blobby-image" :src="blobbyImage" alt="" />
        </div>

        <div class="text-box">
          <h1 id="help-title">Hva går spillet ut på?</h1>

          <div class="scroll-content pupil-thin-scroll">
            <p>
              I dette spillet skal du hjelpe til med å avsløre falsk informasjon og lære hvordan du
              vurderer kilder kritisk.
            </p>

            <p>Du beveger deg gjennom ulike stoppesteder:</p>

            <ul>
              <li><strong>Nyhetskvartalet:</strong> Vurder om nyheter er ekte eller falske.</li>
              <li><strong>Fotografen:</strong> Se om bilder er manipulerte eller ekte.</li>
              <li><strong>Postkontoret:</strong> Identifiser svindel og mistenkelige meldinger.</li>
              <li><strong>Datasenteret:</strong> Løs oppgaver knyttet til digital sikkerhet.</li>
            </ul>

            <p>Underveis samler du poeng (XP) og konkurrerer mot andre klasser på leaderboardet.</p>

            <p>Målet er å lære hvordan du kan navigere trygt i en digital verden.</p>

            <p>Lykke til!</p>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.help-page {
  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
}

.text-box {
  position: absolute;

  top: 18%;

  bottom: 22%;

  left: 16%;
  right: 16%;

  display: flex;
  flex-direction: column;
  overflow: hidden;

  padding: 0.75rem 1.25rem;
  border-radius: 1rem;
}

h1 {
  flex-shrink: 0;

  margin: 0.5rem 0 0.75rem 0;

  text-align: center;
  font-family: 'Inika', serif;

  font-size: clamp(1.6rem, 3vw, 2.4rem);

  color: var(--brown-dark);
}

.scroll-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.75rem;

  font-family: 'Inter', sans-serif;
  color: var(--brown-dark);
  font-size: clamp(1rem, 1.6vw, 1.25rem);
  line-height: 1.6;
}

.scroll-content p {
  margin: 0 0 1rem 0;
}

ul {
  margin: 0 0 1rem 0;
  padding-left: 1.4rem;
}

li {
  margin-bottom: 0.45rem;
}

strong {
  font-weight: 800;
}

@media (max-width: 640px) {
  .text-box {
    top: 20%;
    bottom: 20%;
    left: 14%;
    right: 14%;
  }

  h1 {
    font-size: 1.4rem;
    margin: 0.3rem 0 0.5rem 0;
  }

  .scroll-content {
    font-size: 0.9rem;
    line-height: 1.45;
  }
}
</style>
