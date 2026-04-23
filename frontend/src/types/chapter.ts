export type TaskDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export type ValidationResult = {
  correct: boolean
  feedback: string
  currentRunAttempts: number
  currentRunCorrect: number
  runCompleted: boolean
  categoryCorrect: boolean | null
  justificationCorrect: boolean | null
}

export type ImageVerificationResponse = {
  taskId: number
  taskDescription: string
  fileId: number
  justifications: string[]
}

export type ImageVerificationRequest = {
  selectedCategory: number
  selectedJustification: string
}

export type NewsVerificationResponse = {
  taskId: number
  taskDescription: string
  fileIds: number[]
}

export type NewsVerificationRequest = {
  selectedFileId: number
}
