import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TopBar from '@/components/pupil/TopBar.vue'
import { get } from '@/utils/apiUtils'

const routerPush = vi.fn()
const historyBack = vi.fn()

let routeFullPath = '/pupil/classroom/map'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
  useRoute: () => ({
    get fullPath() {
      return routeFullPath
    },
  }),
  RouterLink: {
    props: ['to'],
    template: '<a :href="to"><slot /></a>',
  },
}))

const mockedGet = vi.mocked(get)

function mountTopBar(props = {}) {
  return mount(TopBar, {
    props: {
      title: 'Testside',
      ...props,
    },
    global: {
      stubs: {
        ChevronLeftIcon: {
          template: '<span data-test="back-icon" />',
        },
        HomeIcon: {
          template: '<span data-test="home-icon" />',
        },
        RouterLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>',
        },
      },
    },
  })
}

describe('TopBar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeFullPath = '/pupil/classroom/map'

    vi.spyOn(window.history, 'back').mockImplementation(historyBack)

    mockedGet.mockResolvedValue({
      email: 'ola@example.com',
      legalName: 'Ola Nordmann',
      role: 'PUPIL',
      username: 'Ola',
      avatarFileObjectId: 123,
    })
  })

  it('renders title', () => {
    const wrapper = mountTopBar()

    expect(wrapper.text()).toContain('Testside')
  })

  it('loads and shows profile username and avatar', async () => {
    const wrapper = mountTopBar()

    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/me')
    expect(wrapper.text()).toContain('Ola')

    const img = wrapper.find('.avatar-button img')
    expect(img.attributes('src')).toBe('/api/objects/123')
  })

  it('uses default avatar when profile has no avatar', async () => {
    mockedGet.mockResolvedValueOnce({
      email: 'ola@example.com',
      legalName: 'Ola Nordmann',
      role: 'PUPIL',
      username: null,
      avatarFileObjectId: null,
    })

    const wrapper = mountTopBar()

    await flushPromises()

    const img = wrapper.find('.avatar-button img')
    expect(img.attributes('src')).toBe('/api/avatar-assets/costume/detective.png')
  })

  it('uses legal name when username is missing', async () => {
    mockedGet.mockResolvedValueOnce({
      email: 'ola@example.com',
      legalName: 'Ola Nordmann',
      role: 'PUPIL',
      username: null,
      avatarFileObjectId: null,
    })

    const wrapper = mountTopBar()

    await flushPromises()

    expect(wrapper.text()).toContain('Ola Nordmann')
  })

  it('uses pupil routes for pupil profile', async () => {
    const wrapper = mountTopBar()

    await flushPromises()

    expect(wrapper.find('a[href="/pupil"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/pupil/profile"]').exists()).toBe(true)
  })

  it('uses teacher routes for teacher profile', async () => {
    mockedGet.mockResolvedValueOnce({
      email: 'teacher@example.com',
      legalName: 'Teacher Name',
      role: 'TEACHER',
      username: 'Teacher',
      avatarFileObjectId: null,
    })

    const wrapper = mountTopBar()

    await flushPromises()

    expect(wrapper.find('a[href="/teacher/classrooms"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/teacher/profile"]').exists()).toBe(true)
  })

  it('does not show back and home buttons on home route', async () => {
    routeFullPath = '/pupil'

    const wrapper = mountTopBar()

    await flushPromises()

    expect(wrapper.find('.nav-icon-parent').exists()).toBe(false)
  })

  it('goes back with browser history when no previousPageUrl is given', async () => {
    const wrapper = mountTopBar()

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(historyBack).toHaveBeenCalled()
  })

  it('uses previousPageUrl when back button is clicked', async () => {
    const wrapper = mountTopBar({
      previousPageUrl: '/pupil/custom-back',
    })

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/custom-back')
  })
})
