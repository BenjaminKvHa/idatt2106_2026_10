import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import TeacherMenu from '@/components/teacher/TeacherMenu.vue'

const apiGet = vi.hoisted(() => vi.fn())
const fetchClassrooms = vi.fn()
const toggleMute = vi.fn()
const isMuted = ref(false)
const mountedWrappers: VueWrapper[] = []
const classrooms = ref([
  {
    classroomCode: 'ABCABC',
    classroomName: 'Matte 1A',
    totalScore: 120,
    globalRank: 1,
  },
  {
    classroomCode: 'DEFDEF',
    classroomName: 'Norsk Gruppe',
    totalScore: 90,
    globalRank: 2,
  },
])
const routeState = {
  path: '/teacher/classrooms',
  query: {} as Record<string, string>,
  params: {} as Record<string, string>,
}

vi.mock('@/composables/useClassrooms', () => ({
  useClassrooms: () => ({
    classrooms,
    fetchClassrooms,
  }),
}))

vi.mock('@/composables/useAudio', () => ({
  useAudio: () => ({
    isMuted,
    toggleMute,
  }),
}))

vi.mock('@/utils/apiUtils', () => ({
  get: apiGet,
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
}))

function mountTeacherMenu() {
  const wrapper = mount(TeacherMenu, {
    global: {
      stubs: {
        Logo: {
          template: '<div data-test="logo" />',
        },
        OptionsMenu: {
          emits: ['close'],
          template: '<section data-test="options-menu"><button type="button" @click="$emit(\'close\')">Close</button></section>',
        },
        BaseModal: {
          emits: ['close'],
          template: '<section data-test="base-modal"><slot /></section>',
        },
        NotificationsMenu: {
          emits: ['close'],
          template: '<section data-test="notifications-menu"><button type="button" @click="$emit(\'close\')">Close</button></section>',
        },
        RouterLink: {
          props: ['to'],
          template: '<a :data-to="to"><slot /></a>',
        },
      },
    },
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('TeacherMenu', () => {
  beforeEach(() => {
    fetchClassrooms.mockReset()
    fetchClassrooms.mockResolvedValue(undefined)
    toggleMute.mockReset()
    apiGet.mockReset()
    apiGet.mockResolvedValue({ unreadNotificationsCount: 0 })
    isMuted.value = false
    classrooms.value = [
      {
        classroomCode: 'ABCABC',
        classroomName: 'Matte 1A',
        totalScore: 120,
        globalRank: 1,
      },
      {
        classroomCode: 'DEFDEF',
        classroomName: 'Norsk Gruppe',
        totalScore: 90,
        globalRank: 2,
      },
    ]
    routeState.path = '/teacher/classrooms'
    routeState.query = {}
    routeState.params = {}
  })

  afterEach(() => {
    mountedWrappers.splice(0).forEach((wrapper) => wrapper.unmount())
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('fetches classrooms and renders the base sidebar actions', () => {
    const wrapper = mountTeacherMenu()

    expect(fetchClassrooms).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-test="logo"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Klasserom')
    expect(wrapper.text()).toContain('Nytt klasserom')
    expect(wrapper.find('[aria-label="Varsler"]').exists()).toBe(true)
    expect(wrapper.find('[aria-label="Innstillinger"]').exists()).toBe(true)
  })

  it('renders classrooms from the teacher classroom overview list', () => {
    const wrapper = mountTeacherMenu()

    expect(wrapper.text()).toContain('Matte 1A')
    expect(wrapper.text()).toContain('Norsk Gruppe')
    expect(wrapper.text()).not.toContain('Ingen klasserom')
  })

  it('shows the empty classroom message when the teacher has no classrooms', () => {
    classrooms.value = []

    const wrapper = mountTeacherMenu()

    expect(wrapper.text()).toContain('Ingen klasserom')
  })

  it('links to the teacher classrooms overview from the Klasserom button', () => {
    const wrapper = mountTeacherMenu()

    expect(wrapper.find('.teacher-card-title').attributes('data-to')).toBe('/teacher/classrooms')
  })

  it('links to the create classroom page from the new classroom button', () => {
    const wrapper = mountTeacherMenu()

    expect(wrapper.find('[data-to="/teacher/classrooms/new"]').exists()).toBe(true)
  })

  it('links to the selected classroom leaderboard using the classroom code', () => {
    const wrapper = mountTeacherMenu()
    const classroomButton = wrapper.findAll('.teacher-card-item-var-1').find((button) => button.text() === 'Norsk Gruppe')

    expect(classroomButton?.attributes('data-to')).toBe('/teacher/classrooms/DEFDEF/leaderboard')
  })

  it('marks the classrooms overview as active outside the new classroom route', () => {
    routeState.path = '/teacher/classrooms/ABCABC/leaderboard'
    routeState.params = { classroomCode: 'ABCABC' }

    const wrapper = mountTeacherMenu()

    const classroomItems = wrapper.findAll('.teacher-card-item-var-1')

    expect(wrapper.find('.teacher-card-title').classes()).toContain('button-variant-2')
    expect(classroomItems).toHaveLength(2)
    expect(classroomItems[0]?.classes()).toContain('button-variant-2')
  })

  it('marks the new classroom action as active on the new classroom route', () => {
    routeState.path = '/teacher/classrooms/new'

    const wrapper = mountTeacherMenu()

    expect(wrapper.find('[data-to="/teacher/classrooms/new"]').classes()).toContain('button-variant-2')
  })

  it('opens the classroom picker modal from the classroom picker button', async () => {
    const wrapper = mountTeacherMenu()

    expect(wrapper.find('[data-test="base-modal"]').exists()).toBe(false)

    await wrapper.find('.teacher-classroom-picker-button').trigger('click')

    expect(wrapper.find('[data-test="base-modal"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Velg klasserom')
    expect(wrapper.text()).toContain('Alle klasserom')
    expect(wrapper.text()).toContain('Matte 1A')
  })

  it('opens and closes the settings options menu from the settings button', async () => {
    const wrapper = mountTeacherMenu()

    expect(wrapper.find('[data-test="options-menu"]').exists()).toBe(false)

    await wrapper.find('[aria-label="Innstillinger"]').trigger('click')

    expect(wrapper.find('[data-test="options-menu"]').exists()).toBe(true)

    await wrapper.find('[data-test="options-menu"] button').trigger('click')

    expect(wrapper.find('[data-test="options-menu"]').exists()).toBe(false)
  })

  it('does not render an unread notification badge when all classrooms have zero unread notifications', async () => {
    const wrapper = mountTeacherMenu()

    await flushPromises()

    expect(wrapper.find('[data-test="notification-badge"]').exists()).toBe(false)
    expect(wrapper.find('[aria-label="Varsler"]').exists()).toBe(true)
  })

  it('renders the summed unread notification count across teacher classrooms', async () => {
    apiGet.mockImplementation(async (endpoint: string) => {
      if (endpoint.includes('ABCABC')) return { unreadNotificationsCount: 2 }
      if (endpoint.includes('DEFDEF')) return { unreadNotificationsCount: 3 }
      return { unreadNotificationsCount: 0 }
    })

    const wrapper = mountTeacherMenu()

    await flushPromises()

    expect(wrapper.find('[data-test="notification-badge"]').text()).toBe('5')
    expect(wrapper.find('[aria-label="Varsler, 5 uleste"]').exists()).toBe(true)
  })

  it('caps the notification badge display at 99+', async () => {
    apiGet.mockImplementation(async (endpoint: string) => {
      if (endpoint.includes('ABCABC')) return { unreadNotificationsCount: 70 }
      if (endpoint.includes('DEFDEF')) return { unreadNotificationsCount: 40 }
      return { unreadNotificationsCount: 0 }
    })

    const wrapper = mountTeacherMenu()

    await flushPromises()

    expect(wrapper.find('[data-test="notification-badge"]').text()).toBe('99+')
    expect(wrapper.find('[aria-label="Varsler, 110 uleste"]').exists()).toBe(true)
  })

  it('polls unread notifications every 30 seconds and clears polling on unmount', async () => {
    vi.useFakeTimers()
    const wrapper = mountTeacherMenu()
    await flushPromises()
    apiGet.mockClear()

    await vi.advanceTimersByTimeAsync(30000)
    await flushPromises()

    expect(apiGet).toHaveBeenCalledTimes(2)
    expect(apiGet).toHaveBeenCalledWith('/api/teacher/classrooms/ABCABC/notifications/unread')
    expect(apiGet).toHaveBeenCalledWith('/api/teacher/classrooms/DEFDEF/notifications/unread')

    wrapper.unmount()
    apiGet.mockClear()

    await vi.advanceTimersByTimeAsync(30000)
    await flushPromises()

    expect(apiGet).not.toHaveBeenCalled()
  })

  it('refreshes unread notifications after the notifications menu closes', async () => {
    const wrapper = mountTeacherMenu()
    await flushPromises()
    apiGet.mockClear()

    await wrapper.find('[aria-label="Varsler"]').trigger('click')
    await wrapper.find('[data-test="notifications-menu"] button').trigger('click')
    await flushPromises()

    expect(apiGet).toHaveBeenCalledTimes(2)
    expect(apiGet).toHaveBeenCalledWith('/api/teacher/classrooms/ABCABC/notifications/unread')
    expect(apiGet).toHaveBeenCalledWith('/api/teacher/classrooms/DEFDEF/notifications/unread')
  })
})
