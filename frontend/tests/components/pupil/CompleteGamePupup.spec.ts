import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import CompleteGamePopup from '@/components/pupil/CompleteGamePopup.vue'

const playSound = vi.fn()
const stopSound = vi.fn()

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
  return mount(CompleteGamePopup, {
    props: {
      show: true,
      success: true,
      image: '/victory.png',
      imageAlt: 'Seier bilde',
      kicker: 'Oppdrag fullført',
      title: 'Du klarte det!',
      message: 'Du svarte riktig på alle oppgavene.',
      subMessage: 'Bra jobbet!',
      highlightTitle: 'Spill fullført',
      highlightText: 'Du har fullført datasenteret.',
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

describe('CompleteGamePopup', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('requestAnimationFrame', vi.fn())
  })

  it('does not render when show is false', () => {
    const wrapper = mountPopup({ show: false })

    expect(wrapper.find('.game-overlay').exists()).toBe(false)
  })

  it('renders title, message and image when shown', () => {
    const wrapper = mountPopup()

    expect(wrapper.text()).toContain('Du klarte det!')
    expect(wrapper.text()).toContain('Du svarte riktig på alle oppgavene.')

    const img = wrapper.find('img')
    expect(img.attributes('src')).toBe('/victory.png')
    expect(img.attributes('alt')).toBe('Seier bilde')
  })

  it('shows highlight text when success is true', () => {
    const wrapper = mountPopup({ success: true })

    expect(wrapper.text()).toContain('Spill fullført')
    expect(wrapper.text()).toContain('Du har fullført datasenteret.')
  })

  it('does not show retry button when success is true', () => {
    const wrapper = mountPopup({ success: true })

    expect(wrapper.find('.retry-button').exists()).toBe(false)
    expect(wrapper.find('.map-button').exists()).toBe(true)
  })

  it('emits map when map button is clicked', async () => {
    const wrapper = mountPopup()

    await wrapper.find('.map-button').trigger('click')

    expect(wrapper.emitted('map')).toBeTruthy()
  })

  it('emits close when overlay is clicked', async () => {
    const wrapper = mountPopup()

    await wrapper.find('.game-overlay').trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('shows retry button when success is false', () => {
    const wrapper = mountPopup({
      success: false,
      image: '/defeat.png',
      imageAlt: 'Tap bilde',
      title: 'Du tapte!',
      message: 'Datatyven slapp unna.',
    })

    expect(wrapper.find('.retry-button').exists()).toBe(true)
    expect(wrapper.text()).toContain('Prøv på nytt')
  })

  it('emits retry when retry button is clicked', async () => {
    const wrapper = mountPopup({
      success: false,
      title: 'Du tapte!',
      message: 'Datatyven slapp unna.',
    })

    await wrapper.find('.retry-button').trigger('click')

    expect(wrapper.emitted('retry')).toBeTruthy()
  })

  it('uses fallback alt text for success image', () => {
    const wrapper = mountPopup({
      imageAlt: undefined,
      success: true,
    })

    expect(wrapper.find('img').attributes('alt')).toBe('Seier')
  })

  it('uses fallback alt text for defeat image', () => {
    const wrapper = mountPopup({
      imageAlt: undefined,
      success: false,
    })

    expect(wrapper.find('img').attributes('alt')).toBe('Tap')
  })

  it('shows the medal reward for a successful game completion', () => {
    const wrapper = mountPopup({
      completedChapterMedalNewlyAwarded: true,
      completedChapterMedal: {
        medalKey: 'chapter-3',
        name: 'Datasenteret',
        description: 'Fullfør Datasenteret.',
        unlocked: true,
        imageFileObjectId: 103,
      },
    })

    expect(wrapper.text()).toContain('Ny medalje låst opp')
    expect(wrapper.text()).toContain('Datasenteret')
    expect(wrapper.find('[data-test="medal-image"]').attributes('src')).toBe('/api/objects/103')
  })

  it('shows the trophy stage for a first-time Datasenteret grand celebration', () => {
    const wrapper = mountPopup({
      grandCelebration: true,
      completedChapterMedalNewlyAwarded: true,
      completedChapterMedal: {
        medalKey: 'chapter-3',
        name: 'Datasenteret',
        description: 'Fullfør Datasenteret.',
        unlocked: true,
        imageFileObjectId: 103,
      },
    })

    expect(wrapper.find('[data-test="trophy-stage"]').exists()).toBe(true)
    expect(wrapper.find('.trophy-image').attributes('src')).toContain('trophy.png')
    expect(wrapper.find('.trophy-image').attributes('alt')).toBe('Trofé')
    expect(wrapper.text()).toContain('Du har vunnet trofeet i Datasenteret.')
  })
})
