export type TeacherClassroomOverview = {
  classroomCode: string
  classroomName: string
  totalScore: number
  globalRank: number
}

export type CreateClassroomRequest = {
  name: string
  description: string
}

export type CreateClassroomResponse = {
  classroomCode: string
  classroomName: string
  classroomDescription: string
}
