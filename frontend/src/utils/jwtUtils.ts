import { getAccessToken, getNewTokens, type BackendAccountRole } from './apiUtils'

type JwtPayload = {
  role?: BackendAccountRole
  email?: string
  legalName?: string
  exp?: number
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
    return '/join-classroom/enter-classroom-code'
  }

  return '/'
}

export function getJwtData(): JwtPayload | null {
  return decodeJwtPayload(getAccessToken());
}

function isJwtExpired(payload: JwtPayload): boolean {
  return typeof payload.exp === 'number' && payload.exp * 1000 <= Date.now()
}

export async function getJwtDataWithRefresh(): Promise<JwtPayload | null> {
  const currentToken = getAccessToken()
  if (currentToken) {
    const currentPayload = decodeJwtPayload(currentToken)
    if (currentPayload && !isJwtExpired(currentPayload)) return currentPayload
  }

  try {
    await getNewTokens();
    const refreshedPayload = decodeJwtPayload(getAccessToken())
    return refreshedPayload && !isJwtExpired(refreshedPayload) ? refreshedPayload : null
  } catch {
    return null
  }
}
