<script setup lang="ts">
import { ref } from 'vue'

const step = ref<number>(1)
const email = ref<string>('')
const password = ref<string>('')
const submitted = ref<boolean>(false)

const goToPassword = (): void => {
  submitted.value = false
  step.value = 3
}

const changeAccount = (): void => {
  password.value = ''
  submitted.value = false
  step.value = 2
}

const login = (): void => {
  submitted.value = true
  console.log('Demo login:', {
    email: email.value,
    passwordLength: password.value.length,
  })
}

const goToSignup = (): void => {
  window.location.href = '/sign-up'
}
</script>

<template>
  <div class="page">
    <div class="frame">
      <div class="card-wrapper">
        <section v-if="step === 1" class="card feide-card">
          <div class="card-header">
            <div class="language-select">
              <span class="flag">🇳🇴</span>
              <span>Bokmål</span>
            </div>
          </div>

          <div class="card-body">
            <h1>Logg inn med Feide</h1>
            <p class="intro">Du må logge deg på via Feide for å få tilgang til systemet.</p>

            <div class="divider"></div>

            <div class="affiliation">
              <p class="label">Din tilhørighet</p>
              <div class="affiliation-box">
                <img src="@/assets/NTNU.png" alt="NTNU" class="affiliation-logo" />
              </div>
            </div>

            <div class="divider"></div>

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

          <div class="card-footer">
            <span>Feide leveres av Sikt</span>
          </div>
        </section>

        <section v-else-if="step === 2" class="card auth-card small-card">
          <div class="auth-body">
            <img src="@/assets/NTNU.png" alt="NTNU" class="brand-logo" />
            <h2>Logg på</h2>

            <form @submit.prevent="goToPassword">
              <input
                v-model="email"
                type="email"
                placeholder="bruker@ntnu.no"
                class="text-input"
                required
              />

              <p class="muted-link">Får du ikke tilgang til kontoen?</p>

              <div class="button-row">
                <button type="button" class="secondary-btn" @click="step = 1">Tilbake</button>
                <button type="submit" class="primary-btn">Neste</button>
              </div>

              <button type="button" class="signup-btn" @click="goToSignup">Sign up</button>
            </form>
          </div>

          <div class="auth-footer">
            <span class="key-icon">⌕</span>
            <span>Påloggingsalternativer</span>
          </div>
        </section>

        <section v-else class="card auth-card password-card">
          <div class="auth-body password-body">
            <img src="@/assets/NTNU.png" alt="NTNU" class="brand-logo" />
            <p class="signed-in-as">{{ email || 'eksempel@epost.no' }}</p>
            <h2>Skriv inn passord</h2>

            <p class="description">
              Dette er kun en visuell demo. Du kan skrive inn passord og trykke på knappen, men
              innloggingen er ikke koblet til noen ekte tjeneste.
            </p>

            <form @submit.prevent="login">
              <input
                v-model="password"
                type="password"
                placeholder="Passord"
                class="text-input"
                required
              />

              <div class="link-list">
                <span>Jeg har glemt passordet</span>
                <span>Bruk et sertifikat eller et smartkort</span>
                <span class="clickable" @click="changeAccount">Logg på med en annen konto</span>
              </div>

              <div class="login-button-row">
                <button type="submit" class="primary-btn">Logg på</button>
              </div>
            </form>

            <p v-if="submitted" class="demo-message">Demo: Innlogging registrert lokalt.</p>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: rgb(239 211 175 / 100%);
  font-family: Arial, Helvetica, sans-serif;
}

.frame,
.card-wrapper {
  width: 100%;
  display: flex;
  justify-content: center;
}

.card {
  background: #ffffff;
  border: none;
  box-shadow: 0 0 0 3px #dfe6ef inset;
}

.feide-card {
  width: 390px;
  overflow: hidden;
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

.chevron {
  font-size: 16px;
  margin-left: 2px;
}

.card-body {
  padding: 20px 14px 18px;
}

h1 {
  margin: 0 0 16px;
  font-size: 24px;
  line-height: 1.2;
  color: #2f2f2f;
}

h2 {
  margin: 0 0 14px;
  font-size: 20px;
  font-weight: 600;
  color: #1f1f1f;
}

.intro {
  margin: 0 0 14px;
  padding-left: 58px;
  max-width: 320px;
  font-size: 14px;
  line-height: 1.45;
  color: #333;
}

.divider {
  height: 1px;
  background: #d7d7d7;
  margin: 14px 0;
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

.affiliation-name {
  font-size: 16px;
  color: #262626;
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

.microsoft span:nth-child(1) {
  background: #f25022;
}
.microsoft span:nth-child(2) {
  background: #7fba00;
}
.microsoft span:nth-child(3) {
  background: #00a4ef;
}
.microsoft span:nth-child(4) {
  background: #ffb900;
}

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

.card-footer {
  background: #dfe6ef;
  padding: 22px 16px;
  text-align: center;
  font-size: 13px;
  color: #54606b;
}

.auth-card {
  background: #ffffff;
  border: 1px solid #666;
  box-shadow: none;
}

.small-card {
  width: 340px;
}

.password-card {
  width: 340px;
  box-shadow: 0 0 0 3px #dfe6ef inset;
  border-color: #d6d6d6;
}

.auth-body {
  padding: 20px 34px 30px;
}

.password-body {
  padding-top: 34px;
  padding-bottom: 32px;
}

.brand-logo {
  height: 36px;
  margin-bottom: 18px;
}

.text-input {
  width: 100%;
  border: none;
  border-bottom: 1px solid #7aa7dd;
  background: transparent;
  font-size: 14px;
  padding: 10px 0 8px;
  outline: none;
  margin-bottom: 14px;
}

.muted-link,
.link-list span {
  display: block;
  font-size: 12px;
  color: #1967b3;
}

.muted-link {
  margin: 0 0 20px;
}

.link-list {
  display: grid;
  gap: 14px;
  margin-top: 4px;
}

.clickable {
  cursor: pointer;
}

.button-row {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  margin-top: 24px;
}

.secondary-btn,
.primary-btn,
.signup-btn {
  min-height: 42px;
  border: none;
  padding: 0 18px;
  font-size: 14px;
  cursor: pointer;
}

.secondary-btn {
  background: #d9d9d9;
  color: #2a2a2a;
  min-width: 82px;
}

.primary-btn {
  background: #0067b8;
  color: white;
  min-width: 84px;
}

.signup-btn {
  width: 100%;
  margin-top: 14px;
  border: 1px solid #8a8f99;
  background: #ffffff;
  color: #2c2c2c;
}

.auth-footer {
  border-top: 1px solid #d5d5d5;
  padding: 14px 34px;
  display: flex;
  gap: 12px;
  align-items: center;
  font-size: 14px;
  color: #333;
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
  .feide-card,
  .small-card,
  .password-card {
    width: 100%;
    max-width: 390px;
  }

  .auth-body {
    padding-left: 22px;
    padding-right: 22px;
  }
}
</style>
