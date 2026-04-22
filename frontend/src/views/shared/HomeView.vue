<script setup lang="ts"></script>

<template>
    <div class="page">
      <div class="center-container">
        <img src="/src/assets/nettdetektivene_logo.png" alt ="Logo" />
        <router-link to="/sign-in">
          <button class="button">Logg inn</button>
        </router-link>
      </div>
    </div>
</template>

<style scoped>
.page{
  display: grid;
  place-items: center;
  height: 100%;
  background-color: var(--beige-light);
}

img {
  width: 670px;
}
.button {
  background-color: var(--green-confirm);
  color: var(--color-text);
  border: 7px solid var(--color-text);
  padding: 2rem 2rem;
  border-radius: 38px;
  font-weight: 600;
  font-size: 50px;
  box-shadow: -10px 8px 8px rgba(0,0,0,0.4);
}
.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
}
</style>
<style>
</style>
