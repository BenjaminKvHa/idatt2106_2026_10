import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilView from '@/views/pupil/PupilView.vue'

const routerLinkStub = {
  props: ['to'],
  emits: ['click'],
  template: '<a :href="to" @click="$emit(\'click\', $event)"><slot /></a>',
}

const pinboardLayoutStub = {
  template: '<div class="pinboard-layout"><slot /></div>',
}

function mountPupilView() {
  return mount(PupilView, {
    global: {
      stubs: {
        TopBar: true,
        PinboardLayout: pinboardLayoutStub,
        Blobby: true,
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
  })

  afterEach(() => {
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

  it('only marks the map button as having its own click sound', () => {
    const wrapper = mountPupilView()

    expect(wrapper.find('.overlay-button.map').attributes('data-no-click-sound')).toBe('true')
    expect(wrapper.find('.overlay-button.notes').attributes('data-no-click-sound')).toBeUndefined()
    expect(
      wrapper.find('.overlay-button.mystery').attributes('data-no-click-sound'),
    ).toBeUndefined()
    expect(wrapper.find('.overlay-button.medals').attributes('data-no-click-sound')).toBeUndefined()
    expect(
      wrapper.find('.overlay-button.leaderboard').attributes('data-no-click-sound'),
    ).toBeUndefined()
    expect(wrapper.find('.overlay-button.help').attributes('data-no-click-sound')).toBeUndefined()
  })

  it('plays the map opening sound when the map button is clicked', async () => {
    const wrapper = mountPupilView()

    await wrapper.find('.overlay-button.map').trigger('click')

    expect(audioConstructor).toHaveBeenCalledTimes(1)
    expect(audioPlay).toHaveBeenCalledTimes(1)
  })
})
