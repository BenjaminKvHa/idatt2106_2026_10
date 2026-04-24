import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsMapView from '@/views/pupil/PupilsMapView.vue'
import { get } from '@/utils/apiUtils'

const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

const mockedGet = vi.mocked(get)

function mountMapView() {
  return mount(PupilsMapView, {
    global: {
      stubs: {
        TopBar: true,
      },
    },
  })
}

function findStopNode(wrapper: VueWrapper, stopName: string) {
  const stopNode = wrapper.findAll('.stop-node').find((node) => node.text().includes(stopName))

  if (!stopNode) {
    throw new Error(`Could not find stop node for ${stopName}`)
  }

  return stopNode
}

async function clickStop(wrapper: VueWrapper, stopName: string) {
  await findStopNode(wrapper, stopName).find('.stop-circle').trigger('click')
}

describe('PupilsMapView', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    routerPush.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads pupil progress from the authenticated progress endpoint', async () => {
    mockedGet.mockResolvedValue([
      { chapterId: 1, highscore: 2, unlocked: true },
      { chapterId: 2, highscore: 0, unlocked: false },
      { chapterId: 3, highscore: 0, unlocked: false },
    ])

    const wrapper = mountMapView()

    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/pupil/progress')
    expect(wrapper.text()).not.toContain('Laster progresjon...')
  })

  it('lets pupils play Nyhetskvartalet even when the progress response has no chapter row', async () => {
    mockedGet.mockResolvedValue([])

    const wrapper = mountMapView()

    await flushPromises()
    await clickStop(wrapper, 'Nyhetskvartalet')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map/news')
  })

  it('lets pupils play Fotografen when backend progress marks chapter 2 unlocked', async () => {
    mockedGet.mockResolvedValue([
      { chapterId: 1, highscore: 3, unlocked: true },
      { chapterId: 2, highscore: 1, unlocked: true },
      { chapterId: 3, highscore: 0, unlocked: false },
    ])

    const wrapper = mountMapView()

    await flushPromises()
    await clickStop(wrapper, 'Fotografen')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map/photographer')
  })

  it('opens the locked popup instead of playing Fotografen when backend progress keeps it locked', async () => {
    mockedGet.mockResolvedValue([
      { chapterId: 1, highscore: 0, unlocked: true },
      { chapterId: 2, highscore: 0, unlocked: false },
      { chapterId: 3, highscore: 0, unlocked: false },
    ])

    const wrapper = mountMapView()

    await flushPromises()
    await clickStop(wrapper, 'Fotografen')

    expect(routerPush).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Fotografen er låst.')
    expect(wrapper.text()).toContain('Få 3 stjerner på Nyhetskvartalet for å låse det opp!')
  })

  it('lets pupils play Datasenteret when backend progress marks chapter 3 unlocked', async () => {
    mockedGet.mockResolvedValue([
      { chapterId: 1, highscore: 3, unlocked: true },
      { chapterId: 2, highscore: 3, unlocked: true },
      { chapterId: 3, highscore: 5, unlocked: true },
    ])

    const wrapper = mountMapView()

    await flushPromises()
    await clickStop(wrapper, 'Datasenteret')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map/datacenter')
  })

  it('caps rendered stars at each stop max when backend highscore is too high', async () => {
    mockedGet.mockResolvedValue([
      { chapterId: 1, highscore: 9, unlocked: true },
      { chapterId: 2, highscore: 9, unlocked: true },
      { chapterId: 3, highscore: 9, unlocked: true },
    ])

    const wrapper = mountMapView()

    await flushPromises()

    expect(findStopNode(wrapper, 'Nyhetskvartalet').findAll('.star.filled')).toHaveLength(3)
    expect(findStopNode(wrapper, 'Fotografen').findAll('.star.filled')).toHaveLength(3)
    expect(findStopNode(wrapper, 'Datasenteret').findAll('.star.filled')).toHaveLength(5)
  })

  it('shows a progress error and does not navigate while progress is unavailable', async () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined)
    mockedGet.mockRejectedValue(new Error('Progress unavailable'))

    const wrapper = mountMapView()

    await flushPromises()
    await clickStop(wrapper, 'Datasenteret')

    expect(consoleError).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Kunne ikke hente progresjon akkurat nå.')
    expect(routerPush).not.toHaveBeenCalled()
  })
})
