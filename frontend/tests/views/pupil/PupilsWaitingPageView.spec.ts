import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsWaitingPageView from '@/views/pupil/PupilsWaitingPageView.vue'
import { clearMeCache, fetchMeCached } from '@/utils/authRedirect'

const routerPush = vi.fn()

vi.mock('@/utils/authRedirect', () => ({
  clearMeCache: vi.fn(),
  fetchMeCached: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

const mockedClearMeCache = vi.mocked(clearMeCache)
const mockedFetchMeCached = vi.mocked(fetchMeCached)

function mountView() {
  return mount(PupilsWaitingPageView)
}

describe('PupilsWaitingPageView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    mockedFetchMeCached.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: false,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('polls fresh me profile when mounted', async () => {
    mountView()

    await flushPromises()

    expect(mockedFetchMeCached).toHaveBeenCalledWith({ force: true })
  })

  it('keeps pending pupils on the waiting page', async () => {
    mountView()

    await flushPromises()

    expect(routerPush).not.toHaveBeenCalled()
    expect(mockedClearMeCache).not.toHaveBeenCalled()
  })

  it('redirects accepted pupils to the pupil dashboard', async () => {
    mockedFetchMeCached.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
    })

    mountView()

    await flushPromises()

    expect(mockedClearMeCache).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith('/pupil')
  })

  it('redirects declined pupils without classroom bridge back to join classroom', async () => {
    mockedFetchMeCached.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: null,
      membershipConfirmed: false,
    })

    mountView()

    await flushPromises()

    expect(mockedClearMeCache).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith('/join-classroom/enter-classroom-code')
  })

  it('checks status again on the polling interval', async () => {
    mountView()

    await flushPromises()
    await vi.advanceTimersByTimeAsync(3000)
    await flushPromises()

    expect(mockedFetchMeCached).toHaveBeenCalledTimes(2)
  })

  it('clears cache and returns to join classroom when cancelling the request', async () => {
    const wrapper = mountView()

    await wrapper.find('.cancel-button').trigger('click')
    await flushPromises()

    expect(mockedClearMeCache).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith('/join-classroom/enter-classroom-code')
  })
})
