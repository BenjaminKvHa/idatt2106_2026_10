<script setup lang="ts"></script>

<template>
  <div class="welcome-page">
    <div class="scene">
      <div class="card">
        <img src="/src/assets/logo/nettdetektivene_logo.png" alt="Nettdetektivene logo" />
        <p class="tagline">Er du klar for oppdraget?</p>
        <router-link to="/sign-in" class="link">
          <button class="ready-button">Logg inn</button>
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome-page {
  min-height: 100vh;
  width: 100%;
  display: grid;
  place-items: center;
  padding: 2rem;
  background-image: url('/src/assets/backgrounds/welcomeBackground.png');
  background-size: cover;
  background-position: center;
}

.scene {
  width: 100%;
  display: grid;
  place-items: center;
}

.card {
  width: min(560px, calc(100vw - 32px));
  padding: clamp(1.8rem, 4vw, 3rem);
  background: var(--beige-light-100);
  border: 3px solid var(--brown-dark);
  border-radius: 34px;
  box-shadow: 0 8px 24px rgb(0 0 0 / 18%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.3rem;
}

img {
  width: min(460px, 80vw);
  max-height: 42vh;
  object-fit: contain;
}

.tagline {
  font-family: 'Inika', serif;
  font-size: clamp(0.8rem, 1.5vw, 1rem);
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin: 0;
  text-align: center;
}

.link {
  text-decoration: none;
}

.ready-button {
  padding: 0.85rem 2.8rem;
  background: linear-gradient(180deg, var(--green-confirm-80) 0%, var(--green-confirm-40) 100%);
  border: 3px solid var(--brown-dark);
  border-radius: 28px;
  font-family: 'Inika', serif;
  font-size: clamp(1.15rem, 2vw, 1.45rem);
  font-weight: 700;
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

.ready-button:active {
  transform: translateY(2px);
  box-shadow:
    0 3px 0 var(--brown-dark),
    0 8px 14px rgb(0 0 0 / 18%);
}

@media (max-width: 700px) {
  .card {
    padding: 2rem 1.4rem;
  }

  img {
    width: min(380px, 82vw);
  }
}
</style>
