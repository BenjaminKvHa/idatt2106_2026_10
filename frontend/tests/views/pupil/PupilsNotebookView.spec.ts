import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsNotebookView from '@/views/pupil/PupilsNotebookView.vue'
import { get, put } from '@/utils/apiUtils'

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
  routeLeaveGuard: undefined as
    | ((to: { fullPath: string }) => boolean | Promise<boolean>)
    | undefined,
}))

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  put: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerMocks.push,
  }),
  onBeforeRouteLeave: vi.fn((guard) => {
    routerMocks.routeLeaveGuard = guard
  }),
}))

const mockedGet = vi.mocked(get)
const mockedPut = vi.mocked(put)

function mountNotebookView() {
  return mount(PupilsNotebookView, {
    global: {
      stubs: {
        TopBar: true,
      },
    },
  })
}

async function finishChapterLoad() {
  await vi.advanceTimersByTimeAsync(250)
  await flushPromises()
}

describe('PupilsNotebookView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    routerMocks.push.mockReset()
    routerMocks.routeLeaveGuard = undefined
    vi.spyOn(window.HTMLMediaElement.prototype, 'play').mockImplementation(() => Promise.resolve())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('shows a loading state before notebook data resolves', () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tips: [],
      ownNotes: '',
    })

    const wrapper = mountNotebookView()

    expect(wrapper.text()).toContain('Loading...')
  })

  it('loads and renders the first chapter notebook from the backend endpoint', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tips: ['Look closely', 'Check the sender'],
      ownNotes: 'My first clue',
    })

    const wrapper = mountNotebookView()

    await finishChapterLoad()

    expect(mockedGet).toHaveBeenCalledWith('/api/pupil/chapters/1/notebook')
    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.text()).toContain('Page 1 / 3')
    expect(wrapper.text()).toContain('Look closely')
    expect(wrapper.text()).toContain('Check the sender')
    expect(wrapper.find('textarea').element.value).toBe('My first clue')
  })

  it('keeps the save button disabled until own notes are changed', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tips: [],
      ownNotes: 'Existing note',
    })

    const wrapper = mountNotebookView()

    await finishChapterLoad()

    expect(wrapper.find('.save-button').attributes('disabled')).toBeDefined()

    await wrapper.find('textarea').setValue('Updated note')

    expect(wrapper.find('.save-button').attributes('disabled')).toBeUndefined()
  })

  it('saves changed own notes with the expected notebook payload', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tips: [],
      ownNotes: 'Existing note',
    })
    mockedPut.mockResolvedValue({
      chapterId: 1,
      tips: ['Still read-only'],
      ownNotes: 'Updated note',
    })

    const wrapper = mountNotebookView()

    await finishChapterLoad()
    await wrapper.find('textarea').setValue('Updated note')
    await wrapper.find('.save-button').trigger('click')
    await flushPromises()

    expect(mockedPut).toHaveBeenCalledWith('/api/pupil/chapters/1/notebook', {
      ownNotes: 'Updated note',
    })
    expect(wrapper.find('textarea').element.value).toBe('Updated note')
    expect(wrapper.text()).toContain('Notes saved')
  })

  it('shows the load error when notebook loading fails', async () => {
    mockedGet.mockRejectedValue(new Error('Notebook unavailable'))

    const wrapper = mountNotebookView()

    await finishChapterLoad()

    expect(wrapper.text()).toContain('Notebook unavailable')
    expect(wrapper.find('.content-grid').exists()).toBe(false)
  })

  it('loads the next chapter notebook when moving forward', async () => {
    mockedGet
      .mockResolvedValueOnce({
        chapterId: 1,
        tips: ['First tip'],
        ownNotes: 'First note',
      })
      .mockResolvedValueOnce({
        chapterId: 2,
        tips: ['Second tip'],
        ownNotes: 'Second note',
      })

    const wrapper = mountNotebookView()

    await finishChapterLoad()
    await wrapper
      .findAll('.nav-button')
      .find((button) => button.text() === 'Forward')
      ?.trigger('click')
    await flushPromises()

    expect(mockedGet).toHaveBeenLastCalledWith('/api/pupil/chapters/2/notebook')
    expect(wrapper.text()).toContain('Fotografen')
    expect(wrapper.find('textarea').element.value).toBe('Second note')
  })

  it('stays on the current chapter when unsaved notes are not confirmed for discard', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tips: [],
      ownNotes: 'First note',
    })

    const wrapper = mountNotebookView()

    await finishChapterLoad()
    await wrapper.find('textarea').setValue('Unsaved note')
    await wrapper
      .findAll('.nav-button')
      .find((button) => button.text() === 'Forward')
      ?.trigger('click')
    await flushPromises()

    expect(wrapper.find('.unsaved-notes-popup').exists()).toBe(true)
    expect(mockedGet).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.find('textarea').element.value).toBe('Unsaved note')
  })

  it('moves to the next chapter when unsaved notes are confirmed for discard', async () => {
    mockedGet
      .mockResolvedValueOnce({
        chapterId: 1,
        tips: [],
        ownNotes: 'First note',
      })
      .mockResolvedValueOnce({
        chapterId: 2,
        tips: [],
        ownNotes: 'Second note',
      })

    const wrapper = mountNotebookView()

    await finishChapterLoad()
    await wrapper.find('textarea').setValue('Unsaved note')
    await wrapper
      .findAll('.nav-button')
      .find((button) => button.text() === 'Forward')
      ?.trigger('click')
    await wrapper.find('.confirm-discard-notes').trigger('click')
    await flushPromises()

    expect(mockedGet).toHaveBeenLastCalledWith('/api/pupil/chapters/2/notebook')
    expect(wrapper.text()).toContain('Fotografen')
    expect(wrapper.find('textarea').element.value).toBe('Second note')
  })

  it('shows unsaved notes popup before route navigation from the top bar', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tips: [],
      ownNotes: 'First note',
    })

    const wrapper = mountNotebookView()

    await finishChapterLoad()
    await wrapper.find('textarea').setValue('Unsaved note')

    expect(routerMocks.routeLeaveGuard?.({ fullPath: '/pupil' })).toBe(false)
    await flushPromises()

    expect(wrapper.find('.unsaved-notes-popup').exists()).toBe(true)
    expect(routerMocks.push).not.toHaveBeenCalled()

    await wrapper.find('.confirm-discard-notes').trigger('click')
    await flushPromises()

    expect(routerMocks.push).toHaveBeenCalledWith('/pupil')
  })
})
