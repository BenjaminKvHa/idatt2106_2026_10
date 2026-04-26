<script setup lang="ts">
import { computed } from 'vue'
import type { Pupil } from './types'

const props = defineProps<{
  pupil: Pupil
  selected: boolean
}>()

const emit = defineEmits<{
  select: [email: string]
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

function selectPupil() {
  emit('select', props.pupil.email)
}
</script>

<template>
  <button
    class="pupil-row"
    :class="{
      active: selected,
      'pending-row': pupil.status === 'PENDING',
    }"
    type="button"
    @click="selectPupil"
  >
    <span class="pupil-icon">
      <img v-if="pupil.avatarUrl" :src="pupil.avatarUrl" :alt="pupil.legalName" class="pupil-icon-image" />
      <span v-else>{{ initials }}</span>
    </span>

    <span class="pupil-text">
      <span class="pupil-name" :class="{ 'pending-name': pupil.status === 'PENDING' }">{{ pupil.legalName }}</span>
      <span class="pupil-username" :class="{ 'pending-username': pupil.status === 'PENDING' }">{{ secondaryText }}</span>
    </span>
  </button>
</template>

<style scoped>
.pupil-row {
  width: 100%;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
  text-align: left;
  transition: 0.2s ease;
}

.pupil-row:hover {
  background: var(--obscure);
  transform: translateX(2px);
}

.pupil-row.active {
  background: var(--accent);
}

.pupil-row.active,
.pupil-row.active * {
  color: var(--txt-sec);
}

.pupil-icon {
  width: 38px;
  min-width: 38px;
  height: 38px;
  border: 3px solid currentColor;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.85rem;
  font-weight: 700;
  overflow: hidden;
  background: var(--background);
}

.pupil-icon-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pupil-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.1;
}

.pupil-name {
  font-size: 1.05rem;
  font-weight: 700;
}

.pupil-username {
  font-size: 0.78rem;
  opacity: 0.72;
}

.pending-name,
.pending-username {
  color: var(--warning);
}

.pending-row.active .pending-name,
.pending-row.active .pending-username {
  color: var(--txt-sec);
}
</style>
