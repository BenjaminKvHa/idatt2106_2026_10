export type ChapterTaskKey = 'nyhetskvartalet' | 'fotografen'

export type TaskDifficulty = 'EASY' | 'MEDIUM' | 'HARD'

export type ImageCategory = 'REAL' | 'AI_GENERATED' | 'MANIPULATED'

export type CorrectFileSlot = 'FILE_1' | 'FILE_2'

export type TaskAdminResponse = {
  id: number
  difficulty: TaskDifficulty
  chapterId: number | null
  classroomCode: string | null
  taskJson: string
  createdAt: string
}

export type ImageVerificationTask = {
  taskDescription?: string
  fileId?: number
  correctCategory?: ImageCategory
  correctJustification?: string
  justifications?: string[]
  correctDescription?: string
  falseDescription?: string
}

export type NewsVerificationTask = {
  taskDescription?: string
  fileId_1?: number
  fileId_2?: number
  correctFileId?: number
  correctDescription?: string
  incorrectDescription?: string
}

export type FotografenTaskFormValues = {
  difficulty: TaskDifficulty
  taskDescription: string
  correctCategory: ImageCategory
  correctJustification: string
  justifications: string[]
  correctDescription: string
  falseDescription: string
  file: File | null
}

export type NyhetskvartaletTaskFormValues = {
  difficulty: TaskDifficulty
  taskDescription: string
  correctFileSlot: CorrectFileSlot
  correctDescription: string
  incorrectDescription: string
  file1: File | null
  file2: File | null
}

export const CHAPTER_ID_BY_TASK_KEY: Record<ChapterTaskKey, number> = {
  nyhetskvartalet: 1,
  fotografen: 2,
}

export const CHAPTER_LABELS: Record<ChapterTaskKey, string> = {
  nyhetskvartalet: 'Nyhetskvartalet',
  fotografen: 'Fotografen',
}

function parseTaskJson<T>(taskJson: string): T | null {
  try {
    return JSON.parse(taskJson) as T
  } catch {
    return null
  }
}

export function parseImageVerificationTask(taskJson: string): ImageVerificationTask | null {
  return parseTaskJson<ImageVerificationTask>(taskJson)
}

export function parseNewsVerificationTask(taskJson: string): NewsVerificationTask | null {
  return parseTaskJson<NewsVerificationTask>(taskJson)
}

export function getCorrectFileSlot(task: NewsVerificationTask): CorrectFileSlot | null {
  if (task.correctFileId === task.fileId_1) {
    return 'FILE_1'
  }

  if (task.correctFileId === task.fileId_2) {
    return 'FILE_2'
  }

  return null
}

export function formatTimestamp(value: string): string {
  const parsedDate = new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('nb-NO', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(parsedDate)
}
