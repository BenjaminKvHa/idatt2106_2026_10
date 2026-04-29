import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilView from '@/views/pupil/PupilView.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

function mountPupilView() {
  return mount(PupilView, {
    global: {
      stubs: {
        TopBar: true,
        RouterLink: routerLinkStub,
        RouterView: true,
      },
    },
  })
}

describe('PupilView', () => {
  let audioPlay: ReturnType<typeof vi.fn>
  let audioConstructor: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.useFakeTimers()

    audioPlay = vi.fn().mockResolvedValue(undefined)
    audioConstructor = vi.fn(function (this: {
      play: typeof audioPlay
      pause: ReturnType<typeof vi.fn>
      addEventListener: ReturnType<typeof vi.fn>
      removeEventListener: ReturnType<typeof vi.fn>
      currentTime: number
      volume: number
    }) {
      this.play = audioPlay
      this.pause = vi.fn()
      this.addEventListener = vi.fn()
      this.removeEventListener = vi.fn()
      this.currentTime = 0
      this.volume = 1
    })

    vi.stubGlobal('Audio', audioConstructor as unknown as typeof Audio)
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
      callback(0)
      return 1
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('renders the six expected homescreen navigation icons', () => {
    const wrapper = mountPupilView()

    const links = wrapper.findAll('.overlay-button')

    expect(links).toHaveLength(6)
    expect(links.map((link) => link.find('img').exists())).toEqual([
      true,
      true,
      true,
      true,
      true,
      true,
    ])
  })

  it('keeps each homescreen icon linked to the correct pupil route and positioning class', () => {
    const wrapper = mountPupilView()

    const expectedLinks = [
      { className: 'notes', route: '/pupil/classroom/notebook' },
      { className: 'mystery', route: '/pupil/classroom/mystery' },
      { className: 'medals', route: '/pupil/classroom/medals' },
      { className: 'map', route: '/pupil/classroom/map' },
      { className: 'leaderboard', route: '/pupil/classroom/leaderboard' },
      { className: 'help', route: '/pupil/classroom/help' },
    ]

    for (const expectedLink of expectedLinks) {
      const link = wrapper.find(`.overlay-button.${expectedLink.className}`)

      expect(link.exists()).toBe(true)
      expect(link.attributes('href')).toBe(expectedLink.route)
    }
  })

  it('shows the Blobby introduction message on the first click', async () => {
    const wrapper = mountPupilView()

    await wrapper.find('.blobby-button').trigger('click')

    expect(wrapper.find('.blobby-message').text()).toBe(
      'Hei mitt navn er Blobby. Trykk på meg for å høre hvilke lyder jeg kan lage!',
    )
    expect(audioConstructor).not.toHaveBeenCalled()
  })

  it('plays a Blobby sound after the introduction step', async () => {
    const wrapper = mountPupilView()
    const blobbyButton = wrapper.find('.blobby-button')

    await blobbyButton.trigger('click')
    await blobbyButton.trigger('click')

    expect(audioConstructor).toHaveBeenCalledTimes(1)
    expect(audioPlay).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.blobby-message').exists()).toBe(false)
  })

  it('resets Blobby and clears the animation after the final interaction', async () => {
    const wrapper = mountPupilView()
    const blobbyButton = wrapper.find('.blobby-button')

    for (let clickCount = 0; clickCount < 7; clickCount += 1) {
      await blobbyButton.trigger('click')
    }

    expect(blobbyButton.classes()).toContain('animate')

    await vi.advanceTimersByTimeAsync(800)

    expect(blobbyButton.classes()).not.toContain('animate')
    expect(wrapper.find('.blobby-message').exists()).toBe(false)
  })
})
