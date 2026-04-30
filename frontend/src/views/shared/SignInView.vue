<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { loginWithCredentials } from '@/utils/apiUtils'
import { clearMeCache } from '@/router'
import { decodeJwtPayload } from '@/utils/jwtUtils'

const router = useRouter()
const step = ref<number>(1)
const email = ref<string>('')
const password = ref<string>('')
const submitted = ref<boolean>(false)
const loading = ref<boolean>(false)
const error = ref<string>('')

const goToPassword = (): void => {
  error.value = ''
  submitted.value = false
  step.value = 3
}

const changeAccount = (): void => {
  password.value = ''
  error.value = ''
  submitted.value = false
  step.value = 2
}

const login = async (): Promise<void> => {
  error.value = ''
  submitted.value = false
  loading.value = true

  try {
    const accessToken = await loginWithCredentials({
      email: email.value.trim(),
      password: password.value,
    })

    const payload = decodeJwtPayload(accessToken)
    submitted.value = true
    clearMeCache()
    await router.push(payload?.role === "PUPIL" ? "/pupil" : "/join-classroom/enter-classroom-code")
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : 'Kunne ikke logge inn.'
  } finally {
    loading.value = false
  }
}

const goToSignup = (): void => {
  router.push('/sign-up')
}
</script>

<template>
  <div class="feide-page">
    <section v-if="step === 1" class="landing-card">
      <div class="card-header">
        <div class="language-select">
          <span class="flag">🇳🇴</span>
          <span>Bokmål</span>
        </div>
      </div>

      <div class="card-body">
        <h1>Logg inn med Feide</h1>
        <p class="intro">Du må logge deg på via Feide for å få tilgang til systemet.</p>

        <div class="feide-divider"></div>

        <div class="affiliation">
          <p class="label">Din tilhørighet</p>
          <div class="affiliation-box">
            <img src="@/assets/logo/NTNU.png" alt="NTNU" class="affiliation-logo" />
          </div>
        </div>

        <div class="feide-divider"></div>

        <button class="option-button primary-option" @click="step = 2">
          <span class="icon microsoft">
            <span></span><span></span><span></span><span></span>
          </span>
          <span>Bruk arbeids- eller skolekonto</span>
        </button>

        <button class="option-button disabled-option" type="button" disabled>
          <span class="icon id-porten">⬒</span>
          <span>Logg inn med ID-porten</span>
        </button>

        <div class="help-row">
          <span>Trenger du hjelp?</span>
          <span class="plus">+</span>
        </div>
      </div>

      <div class="landing-footer">
        <span>Feide leveres av Sikt</span>
      </div>
    </section>

    <section v-else-if="step === 2" class="feide-card">
      <div class="feide-card-body auth-step-body">
        <img src="@/assets/logo/NTNU.png" alt="NTNU" class="brand-logo" />
        <h2>Logg på</h2>

        <form @submit.prevent="goToPassword">
          <input
            v-model="email"
            type="email"
            placeholder="bruker@ntnu.no"
            class="feide-input"
            required
          />

          <p class="feide-muted-link">Får du ikke tilgang til kontoen?</p>

          <div class="feide-button-row">
            <button type="button" class="feide-btn-secondary" @click="step = 1">Tilbake</button>
            <button type="submit" class="feide-btn-primary">Neste</button>
          </div>

          <button type="button" class="signup-btn" @click="goToSignup">Sign up</button>
        </form>
      </div>

      <div class="feide-card-footer auth-key-footer">
        <span class="key-icon">⌕</span>
        <span>Påloggingsalternativer</span>
      </div>
    </section>

    <section v-else class="feide-card password-card">
      <div class="feide-card-body password-body">
        <img src="@/assets/logo/NTNU.png" alt="NTNU" class="brand-logo" />
        <p class="signed-in-as">{{ email || 'eksempel@epost.no' }}</p>
        <h2>Skriv inn passord</h2>

        <p class="description">
          Skriv inn passordet ditt for å logge inn på den valgte kontoen.
        </p>

        <form @submit.prevent="login">
          <input
            v-model="password"
            type="password"
            placeholder="Passord"
            class="feide-input"
            required
          />

          <div class="feide-link-list">
            <span>Jeg har glemt passordet</span>
            <span>Bruk et sertifikat eller et smartkort</span>
            <span class="clickable" @click="changeAccount">Logg på med en annen konto</span>
          </div>

          <div class="login-button-row">
            <button type="submit" class="feide-btn-primary" :disabled="loading">
              {{ loading ? 'Logger inn...' : 'Logg på' }}
            </button>
          </div>
        </form>

        <p v-if="error" class="feide-status-error" role="alert">{{ error }}</p>
        <p v-else-if="submitted" class="demo-message">Innlogging registrert.</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
@import '@/assets/styles/feideStyle.css';
* {
  box-sizing: border-box;
}

.landing-card {
  width: 390px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 0 0 3px #dfe6ef inset;
}

.card-header {
  background: #dfe6ef;
  padding: 14px 16px;
  display: flex;
  justify-content: flex-end;
}

.language-select {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
  font-size: 14px;
}

.flag {
  font-size: 15px;
}

.card-body {
  padding: 20px 14px 18px;
}

h1 {
  margin: 0 0 16px;
  font-size: 24px;
  line-height: 1.2;
  color: #2f2f2f;
  font-family: Arial, Helvetica, sans-serif;
}

h2 {
  margin: 0 0 14px;
  font-size: 20px;
  font-weight: 600;
  color: #1f1f1f;
  font-family: Arial, Helvetica, sans-serif;
}

.intro {
  margin: 0 0 14px;
  padding-left: 58px;
  max-width: 320px;
  font-size: 14px;
  line-height: 1.45;
  color: #333;
}

.label {
  margin: 0 0 10px;
  font-size: 13px;
  color: #333;
}

.affiliation-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.affiliation-logo {
  height: 34px;
  width: auto;
}

.option-button {
  width: 100%;
  min-height: 50px;
  border: 1px solid #8a8f99;
  background: white;
  margin-bottom: 14px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: #2c2c2c;
}

.primary-option {
  cursor: pointer;
}

.primary-option:hover,
.signup-btn:hover {
  background: #f4f7fb;
}

.disabled-option {
  cursor: default;
  opacity: 1;
}

.icon {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.microsoft {
  display: grid;
  grid-template-columns: repeat(2, 9px);
  grid-template-rows: repeat(2, 9px);
  gap: 2px;
}

.microsoft span:nth-child(1) { background: #f25022; }
.microsoft span:nth-child(2) { background: #7fba00; }
.microsoft span:nth-child(3) { background: #00a4ef; }
.microsoft span:nth-child(4) { background: #ffb900; }

.id-porten {
  font-size: 15px;
}

.help-row {
  margin-top: 8px;
  padding: 10px 12px 0;
  border-top: 1px solid #d7d7d7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #2b2b2b;
  font-size: 14px;
}

.plus {
  font-size: 34px;
  line-height: 1;
  color: #28428f;
}

.landing-footer {
  background: #dfe6ef;
  padding: 22px 16px;
  text-align: center;
  font-size: 13px;
  color: #54606b;
}

.password-card {
  box-shadow: 0 0 0 3px #dfe6ef inset;
  border-color: #d6d6d6;
}

.auth-step-body {
  padding-top: 20px;
}

.password-body {
  padding-top: 34px;
  padding-bottom: 32px;
}

.feide-input {
  margin-bottom: 14px;
}

.clickable {
  cursor: pointer;
}

.signup-btn {
  width: 100%;
  min-height: 42px;
  margin-top: 14px;
  border: 1px solid #8a8f99;
  background: #ffffff;
  color: #2c2c2c;
  font-size: 14px;
  cursor: pointer;
  padding: 0 18px;
}

.auth-key-footer {
  gap: 12px;
}

.key-icon {
  font-size: 18px;
}

.signed-in-as {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: #111;
}

.description {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.4;
  color: #444;
}

.login-button-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.demo-message {
  margin-top: 16px;
  font-size: 13px;
  color: #1f5d1f;
}

@media (max-width: 480px) {
  .landing-card {
    width: 100%;
    max-width: 390px;
  }
}
</style>
