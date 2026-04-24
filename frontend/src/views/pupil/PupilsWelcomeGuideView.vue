<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Search, Monitor, UserSearch, ClipboardList, Lock } from '@lucide/vue'

const router = useRouter()

function confirm(): void {
  router.push('/pupil')
}
</script>

<template>
  <div class="center-container">
    <div class="card">
      <div class="secret-badge">
        <Lock :size="14" /> Hemmelig oppdrag
      </div>

      <div class="icon-row">
        <div class="icon-box">
          <Monitor :size="30" :stroke-width="1.75" />
        </div>
        <div class="icon-box big">
          <Search :size="40" :stroke-width="1.75" />
        </div>
        <div class="icon-box">
          <UserSearch :size="30" :stroke-width="1.75" />
        </div>
      </div>

      <h1>Velkommen, nettdetektiv!</h1>
      <div class="divider"></div>

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
</template>

<style scoped>
.center-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3rem 1.5rem;
}

.card {
  position: relative;
  background: var(--beige-light);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  box-shadow: -3px 3px 6px var(--brown-dark);
  max-width: 540px;
  width: 100%;
  padding: 3rem 2.5rem 2.5rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.4rem;
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
  color: var(--brown-dark);
  padding: 5px 20px;
  box-shadow: -3px 3px 6px var(--brown-dark);
}

.icon-row {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  margin-top: 0.8rem;
}

.icon-box {
  background: var(--beige-medium);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 40px;
  box-shadow: -3px 3px 6px var(--brown-dark);
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  transform: rotate(-4deg);
}

.icon-box:last-child {
  transform: rotate(5deg);
}

.icon-box.big {
  width: 84px;
  height: 84px;
  font-size: 44px;
  background: var(--orange-medium);
  transform: rotate(0deg);
  border-radius: 50%;
}

h1 {
  font-size: 2.2rem;
  color: var(--brown-dark);
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
  color: var(--brown-medium);
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
  color: var(--brown-dark);
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
  color: var(--brown-dark);
  cursor: pointer;
  box-shadow: -3px 3px 6px var(--brown-dark);
  transition: transform 0.1s, box-shadow 0.1s;
}

.ready-button:hover {
  transform: translate(-2px, -2px);
  box-shadow: -3px 3px 6px var(--brown-dark);
}

.ready-button:active {
  transform: translate(3px, 3px);
  box-shadow: -3px 3px 6px var(--brown-dark);
}
</style>
