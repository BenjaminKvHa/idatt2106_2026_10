import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import TeachersClassroomsView from '@/views/teacher/TeachersClassroomsView.vue'

const pushMock = vi.fn()
const fetchClassroomsMock = vi.fn()

const classroomsMock = ref<any[]>([])
const loadingMock = ref(false)
const errorMock = ref<string | null>(null)

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

vi.mock('@/composables/useClassrooms', () => ({
  useClassrooms: () => ({
    classrooms: classroomsMock,
    loading: loadingMock,
    error: errorMock,
    fetchClassrooms: fetchClassroomsMock,
  }),
}))

vi.mock('@/composables/useClassroomName', () => ({
  sanitizeClassroomName: vi.fn((name: string) =>
    name.toLowerCase().replace(/\s+/g, '-')
  ),
}))

vi.mock('@/components/ui/StateDisplay.vue', () => ({
  default: {
    name: 'StateDisplay',
    props: ['loading', 'error', 'loadingText'],
    template: '<div data-test="state-display"></div>',
  },
}))

describe('TeachersClassroomView.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    classroomsMock.value = []
    loadingMock.value = false
    errorMock.value = null
  })

  it('fetches classrooms on mount', () => {
    mount(TeachersClassroomsView)

    expect(fetchClassroomsMock).toHaveBeenCalledOnce()
  })

  it('renders empty state when there are no classrooms', () => {
    const wrapper = mount(TeachersClassroomsView)

    expect(wrapper.text()).toContain('Du har ingen klasserom ennå.')
    expect(wrapper.text()).toContain(
      'Opprett ditt første klasserom ved å klikke "Nytt klasserom" i menyen.'
    )
  })

  it('renders classroom cards when classrooms exist', () => {
    classroomsMock.value = [
      {
        classroomName: 'Matte Klasse',
        classroomCode: 'ABC123',
        totalScore: 250,
        globalRank: 4,
      },
    ]

    const wrapper = mount(TeachersClassroomsView)

    expect(wrapper.text()).toContain('Matte Klasse')
    expect(wrapper.text()).toContain('ABC123')
    expect(wrapper.text()).toContain('250')
    expect(wrapper.text()).toContain('#4')
    expect(wrapper.find('button').text()).toBe('Gå til klasserom')
  })

  it('navigates to sanitized classroom leaderboard when button is clicked', async () => {
    classroomsMock.value = [
      {
        classroomName: 'Matte Klasse',
        classroomCode: 'ABC123',
        totalScore: 250,
        globalRank: 4,
      },
    ]

    const wrapper = mount(TeachersClassroomsView)

    await wrapper.find('button').trigger('click')

    expect(pushMock).toHaveBeenCalledWith(
      '/teacher/classrooms/matte-klasse/leaderboard'
    )
  })

  it('does not render classrooms while loading', () => {
    loadingMock.value = true
    classroomsMock.value = [
      {
        classroomName: 'Matte Klasse',
        classroomCode: 'ABC123',
        totalScore: 250,
        globalRank: 4,
      },
    ]

    const wrapper = mount(TeachersClassroomsView)

    expect(wrapper.text()).not.toContain('Matte Klasse')
  })

  it('does not render empty state when there is an error', () => {
    errorMock.value = 'Noe gikk galt'

    const wrapper = mount(TeachersClassroomsView)

    expect(wrapper.text()).not.toContain('Du har ingen klasserom ennå.')
  })
})
