<script setup lang="ts">
import { ref } from 'vue'
import hintCreature from '@/assets/icons/blobby/creatureSittingQuestion.png'
import BaseModal from '@/components/ui/BaseModal.vue'
import { get } from '@/utils/apiUtils'

type ChapterTipResponse = {
  chapterId: number
  tip: string
  newlyAdded: boolean
  allTipsReceived: boolean
}

const props = defineProps<{
  chapterId: number
}>()

const showHintPopup = ref(false)
const loading = ref(false)
const tip = ref('')
const errorMessage = ref('')
const newlyAdded = ref(false)
const allTipsReceived = ref(false)

async function openHintPopup() {
  if (loading.value) return

  showHintPopup.value = true
  loading.value = true
  tip.value = ''
  errorMessage.value = ''
  newlyAdded.value = false
  allTipsReceived.value = false

  try {
    const response = await get<ChapterTipResponse>(`/api/pupil/chapters/${props.chapterId}/tip`)
    tip.value = response.tip?.trim() || 'Ingen tips er tilgjengelige for dette kapittelet ennå.'
    newlyAdded.value = response.newlyAdded
    allTipsReceived.value = response.allTipsReceived
  } catch {
    errorMessage.value = 'Kunne ikke hente tips. Prøv igjen.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="hint-creature-wrapper">
    <button type="button" class="hint-creature-button" :disabled="loading" @click="openHintPopup">
      <img :src="hintCreature" alt="Få hint" />
    </button>
  </div>

  <slot />

  <BaseModal
    v-if="showHintPopup"
    panel-class="no-padding"
    :show-close-button="false"
    :close-on-backdrop="true"
    :close-on-escape="true"
    @close="showHintPopup = false"
  >
    <div class="hint-popup-wrapper">
      <h2>Tips 💡</h2>
      <p v-if="loading">Henter tips...</p>
      <p v-else-if="errorMessage" class="hint-error">{{ errorMessage }}</p>
      <template v-else>
        <p>{{ tip }}</p>
        <p v-if="newlyAdded" class="hint-status">Tipset er lagret i notatboken din.</p>
        <p v-else-if="allTipsReceived" class="hint-status">
          Du har allerede samlet alle tipsene for dette kapittelet.
        </p>
      </template>
      <button class="hint-close-button" @click="showHintPopup = false">Ok</button>
    </div>
  </BaseModal>
</template>

<style scoped>
.hint-creature-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  z-index: 999;
  pointer-events: auto;
}

.hint-creature-button {
  width: 120px;
  height: 120px;
  border: none;
  background: transparent;
  padding: 0;
  cursor: pointer;
  pointer-events: auto;
  display: block;
}

.hint-creature-button img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  pointer-events: none;
  filter: drop-shadow(0 8px 10px rgb(0 0 0 / 25%));
  transition:
    transform ease-in-out var(--icon-transition-duration),
    filter ease-in-out var(--icon-transition-duration);
}

.hint-creature-button:hover img,
.hint-creature-button:focus-visible img {
  transform: scale(var(--icon-hover-scale)) rotate(var(--icon-hover-rotate));
}

.hint-creature-button:disabled {
  cursor: wait;
}

.hint-popup-wrapper {
  width: min(620px, calc(100vw - 32px));
  background: var(--beige-light);
  border: 5px solid var(--brown-dark);
  border-radius: 32px;
  padding: 2rem;
  text-align: center;
  box-shadow: 0 14px 30px rgb(0 0 0 / 25%);
}

.hint-popup-wrapper h2 {
  margin: 0 0 1rem;
}

.hint-popup-wrapper p {
  margin: 0;
  font-size: 1.15rem;
  line-height: 1.5;
}

.hint-status {
  margin-top: 1rem;
  font-size: 1rem;
  font-weight: 800;
  color: var(--brown-dark);
}

.hint-error {
  color: var(--red-error, #9f1f1f);
  font-weight: 800;
}

.hint-close-button {
  margin-top: 1.5rem;
  padding: 1rem 1.5rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: var(--green-confirm);
  font-weight: 800;
  cursor: pointer;
}

@media (max-width: 768px) {
  .hint-creature-wrapper,
  .hint-creature-button {
    width: 85px;
    height: 85px;
  }
}
</style>