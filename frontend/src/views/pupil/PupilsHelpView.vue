<script setup lang="ts">
import PinboardLayout from '@/components/pupil/PinboardLayout.vue'
import TopBar from '@/components/pupil/TopBar.vue'
</script>

<template>
  <div class="help-page-bg pupil-scene-screen">
    <TopBar title="Hjelp" />

    <PinboardLayout heading-id="help-title">
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
            <li><strong>Datasenteret:</strong> Løs oppgaver knyttet til digital sikkerhet.</li>
          </ul>

          <p>Underveis samler du poeng (XP) og konkurrerer mot andre klasser på leaderboardet.</p>

          <p>Målet er å lære hvordan du kan navigere trygt i en digital verden.</p>

          <p>Lykke til!</p>
        </div>
      </div>
    </PinboardLayout>
  </div>
</template>

<style scoped>
.help-page-bg {
  min-height: 100vh;

  background-image: url('@/assets/backgrounds/hjemskjerm-bg.png');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
}

.help-page {
  background: transparent;
}

.text-box {
  position: absolute;
  font-weight: 600;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);

  height: 70%;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  padding: 4rem 3rem 4rem 5rem;
  border-radius: 1rem;

  aspect-ratio: 1.25/1;

  background-image: url('@/assets/paper.png');
  background-size: 100% 100%;
  background-repeat: no-repeat;
  background-position: center;
}

h1 {
  flex-shrink: 0;

  margin: 0.5rem 0 0.75rem 0;

  text-align: center;
  font-family: 'Inika', serif;

  font-size: clamp(1.2rem, 3vw, 2rem);

  color: var(--brown-dark);
}

.scroll-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.75rem;

  font-family: 'Inter', sans-serif;
  color: var(--brown-dark);
  font-size: clamp(0.7rem, 1.6vw, 1rem);
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
    font-size: 1.2rem;
    margin: 0.3rem 0 0.5rem 0;
  }

  .scroll-content {
    font-size: 0.8rem;
    line-height: 1.45;
  }
}
</style>
