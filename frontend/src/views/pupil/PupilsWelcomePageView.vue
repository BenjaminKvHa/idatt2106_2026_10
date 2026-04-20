<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goToHome(): void {
  router.push('/pupil');
}

function goBack(): void {
  router.push('/join-classroom/enter-classroom-code');
}
</script>

<template>
  <div class="page">
    <div class="center-container">
      <h1>Velkommen!</h1>
      <p>Du venter nå på at en lærer skal godkjenne forespørselen din om å bli med i klasserommet.</p>
      <p>I klasserommet skal dere kjempe om å få flere poeng enn andre klasserom, og</p>
      <p>kjempe mot dem om å være de beste nett-detektivene.</p>
      <p>Hvis det tar veldig lang tid å slippe inn i klasserommet må du kontakte læreren din.</p>
      <div class="button-container">
        <button class="cancel-button" @click="goBack">Kanseller forespørsel</button>
        <button class="next-button" @click="goToHome">Gå til hjemskjerm (kun for funksjonalitet, skal ikke være med)</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  height: 100vh;
  box-sizing: border-box;
  overflow: hidden;
}
.center-container{
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 1400px;
}

.center-container h1{
  font-size: 50px;
}

.center-container p{
  margin: 5px;
  font-size: 30px;
}

.button-container{
  margin-top: 40px;
  gap: 2rem;
  display: flex;
  flex-direction: row;
  align-items: center;
  text-align: center;
  justify-content: center;
  width: 100%;
  max-width: 1400px;
}

.next-button{
  margin: 00;
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--orange-medium);
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, filter 0.2s ease;
}
.cancel-button{
  margin: 0;
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--red-danger);
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, filter 0.2s ease;
}
</style>
