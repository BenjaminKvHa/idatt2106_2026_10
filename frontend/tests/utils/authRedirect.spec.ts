import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  clearMeCache,
  fetchMeCached,
  getPupilMembershipRoute,
  resolvePostAuthRoute,
} from '@/utils/authRedirect'
import { get } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

const mockedGet = vi.mocked(get)

describe('authRedirect', () => {
  beforeEach(() => {
    sessionStorage.clear()
    mockedGet.mockReset()
  })

  it('resolves teachers to the teacher classrooms page without loading me profile', async () => {
    await expect(resolvePostAuthRoute('TEACHER')).resolves.toBe('/teacher/classrooms')

    expect(mockedGet).not.toHaveBeenCalled()
  })

  it('resolves pupils without classroom bridge to the join classroom page', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: null,
      membershipConfirmed: false,
      needsIntroSequence: false,
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe(
      '/join-classroom/enter-classroom-code',
    )
  })

  it('resolves pending pupils to the waiting page', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: false,
      needsIntroSequence: false,
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/join-classroom/waiting-classroom')
  })

  it('resolves confirmed pupils to the pupil dashboard', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
      needsIntroSequence: false,
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/pupil')
  })

  it('resolves confirmed pupils without chapter one progression to the welcome guide', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
      needsIntroSequence: true,
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/pupil/welcome-guide')
  })

  it('resolves pupils to sign-in when the me profile cannot be loaded', async () => {
    mockedGet.mockRejectedValue(new Error('Unauthorized'))

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/sign-in')
  })

  it('uses cached me profile unless a fresh profile is requested', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
      needsIntroSequence: false,
    })

    await fetchMeCached()
    await fetchMeCached()

    expect(mockedGet).toHaveBeenCalledTimes(1)

    await fetchMeCached({ force: true })

    expect(mockedGet).toHaveBeenCalledTimes(2)
  })

  it('maps pupil membership states to their route', () => {
    expect(getPupilMembershipRoute(null)).toBe('/join-classroom/enter-classroom-code')
    expect(
      getPupilMembershipRoute({
        role: 'PUPIL',
        classroomCode: 'MAPAAA',
        membershipConfirmed: false,
        needsIntroSequence: false,
      }),
    ).toBe('/join-classroom/waiting-classroom')
    expect(
      getPupilMembershipRoute({
        role: 'PUPIL',
        classroomCode: 'MAPAAA',
        membershipConfirmed: true,
        needsIntroSequence: true,
      }),
    ).toBe('/pupil/welcome-guide')
    expect(
      getPupilMembershipRoute({
        role: 'PUPIL',
        classroomCode: 'MAPAAA',
        membershipConfirmed: true,
        needsIntroSequence: false,
      }),
    ).toBe('/pupil')
  })

  it('clears the cached me profile', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
      needsIntroSequence: false,
    })

    await fetchMeCached()
    clearMeCache()
    await fetchMeCached()

    expect(mockedGet).toHaveBeenCalledTimes(2)
  })
})
