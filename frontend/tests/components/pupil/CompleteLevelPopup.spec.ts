import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import CompleteLevelPopup from '@/components/pupil/CompleteLevelPopup.vue'

const routerPush = vi.fn()
const playSound = vi.fn()
const stopSound = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

vi.mock('@/composables/useAudio', () => ({
  useAudio: () => ({
    playSound,
    stopSound,
  }),
}))

vi.mock('canvas-confetti', () => ({
  default: vi.fn(),
}))

function mountPopup(props = {}) {
  return mount(CompleteLevelPopup, {
    props: {
      show: true,
      success: true,
      title: 'Test title',
      message: 'Test message',
      subMessage: 'Test sub message',
      successImage: '/success.png',
      failImage: '/fail.png',
      ...props,
    },
    global: {
      stubs: {
        AuthenticatedImage: {
          props: ['src', 'alt'],
          template: '<img data-test="medal-image" :src="src" :alt="alt" />',
        },
      },
    },
  })
}

describe('CompleteLevelPopup', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('requestAnimationFrame', vi.fn())
  })

  it('does not render when show is false', () => {
    const wrapper = mountPopup({ show: false })

    expect(wrapper.find('.popup-overlay').exists()).toBe(false)
  })

  it('renders title, message and sub message when shown', () => {
    const wrapper = mountPopup()

    expect(wrapper.text()).toContain('Test title')
    expect(wrapper.text()).toContain('Test message')
    expect(wrapper.text()).toContain('Test sub message')
  })

  it('shows success button when success is true', () => {
    const wrapper = mountPopup({ success: true })

    expect(wrapper.text()).toContain('Tilbake til kartet')
    expect(wrapper.text()).not.toContain('Prøv på nytt')
  })

  it('navigates to map when success map button is clicked', async () => {
    const wrapper = mountPopup({ success: true })

    await wrapper.find('.popup-btn-success').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map')
  })

  it('shows retry and map buttons when success is false', () => {
    const wrapper = mountPopup({ success: false })

    expect(wrapper.text()).toContain('Prøv på nytt')
    expect(wrapper.text()).toContain('Tilbake til kartet')
  })

  it('emits retry when retry button is clicked', async () => {
    const wrapper = mountPopup({ success: false })

    await wrapper.find('.popup-btn-play').trigger('click')

    expect(wrapper.emitted('retry')).toBeTruthy()
  })

  it('emits map when failed map button is clicked', async () => {
    const wrapper = mountPopup({ success: false })

    await wrapper.find('.popup-btn-map').trigger('click')

    expect(wrapper.emitted('map')).toBeTruthy()
  })

  it('emits close when overlay is clicked', async () => {
    const wrapper = mountPopup()

    await wrapper.find('.popup-overlay').trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('shows correct image and alt text for success', () => {
    const wrapper = mountPopup({ success: true })

    const img = wrapper.find('img')

    expect(img.attributes('src')).toBe('/success.png')
    expect(img.attributes('alt')).toBe('Riktig svar')
  })

  it('shows correct image and alt text for failure', () => {
    const wrapper = mountPopup({ success: false })

    const img = wrapper.find('img')

    expect(img.attributes('src')).toBe('/fail.png')
    expect(img.attributes('alt')).toBe('Feil svar')
  })

  it('renders newly awarded medal details for successful chapter completion', () => {
    const wrapper = mountPopup({
      success: true,
      completedChapterMedalNewlyAwarded: true,
      completedChapterMedal: {
        medalKey: 'chapter-1',
        name: 'Nyhetskvartalet',
        description: 'Fullfør Nyhetskvartalet.',
        unlocked: true,
        imageFileObjectId: 101,
      },
    })

    expect(wrapper.text()).toContain('Ny medalje låst opp')
    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.find('[data-test="medal-image"]').attributes('src')).toBe('/api/objects/101')
  })

  it('renders already unlocked wording for repeated successful completion', () => {
    const wrapper = mountPopup({
      success: true,
      completedChapterMedalNewlyAwarded: false,
      completedChapterMedal: {
        medalKey: 'chapter-2',
        name: 'Fotografen',
        description: 'Fullfør Fotografen.',
        unlocked: true,
        imageFileObjectId: 102,
      },
    })

    expect(wrapper.text()).toContain('Medaljen din')
    expect(wrapper.text()).toContain('Du har allerede låst opp denne medaljen')
  })
})
