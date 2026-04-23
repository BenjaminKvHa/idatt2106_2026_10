import { ref } from 'vue'
import type { TopbarTab } from '@/components/ui/TeacherTopbar.vue'

const tabs = ref<TopbarTab[]>([])

export function useTeacherTopbar() {
  function setTabs(newTabs: TopbarTab[]) {
    tabs.value = newTabs
  }
  function clearTabs() {
    tabs.value = []
  }
  return { tabs, setTabs, clearTabs }
}
