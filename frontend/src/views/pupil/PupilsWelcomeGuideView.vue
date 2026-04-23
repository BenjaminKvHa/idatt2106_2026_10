<script setup lang="ts">

import { useRouter } from 'vue-router'

const router = useRouter()

function confirm(): void {
  router.push('/pupil');
}

</script>

<template>
  <div class="center-container">
    <div class="welcome-text">
      <img src="/src/assets/icons/magnifying-glass.png" class="image"/>
      <h1>Velkommen, detektiv!</h1>
      <p>I dette spillet skal du lære å skille
        falsk fra ekte innhold på internett.
        Oppdraget ditt er å løse oppgaver og til slutt bekjempe datatyven.
        Er du klar for å prøve deg som nett-detektiv?
      </p>
    </div>
    <button class="ready-button" @click="confirm">
      Start spillet
    </button>
  </div>

</template>

<style scoped>
.center-container{
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
}
.welcome-text{
  margin-top: 5%;
  text-align: center;
  padding-top: 1.5rem;
  width: 40%;
  white-space: pre-wrap;
  font-size: 30px;
}
.ready-button{
  margin-top: 1.5rem;
  padding: 0.5rem 1rem;
  background: var(--green-confirm);
  border: 5px solid var(--brown-dark);
  border-radius: 40px;
  font-size: 30px;
  min-width: 20%;
}

.image{
  margin-bottom: 1rem;
  max-width: 150px;
}
</style>
