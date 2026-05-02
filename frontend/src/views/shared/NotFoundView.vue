<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="welcome-page">
    <div class="scene">
      <div class="card">
        <img src="/src/assets/logo/nettdetektivene_logo.png" alt="Nettdetektivene" class="logo" />

        <h1>Noe gikk feil!</h1>
        <p class="tagline">Siden er sporløst forsvunnet!</p>

        <div class="actions">
          <button @click="goBack" class="ready-button">Gå tilbake</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background-image: url('/src/assets/backgrounds/welcomeBackground.png');
  background-size: cover;
  background-position: center;
}

.card {
  width: min(500px, 90vw);
  padding: 3rem;
  background: var(--beige-light-100);
  border: 3px solid var(--brown-dark);
  border-radius: 34px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  align-items: center;
}

.logo {
  width: 100%;
}

h1 {
  font-size: 3rem;
  margin: 0;
  color: var(--brown-dark);
}

.tagline {
  font-family: 'Inika', serif;
  font-weight: 700;
  color: var(--red-danger);
  font-size: 1.4rem;
}

.actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.ready-button {
  width: 100%;
  padding: 1rem;
  background: linear-gradient(180deg, var(--green-confirm-80) 0%, var(--green-confirm-40) 100%);
  border: 3px solid var(--brown-dark);
  border-radius: 28px;
  font-family: 'Inika', serif;
  font-size: 1.2rem;
  font-weight: 700;
  box-shadow: 0 4px 0 var(--brown-dark);
  cursor: pointer;
  box-shadow:
    0 5px 0 var(--brown-dark),
    0 12px 20px rgb(0 0 0 / 20%);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    filter 0.16s ease;
}

.ready-button:hover {
  filter: brightness(1.08) saturate(1.05);
  transform: translateY(-3px);
  box-shadow:
    0 8px 0 var(--brown-dark),
    0 18px 26px rgb(0 0 0 / 24%);
}
</style>
