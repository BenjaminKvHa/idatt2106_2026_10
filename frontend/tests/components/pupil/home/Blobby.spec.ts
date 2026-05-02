import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import Blobby from '@/components/pupil/home/Blobby.vue'

const playSound = vi.fn()

vi.mock('@/composables/useAudio', () => ({
  useAudio: () => ({
    playSound,
  }),
}))

describe('Blobby', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()

    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
      callback(0)
      return 1
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('renders blobby button and image', () => {
    const wrapper = mount(Blobby)

    expect(wrapper.find('.blobby-button').exists()).toBe(true)

    const image = wrapper.find('img')
    expect(image.attributes('alt')).toBe('Blobby')
  })

  it('shows message and plays sound on first click', async () => {
    const wrapper = mount(Blobby)

    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.text()).toContain(
      'Hei mitt navn er Blobby. Trykk på meg for å høre hvilke lyder jeg kan lage!',
    )
    expect(playSound).toHaveBeenCalledTimes(1)
  })

  it('keeps message visible and plays another sound on second click', async () => {
    const wrapper = mount(Blobby)

    await wrapper.find('.blobby-button').trigger('click')
    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.find('.blobby-message').exists()).toBe(true)
    expect(playSound).toHaveBeenCalledTimes(2)
  })

  it('closes message when clicking outside blobby', async () => {
    const wrapper = mount(Blobby, {
      attachTo: document.body,
    })

    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.find('.blobby-message').exists()).toBe(true)

    document.dispatchEvent(new MouseEvent('click'))
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.blobby-message').exists()).toBe(false)

    wrapper.unmount()
  })

  it('plays one sound for each of the five first clicks', async () => {
    const wrapper = mount(Blobby)
    const blobbyButton = wrapper.find('.blobby-button')

    for (let i = 0; i < 5; i += 1) {
      await blobbyButton.trigger('click')
    }

    expect(playSound).toHaveBeenCalledTimes(5)
  })

  it('animates blobby after all sounds have been played', async () => {
    const wrapper = mount(Blobby)
    const blobbyButton = wrapper.find('.blobby-button')

    for (let i = 0; i < 6; i += 1) {
      await blobbyButton.trigger('click')
    }

    expect(playSound).toHaveBeenCalledTimes(5)
    expect(wrapper.find('.blobby-button').classes()).toContain('animate')
    expect(wrapper.find('.blobby-image').classes()).toContain('wiggle')

    await vi.advanceTimersByTimeAsync(800)

    expect(wrapper.find('.blobby-button').classes()).not.toContain('animate')
    expect(wrapper.find('.blobby-image').classes()).not.toContain('wiggle')
  })

  it('renders custom message when prop is provided', async () => {
    const customMessage = 'Velkommen til hjemskjermen.'

    const wrapper = mount(Blobby, {
      props: {
        message: customMessage,
      },
    })

    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.text()).toContain(customMessage)
  })
})
