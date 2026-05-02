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
      <img :src="pupil.avatarUrl || '/api/objects/1'" :alt="pupil.legalName" class="pupil-icon-image" />
    </span>

    <span class="pupil-text">
      <span class="pupil-name" :class="{ 'pending-name': pupil.status === 'PENDING' }">{{ props.pupil.username }}</span>
      <span class="pupil-username" :class="{ 'pending-username': pupil.status === 'PENDING' }">Juridisk navn: <b>{{ pupil.legalName }}</b></span>
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
  background: var(--brown-dark-100);
}

.pupil-row.active,
.pupil-row.active * {
  color: var(--beige-medium-100);
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
  background: var(--beige-light-100);
}

.pupil-icon-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pupil-icon-fallback {
  font-size: 0.55rem;
  line-height: 1.05;
  text-align: center;
  padding: 0.15rem;
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
  color: var(--red-danger-100);
}

.pending-row.active .pending-name,
.pending-row.active .pending-username {
  color: var(--beige-medium-100);
}
</style>
