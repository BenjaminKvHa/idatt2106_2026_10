<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ClipboardList, Lock } from '@lucide/vue'

import aliImg from '@/assets/icons/detectives_and_thief/detective_ali.png'
import suryaImg from '@/assets/icons/detectives_and_thief/detective_surya.png'
import villainImg from '@/assets/icons/detectives_and_thief/Datatyven.png'
import bgImg from '@/assets/backgrounds/welcomeBackground.png'

const router = useRouter()
const activeCharacter = ref<string | null>(null)

function confirm(): void {
  router.push('/pupil')
}

function toggleCharacter(character: string): void {
  activeCharacter.value = activeCharacter.value === character ? null : character
}
</script>

<template>
  <div class="center-container" :style="{ backgroundImage: `url(${bgImg})` }">
    <div class="scene">
      
      <div class="heroes">
        <div class="character ali" @click="toggleCharacter('ali')">
          <span class="character-name">Detektiv Ali</span>
          <img :src="aliImg" alt="Detektiv Ali" />

          <div v-if="activeCharacter === 'ali'" class="speech">
            Hei, jeg er Detektiv Ali, og trenger din hjelp til å bekjempe datatyven!
          </div>
        </div>

        <div class="character surya" @click="toggleCharacter('surya')">
          <span class="character-name">Detektiv Surya</span>
          <img :src="suryaImg" alt="Detektiv Surya" />

          <div v-if="activeCharacter === 'surya'" class="speech">
            Hei, jeg er Detektiv Surya, og trenger din hjelp til å bekjempe datatyven!
          </div>
        </div>
      </div>

      <div class="villain" @click="toggleCharacter('villain')">
        <span class="character-name villain-name">Datatyven</span>
        <img :src="villainImg" alt="Datatyven" />

        <div v-if="activeCharacter === 'villain'" class="speech villain-speech">
          Dere klarer aldri å stoppe meg!
        </div>
      </div>

      <div class="card">
        <div class="secret-badge">
          <Lock :size="14" /> Hemmelig oppdrag
        </div>

        <h1>Velkommen, nettdetektiv!</h1>
        <div class="divider"></div>

        <div class="text">
          <span>Akkurat nå er byen din under angrep av <strong>datatyven</strong>.
            Detektiv Ali og Detektiv Surya trenger din hjelp til å bekjempe datatyven.</span>
        </div>

        <div class="mission-box">
          <div class="mission-label">
            <ClipboardList :size="13" /> Ditt oppdrag
          </div>
          <div class="steps">
            <div class="step">
              <span class="step-num">1</span>
              <span>Lær å skille <strong>falsk</strong> fra <strong>ekte</strong> innhold på internett</span>
            </div>
            <div class="step">
              <span class="step-num">2</span>
              <span>Løs oppgaver og samle bevis underveis</span>
            </div>
            <div class="step">
              <span class="step-num">3</span>
              <span>Klar alle nivåene og <strong>bekjemp datatyven</strong>!</span>
            </div>
          </div>
        </div>

        <button class="ready-button" @click="confirm">
          Start spillet
        </button>
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

.heroes {
  display: flex;
  align-items: flex-end;
  gap: 0.65rem;
  position: absolute;
  left: 3%;
  bottom: 10%;
  z-index: 1;
}

.character {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  position: relative;
  width: fit-content;
  padding: 0.55rem 0.15rem 0.35rem;
  border-radius: 28px;
  background: var(--beige-light-20);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 30px var(--brown-dark-20);
  overflow: visible;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.character:hover {
  transform: translateY(-6px) scale(1.02);
}

.character:hover::after {
  content: '';
  position: absolute;
  left: 50%;
  top: -60px;
  transform: translateX(-50%);
  width: 140%;
  height: 160%;
  border-radius: 50%;
  background: radial-gradient(
    ellipse at top,
    var(--yellow-award-40) 0%,
    var(--yellow-award-20) 25%,
    transparent 70%
  );
  filter: blur(25px);
  z-index: 0;
  pointer-events: none;
  animation: spotlightDrop 0.25s ease;
}

.character::before {
  content: '';
  position: absolute;
  width: 75%;
  height: 62%;
  bottom: 8%;
  border-radius: 50%;
  background: var(--yellow-award-20);
  filter: blur(36px);
  opacity: 0.9;
  z-index: 0;
  animation: glowPulse 4s ease-in-out infinite;
}

.character img {
  position: relative;
  z-index: 1;
  display: block;
  margin-inline: -1.4rem;
  margin-bottom: -0.25rem;
  filter:
    drop-shadow(0 6px 0 var(--brown-dark-60))
    drop-shadow(0 0 12px var(--beige-light-40));
}

.ali img {
  width: 250px;
}

.surya img {
  width: 250px;
}

.villain {
  right: 15%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  position: absolute;
  z-index: 1;
  width: fit-content;
  padding: 0.6rem 0.2rem 0.45rem;
  border-radius: 32px;
  background: var(--red-danger-10);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 30px var(--brown-dark-20);
  overflow: visible;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.villain:hover {
  transform: translateY(-6px) scale(1.02);
}

.villain:hover::after {
  content: '';
  position: absolute;
  left: 50%;
  top: -60px;
  transform: translateX(-50%);
  width: 140%;
  height: 160%;
  border-radius: 50%;
  background: radial-gradient(
    ellipse at top,
    var(--red-danger-40) 0%,
    var(--red-danger-20) 25%,
    transparent 70%
  );
  filter: blur(25px);
  z-index: 0;
  pointer-events: none;
  animation: spotlightDrop 0.25s ease;
}

.villain::before {
  content: '';
  position: absolute;
  width: 82%;
  height: 72%;
  bottom: 4%;
  border-radius: 50%;
  background: var(--red-danger-20);
  filter: blur(46px);
  opacity: 0.9;
  z-index: 0;
  animation: villainPulse 3.5s ease-in-out infinite;
}

.villain img {
  width: 250px;
  object-fit: contain;
  position: relative;
  z-index: 1;
  display: block;
  margin-inline: -1.2rem;
  margin-bottom: -0.3rem;
}

.character-name {
  position: relative;
  z-index: 2;
  font-family: 'Inika', serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--brown-dark);
  text-align: center;
  background: var(--beige-light-80);
  backdrop-filter: blur(4px);
  border-radius: 20px;
  padding: 3px 10px;
  white-space: nowrap;
  box-shadow: -2px 2px 0 var(--brown-dark-60);
}

.villain-name {
  color: var(--red-danger);
}

.speech {
  position: absolute;
  bottom: calc(100% + 14px);
  left: 50%;
  transform: translateX(-50%);
  width: 210px;
  background: var(--beige-light);
  color: var(--color-text);
  padding: 10px 14px;
  border-radius: 20px;
  font-size: 13px;
  text-align: center;
  box-shadow: -3px 3px 0 var(--brown-dark);
  z-index: 999;
  pointer-events: none;
  animation: popIn 0.2s ease;
}

.speech::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  border-width: 8px;
  border-style: solid;
  border-color: var(--beige-light) transparent transparent transparent;
}

.villain-speech {
  background: var(--beige-light);
  color: var(--color-text);
}

.villain-speech::after {
  border-color: var(--beige-light) transparent transparent transparent;
}

.card {
  position: relative;
  background: var(--beige-light-100);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  box-shadow: -3px 3px 6px var(--brown-dark);
  max-width: 540px;
  width: 540px;
  padding: 3rem 2.5rem 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.4rem;
  z-index: 2;
}

.secret-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  position: absolute;
  top: -20px;
  background: var(--yellow-award);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  font-family: 'Inika', serif;
  font-size: 14px;
  font-weight: 700;
  padding: 5px 20px;
}

h1 {
  font-size: 2.2rem;
  margin: 0;
  text-align: center;
  line-height: 1.2;
}

.divider {
  width: 60%;
  height: 3px;
  background: var(--brown-medium);
  border-radius: 5px;
  opacity: 0.35;
}

.text {
  text-align: center;
  font-size: 1.2rem;
  line-height: 1.6;
  padding: 0 0.5rem;
}

.mission-box {
  background: var(--beige-medium);
  border: var(--border-wide) solid var(--brown-medium);
  border-radius: 40px;
  box-shadow: -3px 3px 6px var(--brown-dark);
  padding: 1.3rem 1.5rem;
  width: 100%;
}

.mission-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: 'Inika', serif;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.8px;
  text-transform: uppercase;
  margin-bottom: 10px;
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.step {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 16px;
  line-height: 1.5;
}

.step-num {
  background: var(--brown-dark);
  color: var(--beige-light);
  border-radius: 50%;
  min-width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.ready-button {
  margin-top: 0.4rem;
  padding: 0.9rem 3rem;
  background: var(--green-confirm);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  font-family: 'Inika', serif;
  font-size: 1.35rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: -3px 3px 6px var(--brown-dark);
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

@keyframes glowPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.85;
  }
  50% {
    transform: scale(1.08);
    opacity: 1;
  }
}

@keyframes villainPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.85;
  }
  50% {
    transform: scale(1.12);
    opacity: 1;
  }
}

@keyframes spotlightDrop {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(8px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}
</style>
