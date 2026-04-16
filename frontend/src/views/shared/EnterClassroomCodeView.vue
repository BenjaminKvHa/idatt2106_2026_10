<script setup lang="ts">
import { ref } from 'vue'
import creatureSitting from '@/assets/creatureSitting.png'

const classroomCode = ref('')

function handleLogout() {
  console.log('Logg ut')
}

function handleConfirm() {
  console.log('Klasseromkode:', classroomCode.value)
}
</script>

<template>
  <main class="page">
    <div class="top">
      <h1 class="title">Bli med i et klasserom</h1>
    </div>

    <div class="form">
      <label class="label" for="classroom-code">Klasseromkode:</label>
      <div class="input-wrapper">
        <img :src="creatureSitting" alt="Figur" class="creature" />
        <input id="classroom-code" v-model="classroomCode" type="text" class="input" />
      </div>
    </div>

    <div class="buttons">
      <button class="btn btn-red" @click="handleLogout">Logg ut</button>
      <button class="btn btn-green" @click="handleConfirm">Bekreft</button>
    </div>
  </main>
</template>

<style scoped>
.page {
  width: 100%;
  min-height: 100vh;
  background: var(--beige-medium);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 3rem 3rem 2.5rem 3rem;
  box-sizing: border-box;
}

.top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3rem;
}

.title {
  margin: 0;
  font-size: 3rem;
  font-weight: 700;
  color: var(--color-text);
  align-self: flex-start;
}

.form {
  display: flex;
  flex-direction: column;
  align-self: center;
  gap: 0.6rem;
  width: 500px;
}

.label {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
}

.input-wrapper {
  position: relative;
}

.input {
  width: 100%;
  height: 3.5rem;
  padding: 0 1rem;
  font-size: 1.3rem;
  font-family: inherit;
  border: 2px solid var(--brown-dark);
  border-radius: 0.5rem;
  background: white;
  color: var(--color-text);
  box-sizing: border-box;
}

.input:focus {
  outline: none;
  box-shadow: 0 0 0 2px var(--orange-medium);
}

.creature {
  position: absolute;
  right: -3rem;
  bottom: 0.6rem;
  width: 15rem;
  height: auto;
  pointer-events: none;
}

.buttons {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.btn {
  min-width: 13rem;
  min-height: 5rem;
  padding: 0.75rem 2rem;
  font-family: inherit;
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--color-text);
  border: 3px solid var(--color-text);
  border-radius: 1rem;
  box-shadow: 0 5px 0 var(--color-text);
  cursor: pointer;
}

.btn-red:hover {
  background: var(--red-danger-80);
}

.btn-green:hover {
  background: var(--green-confirm-60);
}

.btn:active {
  transform: translateY(3px);
  box-shadow: 0 2px 0 var(--color-text);
}

.btn-red {
  background: var(--red-danger);
}

.btn-green {
  background: var(--green-confirm);
}
</style>
