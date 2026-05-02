import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import NewsView from '@/views/pupil/map_locations/NewsView.vue'
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

vi.stubGlobal('URL', {
  createObjectURL: vi.fn(() => 'blob:test-image-url'),
  revokeObjectURL: vi.fn(),
})

const mockedGet = vi.mocked(get)
const mockedGetBlob = vi.mocked(getBlob)
const mockedPost = vi.mocked(post)

function mountNewsView() {
  return mount(NewsView, {
    global: {
      stubs: {
        TopBar: {
          props: ['title'],
          template: '<div data-test="topbar">{{ title }}</div>',
        },
        HintCreature: {
          template: '<div data-test="hint"></div>',
        },
        TaskGuide: {
          template: '<div data-test="task-guide"><slot /></div>',
        },
        CompleteLevelPopup: {
          props: [
            'show',
            'title',
            'message',
            'success',
            'completedChapterMedal',
            'completedChapterMedalNewlyAwarded',
          ],
          template: `
            <div v-if="show" data-test="complete-popup">
              <p>{{ title }}</p>
              <p>{{ message }}</p>
              <p v-if="completedChapterMedal">{{ completedChapterMedal.name }}</p>
              <p data-test="medal-new">{{ String(completedChapterMedalNewlyAwarded) }}</p>
              <button data-test="retry" @click="$emit('retry')">Prøv på nytt</button>
              <button data-test="map" @click="$emit('map')">Tilbake til kartet</button>
              <button data-test="close" @click="$emit('close')">Lukk</button>
            </div>
          `,
        },
        PupilNewsTask: {
          props: [
            'taskDescription',
            'fileIds',
            'imageUrls',
            'selectedFileId',
            'result',
            'submitting',
            'selectButtonText',
          ],
          template: `
            <div data-test="news-task">
              <p>{{ taskDescription }}</p>
              <button data-test="select-file" @click="$emit('select-file', 101)">
                Velg dette bildet
              </button>
            </div>
          `,
        },
      },
    },
  })
}

describe('NewsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    mockedGet.mockResolvedValue({
      taskId: 7,
      taskDescription: 'Hvilken avisforside er falsk?',
      fileIds: [101, 102],
    })

    mockedGetBlob.mockResolvedValue(new Blob(['image']))
  })

  it('loads and renders news task', async () => {
    const wrapper = mountNewsView()

    await flushPromises()

    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.text()).toContain('Hvilken avisforside er falsk?')
    expect(mockedGet).toHaveBeenCalledWith('/api/chapters/1/nyhetskvartalet?difficulty=EASY')
    expect(mockedGetBlob).toHaveBeenCalledWith('/api/objects/101')
    expect(mockedGetBlob).toHaveBeenCalledWith('/api/objects/102')
  })

  it('submits selected file when image is chosen', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Riktig valgt!',
    })

    const wrapper = mountNewsView()

    await flushPromises()
    await wrapper.find('[data-test="select-file"]').trigger('click')
    await flushPromises()

    expect(mockedPost).toHaveBeenCalledWith('/api/chapters/nyhetskvartalet/tasks/7/validate', {
      selectedFileId: 101,
    })

    expect(wrapper.text()).toContain('Riktig valgt!')
  })

  it('shows error if answer submit fails', async () => {
    mockedPost.mockRejectedValueOnce(new Error('Kunne ikke sende svar'))

    const wrapper = mountNewsView()

    await flushPromises()
    await wrapper.find('[data-test="select-file"]').trigger('click')
    await flushPromises()

    expect(mockedPost).toHaveBeenCalled()
  })

  it('shows next button after answer is submitted', async () => {
    mockedPost.mockResolvedValue({
      correct: false,
      feedback: 'Feil.',
    })

    const wrapper = mountNewsView()

    await flushPromises()
    await wrapper.find('[data-test="select-file"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('.next-level-button').exists()).toBe(true)
    expect(wrapper.find('.next-level-button').text()).toBe('Gå til neste nivå')
  })

  it('passes newly awarded medal reward to the completion popup', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Riktig!',
      runCompleted: true,
      completedChapterMedalNewlyAwarded: true,
      completedChapterMedal: {
        medalKey: 'chapter-1',
        name: 'Nyhetskvartalet',
        description: 'Fullfør Nyhetskvartalet.',
        unlocked: true,
        imageFileObjectId: 101,
      },
    })

    const wrapper = mountNewsView()

    await flushPromises()

    for (let i = 0; i < 3; i += 1) {
      await wrapper.find('[data-test="select-file"]').trigger('click')
      await flushPromises()
      await wrapper.find('.next-level-button').trigger('click')
      await flushPromises()
    }

    expect(wrapper.find('[data-test="complete-popup"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.find('[data-test="medal-new"]').text()).toBe('true')
  })
})
