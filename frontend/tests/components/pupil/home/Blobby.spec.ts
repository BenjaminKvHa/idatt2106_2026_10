import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

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

  it('renders blobby button and image', () => {
    const wrapper = mount(Blobby)

    expect(wrapper.find('.blobby-button').exists()).toBe(true)

    const image = wrapper.find('img')
    expect(image.attributes('alt')).toBe('Blobby')
  })

  it('shows message on first click', async () => {
    const wrapper = mount(Blobby)

    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.text()).toContain(
      'Hei mitt navn er Blobby. Trykk på meg for å høre hvilke lyder jeg kan lage!',
    )
    expect(playSound).not.toHaveBeenCalled()
  })

  it('hides message and plays sound on second click', async () => {
    const wrapper = mount(Blobby)

    await wrapper.find('.blobby-button').trigger('click')
    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.find('.blobby-message').exists()).toBe(false)
    expect(playSound).toHaveBeenCalledTimes(1)
  })

  it('plays one sound for each sound click', async () => {
    const wrapper = mount(Blobby)

    await wrapper.find('.blobby-button').trigger('click')

    for (let i = 0; i < 5; i += 1) {
      await wrapper.find('.blobby-button').trigger('click')
    }

    expect(playSound).toHaveBeenCalledTimes(5)
  })

  it('animates blobby after all sounds have been played', async () => {
    const wrapper = mount(Blobby)

    await wrapper.find('.blobby-button').trigger('click')

    for (let i = 0; i < 6; i += 1) {
      await wrapper.find('.blobby-button').trigger('click')
    }

    expect(wrapper.find('.blobby-button').classes()).toContain('animate')
    expect(wrapper.find('.blobby-image').classes()).toContain('wiggle')

    vi.advanceTimersByTime(800)
    await wrapper.vm.$nextTick()

    expect(wrapper.find('.blobby-button').classes()).not.toContain('animate')
    expect(wrapper.find('.blobby-image').classes()).not.toContain('wiggle')
  })
})
