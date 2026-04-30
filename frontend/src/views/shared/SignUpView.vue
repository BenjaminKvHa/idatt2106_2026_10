<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import type { BackendAccountRole } from '@/utils/apiUtils'
import { signupWithCredentials } from '@/utils/apiUtils'
import { decodeJwtPayload, getDefaultRouteForRole } from '@/utils/jwtUtils'

type RoleOption = 'Lærer' | 'Elev'

const router = useRouter()
const legalName = ref<string>('')
const email = ref<string>('')
const password = ref<string>('')
const confirmPassword = ref<string>('')
const role = ref<RoleOption>('Lærer')
const error = ref<string>('')
const loading = ref<boolean>(false)
const submitted = ref<boolean>(false)

const BACKEND_ROLE_BY_LABEL: Record<RoleOption, BackendAccountRole> = {
  Lærer: 'TEACHER',
  Elev: 'PUPIL',
}

const validatePassword = (pwd: string): string | null => {
  if (pwd.length < 8) return 'Passordet må være minst 8 tegn.'
  if (!/[A-ZÆØÅ]/.test(pwd)) return 'Passordet må inneholde minst én stor bokstav.'
  if (!/[0-9]/.test(pwd)) return 'Passordet må inneholde minst ett tall.'
  return null
}

const handleRegister = async (): Promise<void> => {
  error.value = ''
  submitted.value = false

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!legalName.value.trim()) {
    error.value = 'Du må skrive inn fullt navn.'
    return
  }

  if (!emailRegex.test(email.value)) {
    error.value = 'Du må skrive inn en gyldig e-postadresse.'
    return
  }

  const passwordError = validatePassword(password.value)
  if (passwordError) {
    error.value = passwordError
    return
  }

  if (password.value !== confirmPassword.value) {
    error.value = 'Passordene er ikke like.'
    return
  }

  loading.value = true

  try {
    const accessToken = await signupWithCredentials({
      email: email.value.trim(),
      password: password.value,
      legalName: legalName.value.trim(),
      role: BACKEND_ROLE_BY_LABEL[role.value],
    })

    const payload = decodeJwtPayload(accessToken)
    submitted.value = true

    legalName.value = ''
    email.value = ''
    password.value = ''
    confirmPassword.value = ''
    role.value = 'Lærer'

    await router.push(getDefaultRouteForRole(payload?.role, payload?.email))
  } catch (caughtError) {
    error.value = caughtError instanceof Error ? caughtError.message : 'Kunne ikke opprette bruker.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="feide-page">
    <div class="feide-card">
      <div class="feide-card-body">
        <img src="@/assets/logo/NTNU.png" alt="NTNU" class="brand-logo" />

        <div class="header-text">
          <h1>Opprett bruker</h1>
          <p class="subtitle">Bli en del av NTNU</p>
        </div>

        <form @submit.prevent="handleRegister" novalidate>
          <div class="feide-form-group">
            <label for="reg-legal-name">Fullt navn</label>
            <input
              id="reg-legal-name"
              v-model="legalName"
              type="text"
              autocomplete="name"
              placeholder="Ola Nordmann"
              required
              class="feide-input"
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error' : undefined"
            />
          </div>

          <div class="feide-form-group">
            <label for="reg-email">E-post</label>
            <input
              id="reg-email"
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="navn@ntnu.no"
              required
              class="feide-input"
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error' : undefined"
            />
          </div>

          <div class="feide-form-group">
            <label for="reg-password">Passord</label>
            <input
              id="reg-password"
              v-model="password"
              type="password"
              autocomplete="new-password"
              placeholder="Minst 8 tegn"
              required
              class="feide-input"
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error password-help' : 'password-help'"
            />
            <ul id="password-help" class="password-requirements">
              <li :class="{ met: password.length >= 8 }">Minst 8 tegn</li>
              <li :class="{ met: /[A-ZÆØÅ]/.test(password) }">Minst én stor bokstav</li>
              <li :class="{ met: /[0-9]/.test(password) }">Minst ett tall</li>
            </ul>
          </div>

          <div class="feide-form-group">
            <label for="reg-confirm-password">Bekreft passord</label>
            <input
              id="reg-confirm-password"
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="Skriv inn passordet på nytt"
              required
              class="feide-input"
              :aria-invalid="!!error || (!!confirmPassword && password !== confirmPassword)"
              :aria-describedby="
                [
                  'password-help',
                  error ? 'register-error' : '',
                  confirmPassword && password !== confirmPassword ? 'confirm-password-error' : '',
                ]
                  .filter(Boolean)
                  .join(' ')
              "
            />
            <p
              v-if="confirmPassword && password !== confirmPassword"
              id="confirm-password-error"
              class="feide-field-error"
              role="alert"
            >
              Passordene er ikke like.
            </p>
          </div>

          <div class="feide-form-group">
            <label for="reg-role">Din rolle</label>
            <select
              id="reg-role"
              v-model="role"
              class="feide-input"
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error' : undefined"
            >
              <option value="Lærer">Lærer</option>
              <option value="Elev">Elev</option>
            </select>
          </div>

          <button type="submit" :disabled="loading" class="feide-btn-primary submit-btn">
            {{ loading ? 'Oppretter bruker...' : 'Registrer meg' }}
          </button>
        </form>

        <p v-if="error" id="register-error" class="feide-error-banner" role="alert" aria-live="assertive">
          <span aria-hidden="true">⚠ </span>{{ error }}
        </p>
      </div>

      <div class="feide-card-footer">
        <span>Har du allerede en bruker?</span>
        <router-link to="/sign-in">Logg inn her</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '@/assets/styles/feideStyle.css';

* {
  box-sizing: border-box;
}

.signup-page,
.page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  font-family: Arial, Helvetica, sans-serif;
}

.signup-page::before,
.page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: url('@/assets/backgrounds/welcomeBackground.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  filter: blur(8px);
  transform: scale(1.04);
  z-index: 0;
}

.signup-page::after,
.page::after {
  content: '';
  position: absolute;
  inset: 0;
  background: rgb(239 211 175 / 35%);
  z-index: 1;
}

.signup-card,
.frame {
  position: relative;
  z-index: 2;
}

.signup-card {
  width: 340px;
  background: #ffffff;
  border: 1px solid #666;
  display: flex;
  flex-direction: column;
}

.signup-body {
  padding: 28px 34px 30px;
  background: #ffffff;
}

.brand-logo {
  height: 36px;
  margin-bottom: 18px;
}

.header-text {
  margin-bottom: 18px;
}

h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
  color: #1f1f1f;
  font-family: Arial, Helvetica, sans-serif;
}

.header-text {
  margin-bottom: 18px;
}

.subtitle {
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  color: #444;
}

.password-requirements {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  font-size: 12px;
}

.password-requirements li {
  color: #777;
  padding: 2px 0;
}

.password-requirements li::before {
  content: '✗ ';
}

.password-requirements li.met {
  color: #1f7a3d;
}

.password-requirements li.met::before {
  content: '✓ ';
}

.submit-btn {
  width: 100%;
  margin-top: 12px;
}
</style>
