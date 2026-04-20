import type { BackendAccountRole } from './apiUtils'

type JwtPayload = {
  role?: BackendAccountRole
  email?: string
  legalName?: string
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const [, payload] = token.split('.')
    if (!payload) {
      return null
    }

    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=')
    const decoded = atob(padded)

    return JSON.parse(decoded) as JwtPayload
  } catch {
    return null
  }
}

export function getDefaultRouteForRole(role?: BackendAccountRole, email?: string): string {
  if (role === 'TEACHER') {
    const username = email?.split('@')[0]
    return username ? `/${username}/classrooms` : '/classrooms'
  }

  if (role === 'PUPIL') {
    return '/pupil'
  }

  return '/'
}
