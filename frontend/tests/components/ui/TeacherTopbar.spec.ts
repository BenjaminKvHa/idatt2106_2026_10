import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import TeacherTopbar from '@/components/ui/TeacherTopbar.vue'
import { useTeacherTopbar } from '@/composables/useTeacherTopbar'

const routeState = {
  path: '/teacher/classrooms/matte-1a/oversikt',
  params: {
    classroomName: 'matte-1a',
  } as Record<string, string>,
  matched: [] as Array<{
    path: string
    meta?: Record<string, unknown>
  }>,
}

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({
    currentRoute: {
      value: routeState,
    },
  }),
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

function mountTeacherTopbar(props?: Record<string, unknown>) {
  return mount(TeacherTopbar, {
    props,
    global: {
      stubs: {
        RouterLink: routerLinkStub,
      },
    },
  })
}

describe('TeacherTopbar', () => {
  beforeEach(() => {
    routeState.path = '/teacher/classrooms/matte-1a/oversikt'
    routeState.params = {
      classroomName: 'matte-1a',
    }
    routeState.matched = []
    useTeacherTopbar().clearTabs()
  })

  afterEach(() => {
    useTeacherTopbar().clearTabs()
    vi.restoreAllMocks()
  })

  it('renders an explicit breadcrumb path and title from props', () => {
    const wrapper = mountTeacherTopbar({
      path: [
        { label: 'Klasserom', to: '/teacher/classrooms' },
        { label: 'Matte 1A' },
      ],
    })

    expect(wrapper.find('.topbar-path').text()).toContain('Klasserom')
    expect(wrapper.find('.topbar-path').text()).toContain('Matte 1A')
    expect(wrapper.find('a[href="/teacher/classrooms"]').exists()).toBe(true)
    expect(wrapper.find('.topbar-title').text()).toBe('Matte 1A')
  })

  it('renders explicit tabs and marks the current route as active', () => {
    routeState.path = '/teacher/classrooms/matte-1a/pupils'

    const wrapper = mountTeacherTopbar({
      tabs: [
        { label: 'Oversikt', to: '/teacher/classrooms/matte-1a/oversikt' },
        { label: 'Elever', to: '/teacher/classrooms/matte-1a/pupils' },
      ],
    })

    const tabs = wrapper.findAll('.topbar-tab')

    expect(tabs.map((tab) => tab.text())).toEqual(['Oversikt', 'Elever'])
    expect(tabs[0]?.classes()).toContain('button-variant-2')
    expect(tabs[1]?.classes()).toContain('button-variant-1')
  })

  it('builds breadcrumbs from route meta and fills route params for parent links', () => {
    routeState.matched = [
      {
        path: '/teacher/classrooms',
        meta: {
          breadcrumb: 'Klasserom',
        },
      },
      {
        path: '/teacher/classrooms/:classroomName',
        meta: {
          breadcrumb: () => 'Matte 1A',
        },
      },
      {
        path: '/teacher/classrooms/:classroomName/oversikt',
        meta: {
          breadcrumb: 'Oversikt',
        },
      },
    ]

    const wrapper = mountTeacherTopbar()

    expect(wrapper.find('.topbar-path').text()).toContain('Klasserom')
    expect(wrapper.find('.topbar-path').text()).toContain('Matte 1A')
    expect(wrapper.find('.topbar-path').text()).toContain('Oversikt')
    expect(wrapper.find('a[href="/teacher/classrooms/matte-1a"]').exists()).toBe(true)
    expect(wrapper.find('.topbar-title').text()).toBe('Oversikt')
  })

  it('uses tabs from route meta when no explicit tabs are provided', () => {
    routeState.path = '/teacher/classrooms/matte-1a/oversikt'
    routeState.matched = [
      {
        path: '/teacher/classrooms/:classroomName',
        meta: {
          tabs: () => [
            { label: 'Oversikt', to: '/teacher/classrooms/matte-1a/oversikt' },
            { label: 'Elever', to: '/teacher/classrooms/matte-1a/pupils' },
          ],
        },
      },
    ]

    const wrapper = mountTeacherTopbar()

    const tabs = wrapper.findAll('.topbar-tab')
    expect(tabs.map((tab) => tab.text())).toEqual(['Oversikt', 'Elever'])
    expect(tabs[0]?.classes()).toContain('button-variant-1')
  })

  it('uses composable tabs before route meta tabs', () => {
    useTeacherTopbar().setTabs([{ label: 'Aktiv klasse', to: '/teacher/classrooms/matte-1a/oversikt' }])
    routeState.matched = [
      {
        path: '/teacher/classrooms/:classroomName',
        meta: {
          tabs: () => [{ label: 'Meta tab', to: '/teacher/classrooms/matte-1a/pupils' }],
        },
      },
    ]

    const wrapper = mountTeacherTopbar()

    expect(wrapper.findAll('.topbar-tab').map((tab) => tab.text())).toEqual(['Aktiv klasse'])
  })
})
