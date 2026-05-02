import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsWaitingPageView from '@/views/pupil/PupilsWaitingPageView.vue'
import { clearMeCache, fetchMeCached } from '@/utils/authRedirect'
import { del } from '@/utils/apiUtils'

const routerPush = vi.fn()

vi.mock('@/utils/authRedirect', () => ({
  clearMeCache: vi.fn(),
  fetchMeCached: vi.fn(),
}))

vi.mock('@/utils/apiUtils', () => ({
  del: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

const mockedClearMeCache = vi.mocked(clearMeCache)
const mockedFetchMeCached = vi.mocked(fetchMeCached)
const mockedDel = vi.mocked(del)

function mountView() {
  return mount(PupilsWaitingPageView)
}

describe('PupilsWaitingPageView', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    mockedDel.mockResolvedValue(undefined)
    mockedFetchMeCached.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: false,
      needsIntroSequence: false,
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

  it('shows the classroom code the pupil is waiting for', async () => {
    const wrapper = mountView()

    await flushPromises()

    expect(wrapper.text()).toContain('Klasseromskode:')
    expect(wrapper.text()).toContain('MAPAAA')
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
      needsIntroSequence: false,
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
      needsIntroSequence: false,
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

  it('deletes the pending classroom request before returning to join classroom', async () => {
    const wrapper = mountView()

    await flushPromises()
    await wrapper.find('.cancel-button').trigger('click')
    await flushPromises()

    expect(mockedDel).toHaveBeenCalledWith('/api/classrooms/MAPAAA/pupils/me')
    expect(mockedClearMeCache).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith('/join-classroom/enter-classroom-code')
  })

  it('shows an error and stays on the waiting page when cancelling fails', async () => {
    mockedDel.mockRejectedValueOnce(new Error('Failed'))
    const wrapper = mountView()

    await flushPromises()
    await wrapper.find('.cancel-button').trigger('click')
    await flushPromises()

    expect(routerPush).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Kunne ikke kansellere forespørselen. Prøv igjen.')
  })
})
