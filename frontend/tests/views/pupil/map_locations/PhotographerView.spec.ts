import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PhotographerView from '@/views/pupil/map_locations/PhotographerView.vue'
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

function mountPhotographerView() {
  return mount(PhotographerView, {
    global: {
      stubs: {
        TopBar: {
          props: ['title'],
          template: '<div data-test="topbar">{{ title }}</div>',
        },
        HintCreature: {
          template: '<div data-test="hint"><slot /></div>',
        },
        TaskGuide: {
          template: '<div data-test="task-guide"><slot /></div>',
        },
        CompleteLevelPopup: {
          props: ['show', 'title', 'message'],
          template: `
            <div v-if="show" data-test="complete-popup">
              <p>{{ title }}</p>
              <p>{{ message }}</p>
              <button data-test="map" @click="$emit('map')">Til kartet</button>
            </div>
          `,
        },
        PupilImageTask: {
          name: 'PupilImageTask',
          props: [
            'taskDescription',
            'imageUrl',
            'categories',
            'justifications',
            'selectedCategory',
            'selectedJustification',
            'result',
            'submitting',
          ],
          template: `
            <div data-test="image-task">
              <p>{{ taskDescription }}</p>
              <button data-test="category" @click="$emit('select-category', 'REAL')">Ekte</button>
              <button data-test="justification" @click="$emit('select-justification', 'Fordi bildet ser ekte ut')">
                Begrunnelse
              </button>
            </div>
          `,
        },
      },
    },
  })
}

describe('PhotographerView', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    mockedGet.mockResolvedValue({
      taskId: 10,
      fileId: 99,
      taskDescription: 'Er dette bildet ekte?',
      justifications: ['Fordi bildet ser ekte ut'],
    })

    mockedGetBlob.mockResolvedValue(new Blob(['image']))
  })

  it('loads and renders photographer task', async () => {
    const wrapper = mountPhotographerView()

    await flushPromises()

    expect(wrapper.text()).toContain('Fotografen')
    expect(wrapper.text()).toContain('Er dette bildet ekte?')
    expect(mockedGet).toHaveBeenCalledWith('/api/chapters/2/fotografen?difficulty=EASY')
    expect(mockedGetBlob).toHaveBeenCalledWith('/api/objects/99')
  })

  it('disables submit button until category and justification are selected', async () => {
    const wrapper = mountPhotographerView()

    await flushPromises()

    const submitButton = wrapper.find('.bottom-actions button')

    expect(submitButton.attributes('disabled')).toBeDefined()
  })

  it('submits answer after category and justification are selected', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Bra jobbet!',
      runCompleted: false,
    })

    const wrapper = mountPhotographerView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.bottom-actions button').trigger('click')

    await flushPromises()

    expect(mockedPost).toHaveBeenCalledWith('/api/chapters/fotografen/tasks/10/validate', {
      selectedCategory: 'REAL',
      selectedJustification: 'Fordi bildet ser ekte ut',
    })

    expect(wrapper.text()).toContain('Bra jobbet!')
  })

  it('shows error if task loading fails', async () => {
    mockedGet.mockRejectedValueOnce(new Error('Kunne ikke hente oppgave'))

    const wrapper = mountPhotographerView()

    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke hente oppgave')
  })

  it('navigates back to map when run is finished', async () => {
    mockedPost.mockResolvedValue({
      correct: true,
      feedback: 'Ferdig!',
      runCompleted: true,
    })

    const wrapper = mountPhotographerView()

    await flushPromises()

    await wrapper.find('[data-test="category"]').trigger('click')
    await wrapper.find('[data-test="justification"]').trigger('click')
    await wrapper.find('.bottom-actions button').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="complete-popup"]').exists()).toBe(true)

    await wrapper.find('[data-test="map"]').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/pupil/classroom/map')
  })
})
