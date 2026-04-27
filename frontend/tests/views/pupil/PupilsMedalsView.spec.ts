import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsMedalsView from '@/views/pupil/PupilsMedalsView.vue'
import { get } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

const mockedGet = vi.mocked(get)

const TopBarStub = {
  props: ['title', 'previousPageUrl'],
  template:
    '<div class="topbar-stub" :data-title="title" :data-previous-page-url="previousPageUrl"></div>',
}

function mountMedalsView() {
  return mount(PupilsMedalsView, {
    global: {
      stubs: {
        TopBar: TopBarStub,
      },
    },
  })
}

describe('PupilsMedalsView', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('uses the pupil navbar and forces back navigation to the pupil home view', () => {
    mockedGet.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountMedalsView()
    const topBar = wrapper.find('.topbar-stub')

    expect(topBar.attributes('data-title')).toBe('Medaljer')
    expect(topBar.attributes('data-previous-page-url')).toBe('/pupil')
  })

  it('fetches pupil medals from the backend endpoint', async () => {
    mockedGet.mockResolvedValue([])

    mountMedalsView()
    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/pupil/medals')
  })

  it('renders medals in a board with lock state and progress', async () => {
    mockedGet.mockResolvedValue([
      {
        medalKey: 'chapter-1',
        name: 'Nyhetskvartalet',
        description: 'Fullfør Nyhetskvartalet.',
        unlocked: true,
        imageFileObjectId: 10,
      },
      {
        medalKey: 'chapter-2',
        name: 'Fotografen',
        description: 'Fullfør Fotografen.',
        unlocked: false,
        imageFileObjectId: 11,
      },
      {
        medalKey: 'chapter-3',
        name: 'Datasenteret',
        description: 'Fullfør Datasenteret.',
        unlocked: false,
        imageFileObjectId: 12,
      },
      {
        medalKey: 'weekly-mystery',
        name: 'Ukens mysterium',
        description: 'Få et bidrag godkjent.',
        unlocked: true,
        imageFileObjectId: 13,
      },
    ])

    const wrapper = mountMedalsView()
    await flushPromises()

    expect(wrapper.findAll('.medal-slot')).toHaveLength(4)
    expect(wrapper.findAll('.medal-card')).toHaveLength(4)
    expect(wrapper.text()).toContain('2 av 4 låst opp')
    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.text()).toContain('Fotografen')
    expect(wrapper.findAll('.medal-slot--locked')).toHaveLength(2)
  })

  it('shows an error state when medals cannot be loaded', async () => {
    mockedGet.mockRejectedValue(new Error('Kunne ikke hente medaljer'))

    const wrapper = mountMedalsView()
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke hente medaljer')
    expect(wrapper.findAll('.medal-card')).toHaveLength(0)
  })
})
