import { get, type BackendAccountRole } from '@/utils/apiUtils'

export type MeProfile = {
  role: BackendAccountRole
  classroomCode: string | null
  membershipConfirmed: boolean
  needsIntroSequence: boolean
}

const ME_CACHE_KEY = 'me-profile-cache'

export function clearMeCache() {
  sessionStorage.removeItem(ME_CACHE_KEY)
}

export function cacheMeProfile(profile: MeProfile) {
  sessionStorage.setItem(ME_CACHE_KEY, JSON.stringify(profile))
}

export async function fetchMeCached(options: { force?: boolean } = {}): Promise<MeProfile | null> {
  if (!options.force) {
    const cached = sessionStorage.getItem(ME_CACHE_KEY)
    if (cached) {
      try {
        return JSON.parse(cached) as MeProfile
      } catch {
        clearMeCache()
      }
    }
  }

  try {
    const me = await get<MeProfile>('/api/me')
    cacheMeProfile(me)
    return me
  } catch {
    clearMeCache()
    return null
  }
}

export function getPupilMembershipRoute(profile: MeProfile | null): string {
  if (!profile?.classroomCode) {
    return '/join-classroom/enter-classroom-code'
  }

  if (!profile.membershipConfirmed) {
    return '/join-classroom/waiting-classroom'
  }

  if (profile.needsIntroSequence) {
    return '/pupil/welcome-guide'
  }

  return '/pupil'
}

export async function resolvePostAuthRoute(role?: BackendAccountRole): Promise<string> {
  clearMeCache()

  if (role === 'TEACHER') {
    return '/teacher/classrooms'
  }

  if (role === 'PUPIL') {
    return getPupilMembershipRoute(await fetchMeCached({ force: true }))
  }

  return '/'
}
