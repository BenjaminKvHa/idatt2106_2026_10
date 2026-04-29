import { computed, ref } from 'vue'
import type { TaskDifficulty, ValidationResult } from '@/types/chapter'

const DIFFICULTIES: TaskDifficulty[] = ['EASY', 'MEDIUM', 'HARD']

export function useDifficultyProgression() {
  const stepIndex = ref(0)
  const lastResult = ref<ValidationResult | null>(null)
  const runCorrect = ref(0)
  const runAttempts = ref(0)
  const runDone = ref(false)

  const loading = ref(false)
  const submitting = ref(false)
  const error = ref<string | null>(null)

  const showGuidePopup = ref(true)
  const showCompletePopup = ref(false)
  const popupTitle = ref('')
  const popupMessage = ref('')
  const popupSubMessage = ref('')
  const popupSuccess = ref(false)

  const currentDifficulty = computed(() => DIFFICULTIES[stepIndex.value]!)

  const difficultyLabel = computed(() => {
    const labels: Record<TaskDifficulty, string> = {
      EASY: 'Lett',
      MEDIUM: 'Middels',
      HARD: 'Vanskelig',
    }
    return labels[currentDifficulty.value]
  })

  const progressPercent = computed(() => {
    return ((stepIndex.value + 1) / DIFFICULTIES.length) * 100
  })

  const isLastStep = computed(() => stepIndex.value === DIFFICULTIES.length - 1)

  function recordResult(correct: boolean) {
    runAttempts.value++
    if (correct) runCorrect.value++
  }

  function advanceStep() {
    stepIndex.value++
    lastResult.value = null
  }

  function reset() {
    stepIndex.value = 0
    lastResult.value = null
    runCorrect.value = 0
    runAttempts.value = 0
    runDone.value = false
    showCompletePopup.value = false
  }

  return {
    DIFFICULTIES,
    stepIndex,
    lastResult,
    runCorrect,
    runAttempts,
    runDone,
    loading,
    submitting,
    error,
    showGuidePopup,
    showCompletePopup,
    popupTitle,
    popupMessage,
    popupSubMessage,
    popupSuccess,
    currentDifficulty,
    difficultyLabel,
    progressPercent,
    isLastStep,
    recordResult,
    advanceStep,
    reset,
  }
}
