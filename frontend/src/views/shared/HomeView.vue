<script setup lang="ts">
import { Lock, Search, Monitor, Shield, FileText, AlertCircle } from '@lucide/vue'
</script>

<template>
  <div class="page">
    <div class="float f1"><Search /></div>
    <div class="float f2"><Monitor /></div>
    <div class="float f3"><Shield /></div>
    <div class="float f4"><FileText /></div>
    <div class="float f5"><AlertCircle /></div>
    <div class="float f6"><Lock /></div>

    <div class="center-container">
      <div class="logo-box">
        <img src="/src/assets/nettdetektivene_logo.png" alt="Logo" />
        <p class="tagline">Er du klar for oppdraget?</p>
      </div>

      <router-link to="/sign-in" class="link">
        <button class="button">
          <Lock />
          Logg inn
        </button>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
  position: relative;
  z-index: 1;
}

.float {
  position: absolute;
  background: var(--beige-light);
  border-radius: 20px;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--brown-dark);
}

.float svg {
  width: 50px;
  height: 50px;
  stroke-width: 1.75;
}

.button svg {
  width: 32px;
  height: 32px;
  stroke-width: 2.2;
}

.f1 { top: 8%;     left: 8%;   transform: rotate(-8deg); }
.f2 { top: 12%;    right: 8%;  transform: rotate(6deg); }
.f3 { bottom: 20%; left: 6%;   transform: rotate(5deg); }
.f4 { bottom: 18%; right: 7%;  transform: rotate(-7deg); }
.f5 { top: 40%;    left: 4%;   transform: rotate(10deg);  width: 72px; height: 72px; background: var(--yellow-award); }
.f6 { top: 38%;    right: 4%;  transform: rotate(-9deg);  width: 72px; height: 72px; background: var(--orange-medium); }

.logo-box {
  background: var(--beige-light);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 32px;
  box-shadow: -3px 3px 6px var(--brown-dark);
  padding: 2.5rem 3rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

img {
  width: 520px;
  max-width: 75vw;
}

.tagline {
  font-family: 'Inika', serif;
  font-size: 13px;
  font-weight: 700;
  color: var(--brown-medium);
  letter-spacing: 2px;
  text-transform: uppercase;
  margin: 0;
}

.link {
  text-decoration: none;
}

.button {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--green-confirm);
  color: var(--brown-dark);
  border: var(--border-wide) solid var(--brown-dark);
  padding: 1.1rem 4rem;
  border-radius: 40px;
  font-family: 'Inika', serif;
  font-weight: 700;
  font-size: 2rem;
  cursor: pointer;
  box-shadow: -3px 3px 6px var(--brown-dark);
  transition: transform 0.1s, box-shadow 0.1s;
}

.button:hover {
  transform: translate(-2px, -2px);
  box-shadow: -3px 3px 6px var(--brown-dark);
}

.button:active {
  transform: translate(2px, 2px);
  box-shadow: -3px 3px 6px var(--brown-dark);
}
</style>
