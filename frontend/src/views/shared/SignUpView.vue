<script setup lang="ts">
import { ref } from 'vue'

type Role = 'TEACHER' | 'PUPIL'

const email = ref<string>('')
const password = ref<string>('')
const confirmPassword = ref<string>('')
const role = ref<Role>('TEACHER')
const error = ref<string>('')
const loading = ref<boolean>(false)
const submitted = ref<boolean>(false)

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
    await new Promise((resolve) => setTimeout(resolve, 700))

    console.log('Demo signup:', {
      email: email.value,
      role: role.value,
      passwordLength: password.value.length,
    })

    submitted.value = true

    email.value = ''
    password.value = ''
    confirmPassword.value = ''
    role.value = 'TEACHER'
  } catch {
    error.value = 'Kunne ikke opprette bruker.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="signup-page">
    <div class="signup-card">
      <div class="signup-body">
        <img src="@/assets/NTNU.png" alt="NTNU" class="brand-logo" />

        <div class="header-text">
          <h1>Opprett bruker</h1>
          <p class="subtitle">Bli en del av NTNU</p>
        </div>

        <form @submit.prevent="handleRegister" novalidate>
          <div class="form-group">
            <label for="reg-email">E-post</label>
            <input
              id="reg-email"
              v-model="email"
              type="email"
              autocomplete="email"
              placeholder="navn@ntnu.no"
              required
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error' : undefined"
            />
          </div>

          <div class="form-group">
            <label for="reg-password">Passord</label>
            <input
              id="reg-password"
              v-model="password"
              type="password"
              autocomplete="new-password"
              placeholder="Minst 8 tegn"
              required
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error password-help' : 'password-help'"
            />
            <ul id="password-help" class="password-requirements">
              <li :class="{ met: password.length >= 8 }">Minst 8 tegn</li>
              <li :class="{ met: /[A-ZÆØÅ]/.test(password) }">Minst én stor bokstav</li>
              <li :class="{ met: /[0-9]/.test(password) }">Minst ett tall</li>
            </ul>
          </div>

          <div class="form-group">
            <label for="reg-confirm-password">Bekreft passord</label>
            <input
              id="reg-confirm-password"
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              placeholder="Skriv inn passordet på nytt"
              required
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
              class="field-error"
              role="alert"
            >
              Passordene er ikke like.
            </p>
          </div>

          <div class="form-group">
            <label for="reg-role">Din rolle</label>
            <select
              id="reg-role"
              v-model="role"
              :aria-invalid="!!error"
              :aria-describedby="error ? 'register-error' : undefined"
            >
              <option value="TEACHER">Lærer</option>
              <option value="STUDENT">Elev</option>
            </select>
          </div>

          <button type="submit" :disabled="loading" class="signup-btn-primary">
            {{ loading ? 'Oppretter bruker...' : 'Registrer meg' }}
          </button>
        </form>

        <p v-if="error" id="register-error" class="error-msg" role="alert" aria-live="assertive">
          <span aria-hidden="true">⚠ </span>{{ error }}
        </p>
      </div>

      <div class="signup-footer">
        <span>Har du allerede en bruker?</span>
        <router-link to="/sign-in">Logg inn her</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
* {
  box-sizing: border-box;
}

.signup-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  background: rgb(239 211 175 / 100%);
  font-family: Arial, Helvetica, sans-serif;
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
}

.subtitle {
  margin: 0;
  font-size: 14px;
  line-height: 1.4;
  color: #444;
}

.form-group {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}

label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #1f1f1f;
}

input,
select {
  width: 100%;
  border: none;
  border-bottom: 1px solid #7aa7dd;
  background: transparent;
  font-size: 14px;
  padding: 10px 0 8px;
  outline: none;
  color: #222;
}

select {
  cursor: pointer;
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

.field-error {
  color: #991b1b;
  font-size: 12px;
  margin-top: 6px;
}

.signup-btn-primary {
  width: 100%;
  min-height: 42px;
  margin-top: 12px;
  border: none;
  background: #0067b8;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.signup-btn-primary:disabled {
  background: #b9b9b9;
  cursor: not-allowed;
}

.error-msg {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid #fecaca;
  background: #fff5f5;
  color: #991b1b;
  font-size: 13px;
}

.signup-footer {
  border-top: 1px solid #d5d5d5;
  padding: 14px 34px;
  display: flex;
  gap: 6px;
  align-items: center;
  flex-wrap: wrap;
  font-size: 14px;
  color: #333;
  background: #ffffff;
}

.signup-footer a {
  color: #1967b3;
  text-decoration: none;
}

button:focus-visible,
input:focus-visible,
select:focus-visible,
a:focus-visible {
  outline: 2px solid #1967b3;
  outline-offset: 2px;
}

@media (max-width: 480px) {
  .signup-card {
    width: 100%;
    max-width: 390px;
  }

  .signup-body,
  .signup-footer {
    padding-left: 22px;
    padding-right: 22px;
  }
}
</style>
