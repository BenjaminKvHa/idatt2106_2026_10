<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'
import VictoryImage from '@/assets/Victory.png'
import DefeatImage from '@/assets/Defeat.png'
import DatatyvenImage from '@/assets/Datatyven.png'
import victorySoundFile from '@/assets/victory-sound.mp3'
import defeatSoundFile from '@/assets/evil-laugh.mp3'
import confetti from 'canvas-confetti'
import { get, getBlob, post } from '@/utils/apiUtils'
import type {
  DatacenterAnswerRequest,
  DatacenterRunResponse,
  DatacenterTaskResponse,
  ImageCategory,
  ValidationResult,
} from '@/types/chapter'

type ResultType = 'victory' | 'defeat' | null

type LevelState = {
  task: DatacenterTaskResponse
  imageUrls: string[]
  selectedCategory: ImageCategory | null
  selectedJustification: string | null
  selectedFileId: number | null
  result: ValidationResult | null
}

const router = useRouter()

const DATASENTERET_CHAPTER_ID = 3

const CATEGORIES: { value: ImageCategory; label: string }[] = [
  { value: 'REAL', label: 'Ekte' },
  { value: 'AI_GENERATED', label: 'KI-generert' },
  { value: 'MANIPULATED', label: 'Manipulert' },
]

const victorySound = new Audio(victorySoundFile)
victorySound.volume = 0.3
const defeatSound = new Audio(defeatSoundFile)
defeatSound.volume = 0.3

const showHelpPopup = ref(false)
const showHintPopup = ref(false)
const showGuidePopup = ref(true)
const showResultPopup = ref(false)
const resultType = ref<ResultType>(null)

const levels = ref<LevelState[]>([])
const currentLevelIndex = ref(0)
const loading = ref(false)
const submitting = ref(false)
const error = ref<string | null>(null)

const currentLevel = computed<LevelState | null>(
  () => levels.value[currentLevelIndex.value] ?? null,
)

const isLastLevel = computed(
  () => currentLevelIndex.value === levels.value.length - 1,
)

const isCurrentLevelAnswered = computed(
  () => currentLevel.value?.result != null,
)

const correctCount = computed(
  () => levels.value.filter((l) => l.result?.correct).length,
)

const allAnswersCorrect = computed(
  () =>
    levels.value.length > 0 &&
    levels.value.every((l) => l.result?.correct === true),
)

const canSubmit = computed(() => {
  const level = currentLevel.value
  if (!level || level.result || submitting.value) return false
  if (level.task.format === 'IMAGE_VERIFICATION') {
    return (
      level.selectedCategory !== null && level.selectedJustification !== null
    )
  }
  return level.selectedFileId !== null
})

const resultImage = computed(() =>
  resultType.value === 'victory' ? VictoryImage : DefeatImage,
)
const resultAltText = computed(() =>
  resultType.value === 'victory' ? 'Seier' : 'Tap',
)
const resultKicker = computed(() =>
  resultType.value === 'victory' ? 'Oppdrag fullført' : 'Oppdrag mislyktes',
)
const resultTitle = computed(() =>
  resultType.value === 'victory' ? 'Du klarte det!' : 'Du tapte!',
)
const resultMessage = computed(() =>
  resultType.value === 'victory'
    ? `Gratulerer! Du svarte riktig på alle ${levels.value.length} oppgavene og slo datatyven.`
    : `Du fikk ${correctCount.value} av ${levels.value.length} riktig. Datatyven slapp unna denne gangen.`,
)
const resultSubmessage = computed(() =>
  resultType.value === 'victory'
    ? 'Nå har du vist at du kan tenke som en ekte nett-detektiv!'
    : 'Men ikke gi opp – prøv igjen og se om du klarer å slå datatyven neste gang!',
)
const resultHighlightTitle = computed(() =>
  resultType.value === 'victory' ? '🏆 Spill fullført' : '💥 Prøv igjen',
)
const resultHighlightText = computed(() =>
  resultType.value === 'victory'
    ? 'Du har kommet helt til toppen i Datasenteret.'
    : 'Du kan gå tilbake og prøve oppgavene på nytt for å få en ny sjanse til å fullføre spillet.',
)

function revokeAllImageUrls() {
  for (const level of levels.value) {
    for (const url of level.imageUrls) URL.revokeObjectURL(url)
    level.imageUrls = []
  }
}

async function loadRun() {
  loading.value = true
  error.value = null
  revokeAllImageUrls()
  levels.value = []
  currentLevelIndex.value = 0

  try {
    const run = await get<DatacenterRunResponse>(
      `/api/chapters/${DATASENTERET_CHAPTER_ID}/datasenteret/run`,
    )

    const built: LevelState[] = []
    for (const task of run.tasks) {
      const fileIds =
        task.format === 'IMAGE_VERIFICATION'
          ? task.imageTask
            ? [task.imageTask.fileId]
            : []
          : (task.newsTask?.fileIds ?? [])

      const blobs = await Promise.all(
        fileIds.map((id) => getBlob(`/api/objects/${id}`)),
      )
      const urls = blobs.map((b) => URL.createObjectURL(b))

      built.push({
        task,
        imageUrls: urls,
        selectedCategory: null,
        selectedJustification: null,
        selectedFileId: null,
        result: null,
      })
    }
    levels.value = built
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke laste oppgavene'
  } finally {
    loading.value = false
  }
}

function selectCategory(value: ImageCategory) {
  const level = currentLevel.value
  if (!level || level.result) return
  level.selectedCategory = value
  level.selectedJustification = null
}

function selectJustification(value: string) {
  const level = currentLevel.value
  if (!level || level.result) return
  level.selectedJustification = value
}

function selectFileId(value: number) {
  const level = currentLevel.value
  if (!level || level.result) return
  level.selectedFileId = value
}

function categoryClass(value: ImageCategory) {
  const level = currentLevel.value
  if (!level) return ''
  const selected = level.selectedCategory === value
  if (!selected) return ''
  if (!level.result) return 'selected'
  const ok = level.result.categoryCorrect ?? level.result.correct
  return ok ? 'selected correct' : 'selected wrong'
}

function justificationClass(value: string) {
  const level = currentLevel.value
  if (!level) return ''
  const selected = level.selectedJustification === value
  if (!selected) return ''
  if (!level.result) return 'selected'
  const ok = level.result.justificationCorrect ?? level.result.correct
  return ok ? 'selected correct' : 'selected wrong'
}

function newsCardClass(fileId: number) {
  const level = currentLevel.value
  if (!level) return ''
  if (level.selectedFileId !== fileId) return ''
  if (!level.result) return 'selected'
  return level.result.correct ? 'selected correct' : 'selected wrong'
}

async function submit() {
  const level = currentLevel.value
  if (!level || !canSubmit.value) return
  submitting.value = true
  error.value = null

  try {
    let body: DatacenterAnswerRequest
    if (level.task.format === 'IMAGE_VERIFICATION') {
      body = {
        format: 'IMAGE_VERIFICATION',
        imageAnswer: {
          selectedCategory: level.selectedCategory!,
          selectedJustification: level.selectedJustification!,
        },
        newsAnswer: null,
      }
    } else {
      body = {
        format: 'NEWS_VERIFICATION',
        imageAnswer: null,
        newsAnswer: { selectedFileId: level.selectedFileId! },
      }
    }

    const result = await post<ValidationResult>(
      `/api/chapters/datasenteret/tasks/${level.task.taskId}/validate`,
      body,
    )
    level.result = result

    if (!result.correct) {
      openResultPopup('defeat')
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Kunne ikke sende svar'
  } finally {
    submitting.value = false
  }
}

function goToNextLevel() {
  if (!isCurrentLevelAnswered.value || isLastLevel.value) return
  currentLevelIndex.value += 1
}

function goToPreviousLevel() {
  if (currentLevelIndex.value === 0) return
  currentLevelIndex.value -= 1
}

function finishGame() {
  if (!isCurrentLevelAnswered.value) return
  openResultPopup(allAnswersCorrect.value ? 'victory' : 'defeat')
}

function stopResultSounds() {
  victorySound.pause()
  defeatSound.pause()
  victorySound.currentTime = 0
  defeatSound.currentTime = 0
}

function fireVictoryConfetti() {
  const duration = 2000
  const end = Date.now() + duration
  const defaults = { zIndex: 3000 }

  const frame = () => {
    confetti({
      ...defaults,
      particleCount: 8,
      angle: 60,
      spread: 60,
      origin: { x: 0, y: 0.6 },
    })
    confetti({
      ...defaults,
      particleCount: 8,
      angle: 120,
      spread: 60,
      origin: { x: 1, y: 0.6 },
    })
    if (Date.now() < end) requestAnimationFrame(frame)
  }
  frame()
}

watch(showResultPopup, (isOpen) => {
  if (!isOpen) {
    stopResultSounds()
    return
  }
  stopResultSounds()
  if (resultType.value === 'victory') {
    victorySound.play()
    fireVictoryConfetti()
  } else if (resultType.value === 'defeat') {
    defeatSound.play()
  }
})

function openResultPopup(type: ResultType) {
  resultType.value = type
  showResultPopup.value = true
}

function closeResultPopup() {
  showResultPopup.value = false
  resultType.value = null
  stopResultSounds()
}

function goBackToMap() {
  closeResultPopup()
  void router.push('/pupil/classroom/map')
}

async function resetGame() {
  closeResultPopup()
  showGuidePopup.value = true
  await loadRun()
}

onMounted(loadRun)
onBeforeUnmount(revokeAllImageUrls)
</script>

<template>
  <TopBar
    title="Datasenteret"
    @help="showHelpPopup = true"
    @hint="showHintPopup = true"
  />

  <div class="page">
    <div class="center-container">
      <p v-if="loading">Laster oppgaver…</p>
      <p v-else-if="error" class="error">{{ error }}</p>

      <template v-else-if="currentLevel">
        <div class="progress-box">
          <p>Nivå {{ currentLevelIndex + 1 }}/{{ levels.length }}</p>
        </div>

        <div class="task-wrapper">
          <p>
            {{
              currentLevel.task.format === 'IMAGE_VERIFICATION'
                ? currentLevel.task.imageTask?.taskDescription
                : currentLevel.task.newsTask?.taskDescription
            }}
          </p>
        </div>

        <div class="task-card">
          <template v-if="currentLevel.task.format === 'IMAGE_VERIFICATION'">
            <div
              class="image-card"
              :class="{
                correct:
                  currentLevel.result && currentLevel.result.correct,
                wrong:
                  currentLevel.result && currentLevel.result.correct === false,
              }"
            >
              <img
                v-if="currentLevel.imageUrls[0]"
                :src="currentLevel.imageUrls[0]"
                alt="Oppgavebilde"
                class="task-image"
              />
              <div v-else class="image-placeholder">Laster bilde…</div>
            </div>

            <div class="choices">
              <button
                v-for="cat in CATEGORIES"
                :key="cat.value"
                @click="selectCategory(cat.value)"
                :disabled="!!currentLevel.result"
                :class="categoryClass(cat.value)"
              >
                {{ cat.label }}
              </button>
            </div>

            <div v-if="currentLevel.selectedCategory !== null" class="reason-box">
              <p>Hvorfor?</p>
              <button
                v-for="j in currentLevel.task.imageTask?.justifications ?? []"
                :key="j"
                @click="selectJustification(j)"
                :disabled="!!currentLevel.result"
                :class="justificationClass(j)"
              >
                {{ j }}
              </button>
            </div>
          </template>

          <template v-else>
            <div class="picture-wrapper">
              <div
                v-for="(fileId, idx) in currentLevel.task.newsTask?.fileIds ?? []"
                :key="fileId"
                class="picture-card"
                :class="newsCardClass(fileId)"
              >
                <div class="picture-placeholder">
                  <img
                    v-if="currentLevel.imageUrls[idx]"
                    :src="currentLevel.imageUrls[idx]"
                    :alt="`Bilde ${Number(idx) + 1}`"
                    class="picture"
                  />
                  <span v-else>Laster…</span>
                </div>
                <button
                  class="fake-button"
                  :disabled="!!currentLevel.result || submitting"
                  @click="selectFileId(fileId)"
                >
                  Dette er den falske
                </button>
              </div>
            </div>
          </template>

          <button
            v-if="currentLevel.task.format === 'IMAGE_VERIFICATION'"
            class="submit-button"
            :disabled="!canSubmit"
            @click="submit"
          >
            Svar
          </button>

          <button
            v-else-if="
              currentLevel.task.format === 'NEWS_VERIFICATION' &&
              currentLevel.selectedFileId !== null &&
              !currentLevel.result
            "
            class="submit-button"
            :disabled="!canSubmit"
            @click="submit"
          >
            Bekreft svar
          </button>

          <div
            v-if="currentLevel.result"
            class="feedback-box"
            :class="
              currentLevel.result.correct ? 'feedback-correct' : 'feedback-wrong'
            "
          >
            <p class="feedback-title">
              {{ currentLevel.result.correct ? 'Riktig!' : 'Feil!' }}
            </p>
            <p>{{ currentLevel.result.feedback }}</p>
          </div>
        </div>

        <div class="navigation-bar">
          <button
            class="nav-button secondary"
            :disabled="currentLevelIndex === 0"
            @click="goToPreviousLevel"
          >
            Forrige
          </button>

          <button
            v-if="!isLastLevel"
            class="nav-button primary"
            :disabled="!isCurrentLevelAnswered"
            @click="goToNextLevel"
          >
            Neste
          </button>

          <button
            v-else
            class="nav-button primary"
            :disabled="!isCurrentLevelAnswered"
            @click="finishGame"
          >
            Fullfør
          </button>
        </div>
      </template>

      <div v-if="showGuidePopup" class="intro-overlay">
        <div class="intro-popup">
          <h2 class="intro-title">Velkommen til datasenteret!</h2>
          <p class="intro-text">
            Klarer du å svare riktig på alle oppgavene og slå datatyven?
          </p>
          <img :src="DatatyvenImage" alt="Datatyven" class="intro-image" />
          <button class="intro-button" @click="showGuidePopup = false">
            Jeg er klar!
          </button>
        </div>
      </div>

      <div
        v-if="showHelpPopup"
        class="popup-overlay"
        @click="showHelpPopup = false"
      >
        <div class="hint-popup" @click.stop>
          <h2 class="popup-title">Trenger du hjelp?</h2>
          <div class="hint-text">
            <p>Inne på denne siden skal du løse oppgaver for å bekjempe datatyven.</p>
            <ul>
              <li>Det er én oppgave på hvert nivå</li>
              <li>Du kan bare svare én gang på hver oppgave</li>
              <li>Du må svare riktig på alle nivåene for å vinne</li>
            </ul>
          </div>
          <div class="popup-actions">
            <button class="popup-btn popup-btn-play" @click="showHelpPopup = false">
              Ok
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="showHintPopup"
        class="popup-overlay"
        @click="showHintPopup = false"
      >
        <div class="hint-popup" @click.stop>
          <h2 class="popup-title">Tips 💡</h2>
          <div class="hint-text">
            <p>Les oppgaven nøye før du svarer.</p>
            <ul>
              <li>Se etter små detaljer som kan avsløre riktig svar</li>
              <li>Ta deg god tid – du kan ikke svare på nytt</li>
              <li>Bruk det du har lært tidligere i spillet</li>
            </ul>
          </div>
          <div class="popup-actions">
            <button class="popup-btn popup-btn-play" @click="showHintPopup = false">
              Ok
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="showResultPopup"
        class="victory-overlay"
        @click="closeResultPopup"
      >
        <div class="victory-popup" @click.stop>
          <div class="victory-top">
            <img :src="resultImage" :alt="resultAltText" class="victory-image" />
          </div>
          <div class="victory-content">
            <p class="victory-kicker">{{ resultKicker }}</p>
            <h2 class="victory-title">{{ resultTitle }}</h2>
            <p class="victory-message">{{ resultMessage }}</p>
            <p class="victory-submessage">{{ resultSubmessage }}</p>

            <div v-if="resultType === 'victory'" class="victory-highlight">
              <p class="victory-highlight-title">{{ resultHighlightTitle }}</p>
              <p>{{ resultHighlightText }}</p>
            </div>

            <div class="victory-actions">
              <button
                v-if="resultType === 'defeat'"
                class="retry-button"
                @click="resetGame"
              >
                Prøv på nytt
              </button>
              <button class="victory-close-button" @click="goBackToMap">
                Tilbake til kartet
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2,
p {
  margin: 0;
}

.page {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem 3rem;
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 1000px;
  text-align: center;
  gap: 1.25rem;
}

.error {
  color: #c62828;
  font-weight: 600;
}

.progress-box {
  width: min(100%, 700px);
  padding: 1rem 1.4rem;
  background: var(--beige-light);
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 700;
}

.task-wrapper {
  width: min(100%, 700px);
  padding: 1rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
}

.task-card {
  width: min(100%, 760px);
  background: #fff7e9;
  border: 5px solid var(--brown-dark);
  border-radius: 32px;
  padding: 1.75rem;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  box-sizing: border-box;
}

.image-card {
  display: flex;
  justify-content: center;
  padding: 0.75rem;
  border-radius: 18px;
  border: 3px solid transparent;
  transition: all 0.2s ease;
}

.task-image {
  width: 460px;
  max-width: 100%;
  height: 280px;
  object-fit: contain;
  background: #f2f2f2;
  border-radius: 14px;
  border: 2px solid #999;
}

.image-placeholder {
  width: 460px;
  max-width: 100%;
  height: 280px;
  background: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  border: 2px solid #999;
  font-size: 20px;
  font-weight: 600;
}

.choices {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.choices button {
  padding: 10px 16px;
  border-radius: 20px;
  border: 2px solid #333;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
  font-weight: 500;
}

.choices button:hover:not(:disabled) {
  background: #f4a259;
  transform: scale(1.05);
}

.choices button.selected {
  background: #f4a259;
  border-color: #000;
  transform: scale(1.08);
  box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.2);
}

.reason-box {
  text-align: center;
}

.reason-box p {
  margin-bottom: 10px;
  font-weight: 600;
}

.reason-box button {
  margin: 5px;
  padding: 8px 14px;
  border-radius: 16px;
  border: 2px solid #555;
  background: #eee;
  cursor: pointer;
  transition: all 0.2s ease;
}

.reason-box button:hover:not(:disabled) {
  background: #ddd;
}

.reason-box button.selected {
  background: #90caf9;
  border-color: #1976d2;
  transform: scale(1.05);
}

.picture-wrapper {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
  justify-content: center;
}

.picture-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  flex: 1 1 280px;
  max-width: 380px;
  padding: 0.5rem;
  border-radius: 16px;
  transition: all 0.2s ease;
}

.picture-card.selected {
  box-shadow: 0 0 0 4px rgba(0, 0, 0, 0.2);
}

.picture-card.correct {
  background: #a5d6a7;
  box-shadow: 0 0 0 4px #2e7d32;
}

.picture-card.wrong {
  background: #ef9a9a;
  box-shadow: 0 0 0 4px #c62828;
}

.picture-placeholder {
  width: 100%;
  aspect-ratio: 4 / 3;
  background-color: var(--beige-light);
  border: 2px solid var(--brown-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  overflow: hidden;
  border-radius: 8px;
}

.picture {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.fake-button {
  width: 100%;
  padding: 0.75rem 1.25rem;
  border-radius: 20px;
  border: 4px solid var(--brown-dark);
  background-color: var(--orange-medium);
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.fake-button:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: scale(1.02);
}

.fake-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-button {
  width: 100%;
  padding: 1rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: var(--orange-medium);
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}

.submit-button:hover:not(:disabled) {
  transform: scale(1.01);
  filter: brightness(1.05);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.feedback-box {
  padding: 1rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
}

.feedback-correct {
  background: #dff4d2;
}

.feedback-wrong {
  background: #ffd4cc;
}

.feedback-title {
  margin-bottom: 0.35rem;
  font-weight: 800;
}

.navigation-bar {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.nav-button {
  min-width: 200px;
  min-height: 68px;
  padding: 1rem 1.5rem;
  border-radius: 24px;
  border: 4px solid var(--brown-dark);
  font-weight: 800;
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-button.primary {
  background: var(--green-confirm);
}

.nav-button.secondary {
  background: #ffe08a;
}

.nav-button:hover:not(:disabled) {
  transform: translateY(-2px);
  filter: brightness(1.05);
}

.nav-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.correct {
  background: #a5d6a7 !important;
  border-color: #2e7d32 !important;
}

.wrong {
  background: #ef9a9a !important;
  border-color: #c62828 !important;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.intro-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 35%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 3000;
}

.intro-popup {
  width: min(700px, 90vw);
  background: linear-gradient(180deg, #f5d7a8 0%, #e9b878 100%);
  border: 6px solid var(--brown-dark);
  border-radius: 32px;
  padding: 2rem 1.5rem;
  text-align: center;
  box-shadow: 0 25px 60px rgb(0 0 0 / 35%);
}

.intro-title {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.intro-text {
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
  font-weight: 600;
}

.intro-image {
  width: min(260px, 70%);
  margin: 0 auto 1.5rem;
  display: block;
  object-fit: contain;
}

.intro-button {
  min-width: 220px;
  padding: 1rem;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  background: #7ed321;
  font-weight: 800;
  font-size: 1.2rem;
  cursor: pointer;
  box-shadow: 0 6px 14px rgb(0 0 0 / 25%);
  transition: transform 0.2s ease;
}

.intro-button:hover {
  transform: scale(1.05);
}

.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 18%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.hint-popup {
  width: min(800px, calc(100vw - 32px));
  background: var(--orange-medium-80);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 40px 32px 28px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.popup-title {
  margin: 0 0 16px;
}

.hint-text {
  margin-top: 16px;
  text-align: center;
}

.hint-text p {
  margin: 0 0 14px;
}

.hint-text ul {
  margin: 0;
  padding-left: 24px;
}

.hint-text li {
  margin-bottom: 6px;
}

.popup-actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
}

.popup-btn {
  min-width: 180px;
  padding: 18px 28px;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
}

.popup-btn-play {
  background: var(--green-confirm);
}

.victory-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 32%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.victory-popup {
  width: min(980px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  background: linear-gradient(
    180deg,
    rgb(255 214 120 / 95%) 0%,
    rgb(245 170 92 / 96%) 18%,
    rgb(232 155 86 / 98%) 100%
  );
  border: 6px solid var(--brown-dark);
  border-radius: 40px;
  box-shadow:
    0 22px 60px rgb(0 0 0 / 30%),
    inset 0 3px 0 rgb(255 255 255 / 28%);
  text-align: center;
  position: relative;
}

.victory-top {
  padding: 3rem 2rem 1.5rem;
}

.victory-image {
  width: min(100%, 600px);
  max-height: 400px;
  object-fit: contain;
  filter: drop-shadow(0 12px 20px rgb(0 0 0 / 20%));
}

.victory-content {
  padding: 0 2.5rem 2.5rem;
}

.victory-kicker {
  margin: 0 0 0.5rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.85;
}

.victory-title {
  margin: 0;
  font-size: clamp(2rem, 5.5vw, 4.2rem);
  line-height: 1;
}

.victory-message {
  margin: 1rem auto 0;
  max-width: 700px;
}

.victory-submessage {
  margin: 0.75rem auto 0;
  max-width: 700px;
}

.victory-highlight {
  margin: 2rem auto 0;
  max-width: 700px;
  padding: 1.2rem 1.4rem;
  background: rgb(255 245 214 / 72%);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 8px 20px rgb(0 0 0 / 12%);
}

.victory-highlight-title {
  margin: 0 0 0.4rem;
  font-weight: 800;
}

.victory-actions {
  margin-top: 2rem;
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.retry-button,
.victory-close-button {
  min-width: 240px;
  min-height: 72px;
  padding: 1rem 1.4rem;
  border: 4px solid var(--brown-dark);
  border-radius: 24px;
  font-weight: 800;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  box-shadow: 0 8px 18px rgb(0 0 0 / 18%);
  transition: all 0.2s ease;
}

.retry-button {
  background: #ffe08a;
}

.victory-close-button {
  background: var(--green-confirm);
}

.retry-button:hover,
.victory-close-button:hover {
  filter: brightness(1.05);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .page {
    padding: 1rem;
  }

  .progress-box,
  .task-wrapper,
  .task-card {
    border-width: 3px;
    border-radius: 24px;
  }

  .task-wrapper,
  .task-card {
    padding: 1rem;
  }

  .navigation-bar,
  .victory-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .nav-button,
  .retry-button,
  .victory-close-button,
  .intro-button {
    width: 100%;
    min-width: 0;
  }

  .intro-popup {
    padding: 1.5rem 1rem;
    border-width: 4px;
    border-radius: 26px;
  }

  .victory-popup {
    border-width: 4px;
    border-radius: 28px;
  }

  .victory-top {
    padding: 1.5rem 1rem 0.75rem;
  }

  .victory-content {
    padding: 0 1rem 1.5rem;
  }
}
</style>
