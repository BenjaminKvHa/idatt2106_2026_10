export type TaskDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export type ValidationResult = {
  correct: boolean
  feedback: string
  currentRunAttempts: number
  currentRunCorrect: number
  runCompleted: boolean
}

export type NewsVerificationResponse = {
  taskId: number
  taskDescription: string
  fileIds: number[]
}

export type NewsVerificationRequest = {
  selectedFileId: number
}
