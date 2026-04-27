<script setup lang="ts">
import { computed } from 'vue'
import { SaveIcon } from '@lucide/vue'
import type { Pupil } from './types'

const props = defineProps<{
  pupil: Pupil
  editUsername: string
  showSaveButton?: boolean
}>()

const emit = defineEmits<{
  'update:editUsername': [value: string]
  saveChanges: []
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
        <div class="profile-header-row">
          <p class="profile-name inika">{{ pupil.legalName }}</p>
          <p class="xp-inline inika">{{ pupil.totalXp }} XP</p>
        </div>
        <p class="username-display inika">{{ secondaryText }}</p>
      </div>

      <div class="username-edit">
        <label class="field-label inika" for="username-input">Brukernavn</label>

        <div class="username-edit-row">
          <input
            id="username-input"
            :value="editUsername"
            type="text"
            class="small-input"
            placeholder="Brukernavn"
            @input="updateUsername"
          />

          <button
            v-if="showSaveButton"
            class="save-btn inika"
            type="button"
            aria-label="Lagre endringer"
            @click="emit('saveChanges')"
          >
            <SaveIcon />
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.profile-summary-card {
  border: var(--border-thin) solid var(--accent);
  border-radius: 1rem;
  padding: 2.5rem;
  display: flex;
  gap: 1rem;
  align-items: center;
  background: var(--background);
}

.avatar-area {
  display: grid;
  grid-template-columns: 120px 1fr;
  column-gap: 18px;
  row-gap: .5px;
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

.profile-header-row {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.profile-name {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
}

.xp-inline {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  opacity: 0.8;
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

.username-edit-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 0.75rem;
  align-items: center;
}

.save-btn {
  width: 3rem;
  height: 3rem;
  border: 3px solid var(--accent);
  border-radius: 999px;
  padding: 0;
  cursor: pointer;
  transition: 0.2s ease;
  background: var(--background);
  color: var(--brown-dark);
  display: flex;
  align-items: center;
  justify-content: center;
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 0 var(--accent);
}

.save-btn :deep(svg) {
  width: 1.2rem;
  height: 1.2rem;
}

@media (max-width: 1100px) {
  .username-edit-row {
    grid-template-columns: 1fr;
  }
}
</style>
