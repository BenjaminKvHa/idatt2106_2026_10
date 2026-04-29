import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ProfileButton from '@/components/pupil/ProfileButton.vue'

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

vi.mock('@/composables/exampleUser.ts', () => ({
  user: {
    value: {
      avatarUrl: '/test-avatar.png',
    },
  },
}))

vi.mock('@/utils/jwtUtils.ts', () => ({
  getJwtData: vi.fn(() => ({
    legalName: 'Test Bruker',
    email: 'test@example.com',
  })),
}))

describe('ProfileButton', () => {
  beforeEach(() => {
    pushMock.mockClear()
    vi.spyOn(history, 'back').mockImplementation(() => {})
  })

  it('renders profile button', () => {
    const wrapper = mount(ProfileButton)

    expect(wrapper.find('button').exists()).toBe(true)
    expect(wrapper.find('img').exists()).toBe(true)
    expect(wrapper.text()).toContain('Test Bruker')
  })

  it('uses default topbar variant', () => {
    const wrapper = mount(ProfileButton)

    expect(wrapper.find('button').classes()).toContain('profile-btn--topbar')
  })

  it('can use home variant', () => {
    const wrapper = mount(ProfileButton, {
      props: {
        variant: 'home',
      },
    })

    expect(wrapper.find('button').classes()).toContain('profile-btn--home')
  })

  it('shows avatar with correct src and alt text', () => {
    const wrapper = mount(ProfileButton)

    const img = wrapper.find('img')

    expect(img.attributes('src')).toBe('/test-avatar.png')
    expect(img.attributes('alt')).toBe('Test Bruker')
  })

  it('navigates to default profile route when clicked', async () => {
    const wrapper = mount(ProfileButton)

    await wrapper.find('button').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/pupil/profile')
  })

  it('navigates to custom route when clicked', async () => {
    const wrapper = mount(ProfileButton, {
      props: {
        to: '/custom-profile',
      },
    })

    await wrapper.find('button').trigger('click')

    expect(pushMock).toHaveBeenCalledWith('/custom-profile')
  })
})
