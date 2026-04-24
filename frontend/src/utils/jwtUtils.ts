import { getAccessToken, getNewTokens, type BackendAccountRole } from './apiUtils'

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

    const parsedJwt = JSON.parse(decoded);
    const jwtData: JwtPayload = { ...parsedJwt }
    jwtData.email = jwtData.email ? jwtData.email : parsedJwt["sub"];
    return jwtData;
  } catch {
    return null
  }
}

export function getDefaultRouteForRole(role?: BackendAccountRole, email?: string): string {
  if (role === 'TEACHER') {
    return '/teacher/classrooms'
  }

  if (role === 'PUPIL') {
    return '/pupil'
  }

  return '/'
}

export function getJwtData(): JwtPayload | null {
  return decodeJwtPayload(getAccessToken());
}

export async function getJwtDataWithRefresh(): Promise<JwtPayload | null> {
  if (getAccessToken()) return decodeJwtPayload(getAccessToken());
  else {
    await getNewTokens();
    return decodeJwtPayload(getAccessToken())
  }
}