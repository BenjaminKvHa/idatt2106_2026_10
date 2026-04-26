<script setup lang="ts">

</script>

<template>
  <div class="center-container">
    <div class="scene">

      <div class="card">
        <img src="/src/assets/nettdetektivene_logo.png" alt="Nettdetektivene logo" />
        <p class="tagline">Er du klar for oppdraget?</p>
        <router-link to="/sign-in" class="link">
          <button class="ready-button">
            Logg inn
          </button>
        </router-link>
      </div>

    </div>
  </div>
</template>

<style scoped>
.center-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 3rem 1.5rem;
  overflow: hidden;
  background-image: url('/src/assets/welcomeBackground.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.scene {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 100%;
  z-index: 1;
}

.card {
  position: relative;
  background: var(--beige-light-100);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  box-shadow: -3px 3px 3px var(--brown-dark);
  max-width: 600px;
  width: 600px;
  padding: 3rem 3rem 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.4rem;
  z-index: 2;
}

img {
  width: 520px;
  max-width: 85vw;
}


.tagline {
  font-family: 'Inika', serif;
  font-size: 14px;
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
  margin-top: 0.4rem;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0.9rem 3rem;
  background: var(--green-confirm);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  font-family: 'Inika', serif;
  font-size: 1.35rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: -3px 3px 3px var(--brown-dark);
  transition: transform 0.1s, box-shadow 0.1s;
}

.ready-button:hover {
  transform: translate(-2px, -2px);
  box-shadow: -5px 5px 0 var(--brown-dark);
}

.ready-button:active {
  transform: translate(3px, 3px);
  box-shadow: -1px 1px 0 var(--brown-dark);
}

@media (max-width: 700px) {
  .card {
    width: 100%;
    padding: 2.5rem 1.5rem 2rem;
  }

  img {
    width: 420px;
  }
}
</style>
