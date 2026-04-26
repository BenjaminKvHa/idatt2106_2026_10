<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { get } from '@/utils/apiUtils'

type PupilMedal = {
  medalKey: string
  name: string
  description: string
  unlocked: boolean
  imageFileObjectId: number | null
}

const medals = ref<PupilMedal[]>([])
const loading = ref(true)
const errorMessage = ref('')

onMounted(async () => {
  try {
    medals.value = await get<PupilMedal[]>('/api/pupil/medals')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Kunne ikke laste medaljer'
  } finally {
    loading.value = false
  }
})

function medalImageUrl(medal: PupilMedal) {
  return medal.imageFileObjectId === null ? '' : `/api/objects/${medal.imageFileObjectId}`
}
</script>

<template>
  <main class="medals-page">
    <h1>Medaljer</h1>

    <p v-if="loading">Laster medaljer...</p>
    <p v-else-if="errorMessage" class="error">{{ errorMessage }}</p>

    <ul v-else class="medal-list">
      <li v-for="medal in medals" :key="medal.medalKey" class="medal-card">
        <img
          v-if="medal.imageFileObjectId !== null"
          :src="medalImageUrl(medal)"
          :alt="medal.name"
          class="medal-image"
        />
        <div v-else class="missing-image">Mangler bilde</div>

        <div>
          <h2>{{ medal.name }}</h2>
          <p>{{ medal.description }}</p>
          <strong>{{ medal.unlocked ? 'Låst opp' : 'Låst' }}</strong>
        </div>
      </li>
    </ul>
  </main>
</template>

<style scoped>
.medals-page {
  padding: 2rem;
}

.medal-list {
  display: grid;
  gap: 1rem;
  list-style: none;
  padding: 0;
}

.medal-card {
  align-items: center;
  border: 1px solid #d8c6a4;
  border-radius: 12px;
  display: flex;
  gap: 1rem;
  max-width: 560px;
  padding: 1rem;
}

.medal-image,
.missing-image {
  align-items: center;
  display: flex;
  height: 88px;
  justify-content: center;
  width: 88px;
}

.medal-image {
  object-fit: contain;
}

.missing-image {
  background: #f5ead8;
  border-radius: 8px;
  color: #7a6446;
  font-size: 0.8rem;
  text-align: center;
}

.error {
  color: #9b1c1c;
}
</style>
