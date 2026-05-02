<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import creatureSitting from '@/assets/icons/blobby/creatureSitting1.png'
import creatureSittingHover from '@/assets/icons/blobby/creatureSittingDetective1.png'
import classroomBg from '@/assets/backgrounds/hjemskjerm-bg.png'
import { post } from '@/utils/apiUtils'
import { clearMeCache } from '@/utils/authRedirect'

const router = useRouter()

const classroomCode = ref('')
const waiting = ref(false)
const errorMessage = ref('')

const showHint = ref(false)

const toggleHint = () => {
  showHint.value = !showHint.value
}

const closeHint = () => {
  showHint.value = false
}

const isHovering = ref(false)

function handleInput(event: Event) {
  errorMessage.value = ''

  const target = event.target as HTMLInputElement

  let value = target.value
  value = value.replace(/[^a-zA-Z]/g, '')
  value = value.toUpperCase()
  value = value.slice(0, 6)

  classroomCode.value = value
  target.value = value
}

async function handleConfirm() {
  if (classroomCode.value.length !== 6) return

  errorMessage.value = ''
  waiting.value = true

  try {
    await post(`/api/pupil/classrooms/${classroomCode.value}/join`, {})
    clearMeCache()

    await router.push({
      path: '/join-classroom/set-avatar',
      query: {
        classroomCode: classroomCode.value,
      },
    })
  } catch (e) {
    errorMessage.value = 'Kunne ikke bli med i klasserommet. Sjekk koden og prøv igjen.'
    console.error(e)
  } finally {
    waiting.value = false
  }
}
</script>

<template>
  <main class="page" :style="{ '--page-bg': `url(${classroomBg})` }" @click="closeHint">
    <section class="join-card">
      <div class="top">
        <h1 class="title">Bli med i et klasserom</h1>
      </div>

      <div class="form">
        <label class="label" for="classroom-code">Klasseromkode:</label>

        <div class="input-wrapper">
          <div class="creature-wrapper">
            <img
              :src="showHint || isHovering ? creatureSittingHover : creatureSitting"
              alt="Figur"
              class="creature clickable-creature"
              :class="{ hover: isHovering }"
              @mouseenter="isHovering = true"
              @mouseleave="isHovering = false"
              @click.stop="toggleHint"
            />

            <div v-if="showHint" class="hint-box">
              Du skal få en klasseromskode av læreren din. Denne skriver du inn her for å kunne
              starte spillet.
            </div>
          </div>

          <input
            id="classroom-code"
            :value="classroomCode"
            @input="handleInput"
            type="text"
            class="input"
            maxlength="6"
            placeholder="******"
          />
        </div>
      </div>

      <p v-if="errorMessage" class="error-message" role="alert">
        {{ errorMessage }}
      </p>
    </section>

    <div class="buttons">
      <button
        class="btn btn-green"
        @click="handleConfirm()"
        :disabled="classroomCode.length !== 6 || waiting"
      >
        {{ waiting ? 'Sender...' : 'Bekreft' }}
      </button>
    </div>
  </main>
</template>

<style scoped>
.page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 3rem 3rem 2.5rem 3rem;
  box-sizing: border-box;
  overflow: hidden;
}

.page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: var(--page-bg);
  background-size: cover;
  background-position: center;
  filter: blur(12px);
  transform: scale(1.08);
  z-index: 0;
}

.page::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(40, 25, 10, 0.18);
  z-index: 1;
}

.page > * {
  position: relative;
  z-index: 2;
}

.join-card {
  align-self: center;
  width: min(720px, 92vw);
  margin-top: 4rem;
  padding: 2.5rem 3rem 3rem;

  background: var(--beige-light-100);

  border-radius: 1.5rem;

  border: 4px solid var(--brown-dark-100);

  box-shadow:
    0 8px 0 #2b1a10,
    0 18px 40px rgba(0, 0, 0, 0.3);
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
  font-weight: 800;
  color: black;
  text-shadow: none;
  text-align: center;
}

.form {
  display: flex;
  flex-direction: column;
  align-self: center;
  gap: 0.6rem;
  width: 100%;
  max-width: 500px;
  margin: 3rem auto 0;
}

.label {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f1208;
}

.input-wrapper {
  position: relative;
  overflow: visible;
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

.buttons {
  display: flex;
  justify-content: flex-end;
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

.btn-green:disabled {
  background: var(--green-confirm-40);
  cursor: not-allowed;
  transform: none;
}

.btn-red {
  background: var(--red-danger);
}

.btn-green {
  background: var(--green-confirm);
}

.creature {
  width: 8rem;
  height: auto;

  transform-origin: bottom right;
  transition: transform 0.2s ease;
}

.creature.hover {
  transform: scale(1.35);
}

.creature-wrapper {
  position: absolute;
  right: -2rem;
  bottom: 0.8rem;
  z-index: 5;
}

.clickable-creature {
  cursor: pointer;
  transition:
    transform 0.15s ease,
    filter 0.15s ease;
}

.clickable-creature:hover {
  content: url('@/assets/icons/blobby/creatureSittingDetective1.png');
  transform: scale(1.04);
}

.clickable-creature:active {
  transform: scale(0.98);
}

.error-message {
  align-self: center;
  width: min(520px, 90%);
  margin: 3rem auto 0;
  padding: 0.7rem 1rem;
  background: rgba(255, 255, 255, 0.92);
  border: 2px solid var(--red-danger);
  border-radius: 0.6rem;
  color: var(--color-text);
  font-weight: 600;
  text-align: center;
}

.hint-box {
  position: absolute;
  top: 0;
  right: 105%;

  width: 200px;
  padding: 0.8rem;

  background: white;
  border: 3px solid #000;
  border-radius: 0.8rem;

  font-size: 0.9rem;
  line-height: 1.3;
  color: black;

  box-shadow: 0 6px 0 #000;

  white-space: normal;
  word-break: break-word;
}

@media (max-width: 768px) {
  .buttons {
    justify-content: center;
    align-items: center;
    margin-top: 2rem;
  }
}
</style>
