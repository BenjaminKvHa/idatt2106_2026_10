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

export type ImageCategory = 'REAL' | 'AI_GENERATED' | 'MANIPULATED'

export type ImageVerificationRequest = {
  selectedCategory: ImageCategory
  selectedJustification: string
}

export type NewsVerificationResponse = {
  taskId: number
  taskDescription: string
  fileIds: number[]
  altTexts?: (string | null)[]
}

export type NewsVerificationRequest = {
  selectedFileId: number
}

export type DatacenterTaskFormat = 'IMAGE_VERIFICATION' | 'NEWS_VERIFICATION'

export type DatacenterTaskResponse = {
  taskId: number
  format: DatacenterTaskFormat
  imageTask: ImageVerificationResponse | null
  newsTask: NewsVerificationResponse | null
}

export type DatacenterRunResponse = {
  tasks: DatacenterTaskResponse[]
}

export type DatacenterAnswerRequest = {
  format: DatacenterTaskFormat
  imageAnswer: ImageVerificationRequest | null
  newsAnswer: NewsVerificationRequest | null
}
