<script setup lang="ts">
import TopBar from '@/components/pupil/TopBar.vue'
import boardBackground from '@/assets/backgrounds/korktavle.png'
import homeBg from '@/assets/backgrounds/hjemskjerm-bg.png'
</script>

<template>
  <TopBar title="Hjelp" />

  <div class="page" :style="{ backgroundImage: `url(${homeBg})` }">
    <div class="board-card">
      <img class="board-image" :src="boardBackground" alt="Korktavle" />

      <div class="text-box">
        <h1>Hva går spillet ut på?</h1>

        <div class="scroll-content">
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

          <p>Lykke til! 🕵️‍♂️</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  min-height: calc(100vh - 4rem);
  align-items: center;
  justify-content: center;
  padding: 1.5rem;

  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.board-card {
  position: relative;
  width: min(92vw, 70rem);
}

.board-image {
  display: block;
  width: 100%;
  height: auto;
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

  scrollbar-width: thin;
  scrollbar-color: rgba(88, 52, 28, 0.75) rgba(255, 238, 200, 0.35);
}

.scroll-content::-webkit-scrollbar {
  width: 0.75rem;
}

.scroll-content::-webkit-scrollbar-track {
  background: rgba(255, 238, 200, 0.35);
  border-radius: 999px;
  border: 1px solid rgba(88, 52, 28, 0.25);
}

.scroll-content::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, rgba(126, 74, 36, 0.95), rgba(82, 45, 22, 0.95));
  border-radius: 999px;
  border: 2px solid rgba(255, 238, 200, 0.65);
}

.scroll-content::-webkit-scrollbar-thumb:hover {
  background: rgba(82, 45, 22, 1);
}

.scroll-content::-webkit-scrollbar-button {
  display: none;
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
  .board-card {
    width: 98vw;
  }

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
