import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import TopBar from '@/components/pupil/TopBar.vue'
import { get } from '@/utils/apiUtils'

const routerPush = vi.fn()

let routePath = '/pupil/classroom/map'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
  useRoute: () => ({
    get fullPath() {
      return routePath
    },
    get path() {
      return routePath
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
    routePath = '/pupil/classroom/map'

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
    routePath = '/pupil'

    const wrapper = mountTopBar()

    await flushPromises()

    expect(wrapper.find('.nav-icon-parent').exists()).toBe(false)
  })

  it('routes from map page back to pupil dashboard', async () => {
    const wrapper = mountTopBar()

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil')
  })

  it.each([
    '/pupil/classroom/map/news',
    '/pupil/classroom/map/photographer',
    '/pupil/classroom/map/datacenter',
  ])('routes from chapter task page %s back to map', async (path) => {
    routePath = path
    const wrapper = mountTopBar()

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map')
  })

  it.each([
    '/pupil/classroom/notebook',
    '/pupil/classroom/mystery',
    '/pupil/classroom/medals',
    '/pupil/classroom/help',
    '/pupil/classroom/leaderboard',
    '/pupil/profile',
  ])('routes from feature page %s back to pupil dashboard', async (path) => {
    routePath = path
    const wrapper = mountTopBar()

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil')
  })

  it('routes from pupil avatar edit back to profile', async () => {
    routePath = '/pupil/profile/avatar'
    const wrapper = mountTopBar()

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/profile')
  })

  it('routes unmapped pupil pages back to pupil dashboard', async () => {
    routePath = '/pupil/classroom/unknown'
    const wrapper = mountTopBar()

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil')
  })

  it('uses previousPageUrl when back button is clicked', async () => {
    routePath = '/pupil/classroom/map/news'
    const wrapper = mountTopBar({
      previousPageUrl: '/pupil/custom-back',
    })

    await flushPromises()

    await wrapper.find('button.nav-icon-parent').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/custom-back')
  })
})
