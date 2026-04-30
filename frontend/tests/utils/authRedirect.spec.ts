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
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/join-classroom/enter-classroom-code')
  })

  it('resolves pending pupils to the waiting page', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: false,
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/join-classroom/waiting-classroom')
  })

  it('resolves confirmed pupils to the pupil dashboard', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
    })

    await expect(resolvePostAuthRoute('PUPIL')).resolves.toBe('/pupil')
  })

  it('uses cached me profile unless a fresh profile is requested', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
    })

    await fetchMeCached()
    await fetchMeCached()

    expect(mockedGet).toHaveBeenCalledTimes(1)

    await fetchMeCached({ force: true })

    expect(mockedGet).toHaveBeenCalledTimes(2)
  })

  it('maps pupil membership states to their route', () => {
    expect(getPupilMembershipRoute(null)).toBe('/join-classroom/enter-classroom-code')
    expect(getPupilMembershipRoute({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: false,
    })).toBe('/join-classroom/waiting-classroom')
    expect(getPupilMembershipRoute({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
    })).toBe('/pupil')
  })

  it('clears the cached me profile', async () => {
    mockedGet.mockResolvedValue({
      role: 'PUPIL',
      classroomCode: 'MAPAAA',
      membershipConfirmed: true,
    })

    await fetchMeCached()
    clearMeCache()
    await fetchMeCached()

    expect(mockedGet).toHaveBeenCalledTimes(2)
  })
})
