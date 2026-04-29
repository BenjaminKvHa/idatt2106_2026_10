import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import DatacenterView from '@/views/pupil/map_locations/DatacenterView.vue'
import { get, getBlob, post } from '@/utils/apiUtils'

const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  getBlob: vi.fn(),
  post: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

vi.mock('@/composables/useAudio', () => ({
  useAudio: () => ({
    playSound: vi.fn(),
    stopSound: vi.fn(),
  }),
}))

vi.mock('canvas-confetti', () => ({
  default: vi.fn(),
}))

vi.stubGlobal('URL', {
  createObjectURL: vi.fn(() => 'blob:test-image-url'),
  revokeObjectURL: vi.fn(),
})

vi.stubGlobal('requestAnimationFrame', vi.fn())

const mockedGet = vi.mocked(get)
const mockedGetBlob = vi.mocked(getBlob)
const mockedPost = vi.mocked(post)

const imageTask = {
  taskId: 1,
  format: 'IMAGE_VERIFICATION',
  imageTask: {
    taskDescription: 'Er bildet ekte?',
    fileId: 10,
    justifications: ['Ser ekte ut'],
  },
  newsTask: null,
}

const newsTask = {
  taskId: 2,
  format: 'NEWS_VERIFICATION',
  imageTask: null,
  newsTask: {
    taskDescription: 'Hvilken nyhet er falsk?',
    fileIds: [20, 21],
  },
}

function mountDatacenterView() {
  return mount(DatacenterView, {
    global: {
      stubs: {
        TopBar: {
          props: ['title'],
          template: '<div>{{ title }}</div>',
        },
        HintCreature: {
          template: '<div data-test="hint"></div>',
        },
        TaskGuide: {
          template: '<div data-test="guide"><slot /></div>',
        },
        PupilImageTask: {
          template: `
            <div data-test="image-task">
              <button data-test="category" @click="$emit('select-category', 'REAL')">Ekte</button>
              <button data-test="justification" @click="$emit('select-justification', 'Ser ekte ut')">
                Begrunnelse
              </button>
            </div>
          `,
        },
        PupilNewsTask: {
          template: `
            <div data-test="news-task">
              <button data-test="select-file" @click="$emit('select-file', 20)">
                Velg nyhet
              </button>
            </div>
          `,
        },
        CompleteGamePopup: {
          props: ['show', 'title', 'message', 'success'],
          template: `
            <div v-if="show" data-test="game-popup">
              <p>{{ title }}</p>
              <p>{{ message }}</p>
              <button data-test="map" @click="$emit('map')">Til kartet</button>
              <button data-test="retry" @click="$emit('retry')">Prøv igjen</button>
            </div>
          `,
        },
      },
    },
  })
}

describe('DatacenterView', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    mockedGet.mockResolvedValue({
      tasks: [imageTask, newsTask],
    })

    mockedGetBlob.mockResolvedValue(new Blob(['image']))
  })

  it('loads and renders first level', async () => {
    const wrapper = mountDatacenterView()

    await flushPromises()

    expect(wrapper.text()).toContain('Datasenteret')
    expect(wrapper.text()).toContain('Nivå 1/2')
    expect(wrapper.text()).toContain('Er bildet ekte?')
    expect(mockedGet).toHaveBeenCalledWith('/api/chapters/3/datasenteret/run')
  })

  it('submits image verification answer', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Riktig!',
    })

    const wrapper = mountDatacenterView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    expect(mockedPost).toHaveBeenCalledWith('/api/chapters/datasenteret/tasks/1/validate', {
      format: 'IMAGE_VERIFICATION',
      imageAnswer: {
        selectedCategory: 'REAL',
        selectedJustification: 'Ser ekte ut',
      },
      newsAnswer: null,
    })

    expect(wrapper.text()).toContain('Riktig!')
  })

  it('moves to next level after first answer', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Riktig!',
    })

    const wrapper = mountDatacenterView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    await wrapper.find('.nav-button-primary').trigger('click')

    expect(wrapper.text()).toContain('Nivå 2/2')
    expect(wrapper.text()).toContain('Hvilken nyhet er falsk?')
  })

  it('submits news verification answer', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Riktig!',
    })

    const wrapper = mountDatacenterView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    await wrapper.find('.nav-button-primary').trigger('click')

    await wrapper.find('[data-test="select-file"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    expect(mockedPost).toHaveBeenLastCalledWith('/api/chapters/datasenteret/tasks/2/validate', {
      format: 'NEWS_VERIFICATION',
      imageAnswer: null,
      newsAnswer: {
        selectedFileId: 20,
      },
    })
  })

  it('shows defeat popup when not all answers are correct', async () => {
    mockedPost
    .mockResolvedValueOnce({
      correct: true,
      feedback: 'Riktig!',
    })
    .mockResolvedValueOnce({
      correct: false,
      feedback: 'Feil!',
    })

    const wrapper = mountDatacenterView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    await wrapper.find('.nav-button-primary').trigger('click')

    await wrapper.find('[data-test="select-file"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    await wrapper.find('.nav-button-primary').trigger('click')

    expect(wrapper.find('[data-test="game-popup"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Du tapte!')
  })

  it('goes back to map from result popup', async () => {
    mockedPost
    .mockResolvedValueOnce({
      correct: true,
      feedback: 'Riktig!',
    })
    .mockResolvedValueOnce({
      correct: true,
      feedback: 'Riktig!',
    })

    const wrapper = mountDatacenterView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    await wrapper.find('.nav-button-primary').trigger('click')

    await wrapper.find('[data-test="select-file"]').trigger('click')
    await wrapper.find('.submit-button').trigger('click')
    await flushPromises()

    await wrapper.find('.nav-button-primary').trigger('click')
    await wrapper.find('[data-test="map"]').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map')
  })

  it('shows error if loading run fails', async () => {
    mockedGet.mockRejectedValueOnce(new Error('Kunne ikke laste'))

    const wrapper = mountDatacenterView()

    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke laste')
  })
})
