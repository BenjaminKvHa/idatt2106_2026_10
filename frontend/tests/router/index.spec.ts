import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => {
  return {
    beforeEachGuard: undefined as
      | undefined
      | ((to: { path: string }) => Promise<unknown> | unknown),
    router: {
      beforeEach: vi.fn((guard) => {
        mocks.beforeEachGuard = guard
      }),
    },
    getJwtDataWithRefresh: vi.fn(),
    fetchMeCached: vi.fn(),
    getPupilMembershipRoute: vi.fn(),
    clearMeCache: vi.fn(),
  }
})

vi.mock('vue-router', () => ({
  createRouter: vi.fn(() => mocks.router),
  createWebHistory: vi.fn(() => ({ history: 'mock-history' })),
}))

vi.mock('@/utils/jwtUtils', () => ({
  getJwtDataWithRefresh: mocks.getJwtDataWithRefresh,
}))

vi.mock('@/utils/authRedirect', () => ({
  clearMeCache: mocks.clearMeCache,
  fetchMeCached: mocks.fetchMeCached,
  getPupilMembershipRoute: mocks.getPupilMembershipRoute,
}))

vi.mock('@/views/shared/HomeView.vue', () => ({ default: { name: 'HomeView' } }))
vi.mock('@/views/shared/NotFoundView.vue', () => ({ default: { name: 'NotFoundView' } }))
vi.mock('@/views/shared/LeaderboardView.vue', () => ({ default: { name: 'LeaderboardView' } }))
vi.mock('@/views/shared/EnterClassroomCodeView.vue', () => ({
  default: { name: 'EnterClassroomCodeView' },
}))
vi.mock('@/views/shared/SetAvatarView.vue', () => ({ default: { name: 'SetAvatarView' } }))
vi.mock('@/views/shared/SignInView.vue', () => ({ default: { name: 'SignInView' } }))
vi.mock('@/views/shared/SignUpView.vue', () => ({ default: { name: 'SignUpView' } }))

vi.mock('@/views/teacher/TeacherView.vue', () => ({ default: { name: 'TeacherView' } }))
vi.mock('@/views/teacher/TeachersProfileView.vue', () => ({
  default: { name: 'TeachersProfileView' },
}))
vi.mock('@/views/teacher/TeachersNotificationsView.vue', () => ({
  default: { name: 'TeachersNotificationsView' },
}))
vi.mock('@/views/teacher/TeachersClassroomsView.vue', () => ({
  default: { name: 'TeachersClassroomsView' },
}))
vi.mock('@/views/teacher/TeachersCreateClassroomView.vue', () => ({
  default: { name: 'TeachersCreateClassroomView' },
}))
vi.mock('@/views/teacher/TeachersClassroomView.vue', () => ({
  default: { name: 'TeachersClassroomView' },
}))
vi.mock('@/views/teacher/TeachersPupilsView.vue', () => ({
  default: { name: 'TeachersPupilsView' },
}))
vi.mock('@/views/teacher/TeachersPupilView.vue', () => ({ default: { name: 'TeachersPupilView' } }))
vi.mock('@/views/teacher/TeachersNotebookChapterView.vue', () => ({
  default: { name: 'TeachersNotebookChapterView' },
}))
vi.mock('@/views/teacher/TeachersLeaderboardView.vue', () => ({
  default: { name: 'TeachersLeaderboardView' },
}))
vi.mock('@/views/teacher/TeachersEditTasksView.vue', () => ({
  default: { name: 'TeachersEditTasksView' },
}))
vi.mock('@/views/teacher/TeachersMysteryView.vue', () => ({
  default: { name: 'TeachersMysteryView' },
}))
vi.mock('@/views/teacher/TeachersEditClassroom.vue', () => ({
  default: { name: 'TeachersEditClassroom' },
}))

vi.mock('@/views/pupil/PupilView.vue', () => ({ default: { name: 'PupilView' } }))
vi.mock('@/views/pupil/PupilsClassroomView.vue', () => ({
  default: { name: 'PupilsClassroomView' },
}))
vi.mock('@/views/pupil/PupilsNotebookView.vue', () => ({ default: { name: 'PupilsNotebookView' } }))
vi.mock('@/views/pupil/PupilsNotebookChapterView.vue', () => ({
  default: { name: 'PupilsNotebookChapterView' },
}))
vi.mock('@/views/pupil/PupilsMysteryView.vue', () => ({ default: { name: 'PupilsMysteryView' } }))
vi.mock('@/views/pupil/PupilsMedalsView.vue', () => ({ default: { name: 'PupilsMedalsView' } }))
vi.mock('@/views/pupil/PupilsHelpView.vue', () => ({ default: { name: 'PupilsHelpView' } }))
vi.mock('@/views/pupil/PupilsMapView.vue', () => ({ default: { name: 'PupilsMapView' } }))
vi.mock('@/views/pupil/PupilsWaitingPageView.vue', () => ({
  default: { name: 'PupilsWaitingPageView' },
}))
vi.mock('@/views/pupil/PupilsWelcomeGuideView.vue', () => ({
  default: { name: 'PupilsWelcomeGuideView' },
}))
vi.mock('@/views/pupil/PupilsProfileView.vue', () => ({ default: { name: 'PupilsProfileView' } }))
vi.mock('@/views/pupil/PupilsEditAvatarView.vue', () => ({
  default: { name: 'PupilsEditAvatarView' },
}))

vi.mock('@/views/pupil/map_locations/NewsView.vue', () => ({ default: { name: 'NewsView' } }))
vi.mock('@/views/pupil/map_locations/DatacenterView.vue', () => ({
  default: { name: 'DatacenterView' },
}))
vi.mock('@/views/pupil/map_locations/PhotographerView.vue', () => ({
  default: { name: 'PhotographerView' },
}))

describe('router', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    vi.resetModules()
    mocks.beforeEachGuard = undefined

    await import('@/router/index')
  })

  async function runGuard(path: string) {
    if (!mocks.beforeEachGuard) {
      throw new Error('beforeEach guard was not registered')
    }

    return await mocks.beforeEachGuard({ path })
  }

  it('registers a beforeEach guard', () => {
    expect(mocks.router.beforeEach).toHaveBeenCalledTimes(1)
    expect(mocks.beforeEachGuard).toBeTypeOf('function')
  })

  it('allows public routes without checking JWT', async () => {
    const result = await runGuard('/sign-in')

    expect(result).toBe(true)
    expect(mocks.getJwtDataWithRefresh).not.toHaveBeenCalled()
  })

  it('redirects protected routes to sign-in when JWT is missing', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce(null)

    await expect(runGuard('/teacher/classrooms')).resolves.toBe('/sign-in')
  })

  it('redirects pupil away from teacher routes', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })

    await expect(runGuard('/teacher/classrooms')).resolves.toBe('/pupil')
  })

  it('redirects teacher away from pupil routes', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'TEACHER',
    })

    await expect(runGuard('/pupil')).resolves.toBe('/teacher/classrooms')
  })

  it('allows teacher on teacher routes', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'TEACHER',
    })

    await expect(runGuard('/teacher/classrooms')).resolves.toBe(true)
  })

  it('allows pupil on pupil route when membership route is pupil', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'APPROVED',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/pupil')

    await expect(runGuard('/pupil')).resolves.toBe(true)

    expect(mocks.fetchMeCached).toHaveBeenCalledWith({
      force: false,
    })
  })

  it('redirects pupil route to membership route when pupil is not ready for /pupil', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'PENDING',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/join-classroom/waiting-classroom')

    await expect(runGuard('/pupil')).resolves.toBe('/join-classroom/waiting-classroom')
  })

  it('redirects pupil route to welcome guide when intro is needed', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'APPROVED',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/pupil/welcome-guide')

    await expect(runGuard('/pupil')).resolves.toBe('/pupil/welcome-guide')
  })

  it('allows the welcome guide route when intro is needed', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'APPROVED',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/pupil/welcome-guide')

    await expect(runGuard('/pupil/welcome-guide')).resolves.toBe(true)
  })

  it('redirects join-classroom route to pupil when pupil already has classroom', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'APPROVED',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/pupil')

    await expect(runGuard('/join-classroom/enter-classroom-code')).resolves.toBe('/pupil')
  })

  it('redirects join-classroom route to welcome guide when confirmed pupil still needs intro', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'APPROVED',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/pupil/welcome-guide')

    await expect(runGuard('/join-classroom/enter-classroom-code')).resolves.toBe(
      '/pupil/welcome-guide',
    )
  })

  it('redirects set-avatar to enter-classroom-code when pupil has no classroom', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'NONE',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/join-classroom/enter-classroom-code')

    await expect(runGuard('/join-classroom/set-avatar')).resolves.toBe(
      '/join-classroom/enter-classroom-code',
    )
  })

  it('redirects join-classroom route to waiting-classroom when pupil is waiting', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'PENDING',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/join-classroom/waiting-classroom')

    await expect(runGuard('/join-classroom/enter-classroom-code')).resolves.toBe(
      '/join-classroom/waiting-classroom',
    )
  })

  it('allows set-avatar while pupil is waiting', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'PENDING',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/join-classroom/waiting-classroom')

    await expect(runGuard('/join-classroom/set-avatar')).resolves.toBe(true)
  })

  it('redirects waiting-classroom to enter-classroom-code when pupil has no classroom', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({
      status: 'NONE',
    })
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/join-classroom/enter-classroom-code')

    await expect(runGuard('/join-classroom/waiting-classroom')).resolves.toBe(
      '/join-classroom/enter-classroom-code',
    )
  })

  it('forces fresh me fetch on waiting-classroom route', async () => {
    mocks.getJwtDataWithRefresh.mockResolvedValueOnce({
      role: 'PUPIL',
    })
    mocks.fetchMeCached.mockResolvedValueOnce({})
    mocks.getPupilMembershipRoute.mockReturnValueOnce('/join-classroom/waiting-classroom')

    await runGuard('/join-classroom/waiting-classroom')

    expect(mocks.fetchMeCached).toHaveBeenCalledWith({
      force: true,
    })
  })
})
