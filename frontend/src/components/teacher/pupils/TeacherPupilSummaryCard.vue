<script setup lang="ts">
import { computed } from 'vue'
import type { Pupil } from './types'

const props = defineProps<{
  pupil: Pupil
  editUsername: string
}>()

const emit = defineEmits<{
  'update:editUsername': [value: string]
}>()

const initials = computed(() =>
  props.pupil.legalName
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? '')
    .join(''),
)

const secondaryText = computed(() => {
  if (props.pupil.username.trim().length > 0) {
    return `@${props.pupil.username}`
  }

  return props.pupil.email
})

function updateUsername(event: Event) {
  const target = event.target as HTMLInputElement
  emit('update:editUsername', target.value)
}
</script>

<template>
  <div class="profile-summary-card">
    <div class="avatar-area">
      <div v-if="pupil.avatarUrl" class="avatar-image-wrapper">
        <img :src="pupil.avatarUrl" :alt="pupil.legalName" class="avatar-image" />
      </div>

      <div v-else class="avatar-fallback">
        {{ initials }}
      </div>

      <div class="profile-text">
        <p class="profile-name inika">{{ pupil.legalName }}</p>
        <p class="username-display inika">{{ secondaryText }}</p>
      </div>

      <div class="username-edit">
        <label class="field-label inika" for="username-input">Brukernavn</label>

        <input
          id="username-input"
          :value="editUsername"
          type="text"
          class="small-input"
          placeholder="Brukernavn"
          @input="updateUsername"
        />
      </div>
    </div>

    <div class="xp-area">
      <div class="stat-card">
        <div class="stat-item">
          <span class="stat-label inika">XP</span>
          <strong class="stat-value inika">{{ pupil.totalXp }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-summary-card {
  border: 5px solid var(--accent);
  border-radius: 28px;
  padding: 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  align-items: center;
  background: var(--background);
}

.avatar-area {
  display: grid;
  grid-template-columns: 120px 1fr;
  column-gap: 18px;
  row-gap: 10px;
  align-items: center;
}

.avatar-image-wrapper,
.avatar-fallback {
  width: 120px;
  height: 120px;
  grid-row: span 2;
  border-radius: 50%;
  overflow: hidden;
  background: var(--background-sec);
  display: grid;
  place-items: center;
  font-size: 2.6rem;
  font-weight: 700;
  border: 4px solid var(--accent);
  box-shadow: inset 0 0 0 4px var(--background);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-text {
  align-self: end;
}

.profile-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
}

.username-display {
  margin: 4px 0 0;
  font-size: 0.95rem;
  font-weight: 700;
  opacity: 0.72;
}

.username-edit {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.field-label {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
}

.small-input {
  width: 100%;
  border: 3px solid var(--background-sec);
  border-radius: 12px;
  background: var(--background);
  padding: 9px 12px;
  font-size: 1rem;
}

.xp-area {
  display: flex;
  justify-content: center;
}

.stat-card {
  display: grid;
  place-items: center;
  width: 100%;
  max-width: 220px;
  background: var(--background);
  border: 3px solid var(--background-sec);
  border-radius: 18px;
  padding: 18px 20px;
  margin: 0 auto;
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.95rem;
  font-weight: 700;
  opacity: 0.75;
}

.stat-value {
  display: block;
  margin-top: 4px;
  font-size: 2rem;
}

@media (max-width: 1100px) {
  .profile-summary-card {
    grid-template-columns: 1fr;
  }
}
</style>
