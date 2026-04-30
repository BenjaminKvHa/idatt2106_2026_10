import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import BlobbyQuestion from '@/components/pupil/task/BlobbyQuestion.vue'
import { get } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

const mockedGet = vi.mocked(get)

type ChapterTipResponse = {
  chapterId: number
  tip: string
  newlyAdded: boolean
  allTipsReceived: boolean
}

function mountBlobbyQuestion(chapterId = 1) {
  return mount(BlobbyQuestion, {
    props: { chapterId },
    slots: {
      default: '<div class="task-content">Chapter task content</div>',
    },
    global: {
      stubs: {
        Teleport: true,
      },
    },
  })
}

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

describe('BlobbyQuestion', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  it('fetches a chapter tip when Blobby is clicked', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tip: 'Se nøye på kilden.',
      newlyAdded: false,
      allTipsReceived: false,
    })
    const wrapper = mountBlobbyQuestion(1)

    await wrapper.find('.hint-creature-button').trigger('click')
    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/pupil/chapters/1/tip')
    expect(wrapper.text()).toContain('Se nøye på kilden.')
  })

  it('shows loading state while the tip request is pending', async () => {
    const deferred = createDeferred<ChapterTipResponse>()
    mockedGet.mockReturnValue(deferred.promise)
    const wrapper = mountBlobbyQuestion(1)

    await wrapper.find('.hint-creature-button').trigger('click')

    expect(wrapper.text()).toContain('Henter tips...')
    expect(wrapper.find('.hint-creature-button').attributes('disabled')).toBeDefined()

    deferred.resolve({
      chapterId: 1,
      tip: 'Et tips',
      newlyAdded: false,
      allTipsReceived: false,
    })
    await flushPromises()
  })

  it('shows that a newly received tip was saved in the notebook', async () => {
    mockedGet.mockResolvedValue({
      chapterId: 2,
      tip: 'Sjekk rare detaljer i bildet.',
      newlyAdded: true,
      allTipsReceived: false,
    })
    const wrapper = mountBlobbyQuestion(2)

    await wrapper.find('.hint-creature-button').trigger('click')
    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/pupil/chapters/2/tip')
    expect(wrapper.text()).toContain('Sjekk rare detaljer i bildet.')
    expect(wrapper.text()).toContain('Tipset er lagret i notatboken din.')
  })

  it('shows an error message when tip loading fails', async () => {
    mockedGet.mockRejectedValue(new Error('Unavailable'))
    const wrapper = mountBlobbyQuestion(3)

    await wrapper.find('.hint-creature-button').trigger('click')
    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/pupil/chapters/3/tip')
    expect(wrapper.text()).toContain('Kunne ikke hente tips. Prøv igjen.')
  })

  it('renders wrapped task content while keeping the Blobby button available', () => {
    mockedGet.mockResolvedValue({
      chapterId: 1,
      tip: 'Et tips',
      newlyAdded: false,
      allTipsReceived: false,
    })

    const wrapper = mountBlobbyQuestion(1)

    expect(wrapper.find('.task-content').text()).toBe('Chapter task content')
    expect(wrapper.find('.hint-creature-button').exists()).toBe(true)
  })
})
