import { ref } from 'vue'
import { get } from '@/utils/apiUtils'
import type { TeacherClassroomOverview } from '@/types/classroom'

const classrooms = ref<TeacherClassroomOverview[]>([])
const loading = ref(false)
const error = ref('')

export function useClassrooms() {
  async function fetchClassrooms() {
    loading.value = true
    error.value = ''
    try {
      classrooms.value = await get<TeacherClassroomOverview[]>('/api/teacher/classrooms')
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to fetch classrooms'
      console.error('Failed to fetch classrooms:', errorMsg)
      error.value = errorMsg
    } finally {
      loading.value = false
    }
  }

  return {
    classrooms,
    loading,
    error,
    fetchClassrooms,
  }
}
