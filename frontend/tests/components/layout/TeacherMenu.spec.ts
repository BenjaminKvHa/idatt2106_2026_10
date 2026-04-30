import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import TeacherMenu from '@/components/teacher/TeacherMenu.vue'

const routerPush = vi.fn()
const fetchClassrooms = vi.fn()
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

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({
    push: routerPush,
  }),
}))

function mountTeacherMenu() {
  return mount(TeacherMenu, {
    global: {
      stubs: {
        Logo: {
          template: '<div data-test="logo" />',
        },
        OptionsMenu: {
          emits: ['close'],
          template: '<section data-test="options-menu"><button type="button" @click="$emit(\'close\')">Close</button></section>',
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
}

describe('TeacherMenu', () => {
  beforeEach(() => {
    routerPush.mockReset()
    fetchClassrooms.mockReset()
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

    expect(wrapper.find('.teacher-menu-button').attributes('data-to')).toBe('/teacher/classrooms/new')
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

    expect(wrapper.find('.teacher-menu-button').classes()).toContain('button-variant-2')
  })

  it('opens and closes the settings options menu from the settings button', async () => {
    const wrapper = mountTeacherMenu()

    expect(wrapper.find('[data-test="options-menu"]').exists()).toBe(false)

    await wrapper.find('[aria-label="Innstillinger"]').trigger('click')

    expect(wrapper.find('[data-test="options-menu"]').exists()).toBe(true)

    await wrapper.find('[data-test="options-menu"] button').trigger('click')

    expect(wrapper.find('[data-test="options-menu"]').exists()).toBe(false)
  })
})
