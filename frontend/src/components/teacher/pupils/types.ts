export type PupilStatus = 'APPROVED' | 'PENDING'
export type ActivePanel = 'NONE' | 'NOTES' | 'MYSTERY'
export type StopName = 'Nyhetskvartalet' | 'Fotografen' | 'Postkontoret' | 'Datasenteret'

export interface StopNote {
  stopName: StopName
  note: string
}

export interface Pupil {
  email: string
  legalName: string
  username: string
  classroomCode: string
  status: PupilStatus
  totalXp: number
  notes: StopNote[]
  mysterySuggestion: string
  avatarUrl?: string | null
}
