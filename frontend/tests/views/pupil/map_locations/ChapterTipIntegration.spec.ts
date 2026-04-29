import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DatacenterView from '@/views/pupil/map_locations/DatacenterView.vue'
import NewsView from '@/views/pupil/map_locations/NewsView.vue'
import PhotographerView from '@/views/pupil/map_locations/PhotographerView.vue'
import { get, getBlob } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  getBlob: vi.fn(),
  post: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}))

vi.mock('canvas-confetti', () => ({
  default: vi.fn(),
}))

const mockedGet = vi.mocked(get)
const mockedGetBlob = vi.mocked(getBlob)

const globalStubs = {
  TopBar: true,
  TaskGuide: true,
  CompleteLevelPopup: true,
  CompleteGamePopup: true,
  PupilImageTask: true,
  PupilNewsTask: true,
  HintCreature: {
    props: ['chapterId'],
    template: '<div class="hint-creature-stub" :data-chapter-id="chapterId"><slot /></div>',
  },
}

function stubObjectUrls() {
  Object.defineProperty(URL, 'createObjectURL', {
    value: vi.fn(() => 'blob:test-image'),
    configurable: true,
  })
  Object.defineProperty(URL, 'revokeObjectURL', {
    value: vi.fn(),
    configurable: true,
  })
}

describe('chapter task tip integration', () => {
  beforeEach(() => {
    mockedGet.mockReset()
    mockedGetBlob.mockReset()
    mockedGetBlob.mockResolvedValue(new Blob(['image'], { type: 'image/png' }))
    stubObjectUrls()
  })

  it('passes chapter id 1 to the Nyhetskvartalet Blobby tip component', async () => {
    mockedGet.mockResolvedValue({
      taskId: 10,
      taskDescription: 'Finn den falske nyheten.',
      fileIds: [1, 2],
    })

    const wrapper = mount(NewsView, {
      global: {
        stubs: globalStubs,
      },
    })

    await flushPromises()

    expect(wrapper.find('.hint-creature-stub').attributes('data-chapter-id')).toBe('1')
  })

  it('passes chapter id 2 to the Fotografen Blobby tip component', async () => {
    mockedGet.mockResolvedValue({
      taskId: 20,
      taskDescription: 'Vurder bildet.',
      fileId: 3,
      justifications: ['Detaljene stemmer ikke'],
    })

    const wrapper = mount(PhotographerView, {
      global: {
        stubs: globalStubs,
      },
    })

    await flushPromises()

    expect(wrapper.find('.hint-creature-stub').attributes('data-chapter-id')).toBe('2')
  })

  it('passes chapter id 3 to the Datasenteret Blobby tip component', async () => {
    mockedGet.mockResolvedValue({
      tasks: [
        {
          taskId: 30,
          format: 'IMAGE_VERIFICATION',
          imageTask: {
            taskDescription: 'Finn ut om bildet er ekte.',
            fileId: 4,
            justifications: ['Bakgrunnen ser rar ut'],
          },
          newsTask: null,
        },
      ],
    })

    const wrapper = mount(DatacenterView, {
      global: {
        stubs: globalStubs,
      },
    })

    await flushPromises()

    expect(wrapper.find('.hint-creature-stub').attributes('data-chapter-id')).toBe('3')
  })
})
