const ACCESS_TOKEN_KEY = 'access_token'
export function clearAuthSessionStorage() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}
export function getAccessToken(): string {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}
export function hasAccessToken(): boolean {
  return getAccessToken().length > 0
}
function redirectToSignIn() {
  if (typeof window !== 'undefined') {
    window.location.href = '/sign-in'
  }
}

let refreshPromise: Promise<void> | null = null
type RefreshResponse = {
  accessToken?: string
}

export async function getNewTokens(): Promise<void> {
  if (typeof window === 'undefined') return
  if (refreshPromise) return refreshPromise

  refreshPromise = (async () => {
    console.info('Attempting to refresh tokens')
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })

    if (res.status === 401 || !res.ok) {
      console.info('Refreshing tokens failed. Clearing session storage & redirecting to signin')
      clearAuthSessionStorage()
      redirectToSignIn()
      throw new Error('Unable to refresh session')
    }

    const data = (await res.json()) as RefreshResponse
    if (!data.accessToken) {
      console.info(
        'Refreshing tokens failed because of missing access token. Clearing session storage & redirecting to signin',
      )
      clearAuthSessionStorage()
      redirectToSignIn()
      throw new Error('Refresh response missing access token')
    }

    sessionStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken)
  })().finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

async function parseResponse(res: Response) {
  if (res.status === 204) return null
  const contentType = res.headers.get('content-type') ?? ''
  if (contentType.includes('application/json')) return res.json()
  return res.text()
}

async function requestWithOptions<T = unknown>(
  endpoint: string,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
  body?: unknown,
): Promise<T> {
  if (!hasAccessToken()) {
    await getNewTokens()
  }

  const headers: Record<string, string> = (body !== undefined ? { 'Content-Type': 'application/json' } : {})

  const token = getAccessToken()
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const requestData: RequestInit = {
    method,
    credentials: 'include',
    headers,
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  }
  let res = await fetch(endpoint, requestData)
  if (res.status === 401) {
    console.info(`fetching ${endpoint} failed, attempting to get new tokens & refetching`)
    await getNewTokens()
    const refreshedToken = getAccessToken()
    if (!refreshedToken) {
      console.info(
        `getting new tokens after failure also failed. Clearing session storage & redirecting to signin`,
      )
      clearAuthSessionStorage()
      redirectToSignIn()
      throw new Error('Unauthorized')
    }

    headers.Authorization = `Bearer ${refreshedToken}`
    res = await fetch(endpoint, requestData)
  }

  if (!res.ok) {
    if (res.status === 401) {
      clearAuthSessionStorage()
      redirectToSignIn()
    }

    throw new Error(`Request failed with status ${res.status}`)
  }

  return (await parseResponse(res)) as T
}

export async function get<T = unknown>(endpoint: string): Promise<T> {
  return await requestWithOptions(endpoint, 'GET')
}
export async function post<T = unknown>(endpoint: string, body: unknown): Promise<T> {
  return await requestWithOptions(endpoint, 'POST', body)
}
export async function patch<T = unknown>(endpoint: string, body: unknown): Promise<T> {
  return await requestWithOptions(endpoint, 'PATCH', body)
}
export async function put<T = unknown>(endpoint: string, body: unknown): Promise<T> {
  return await requestWithOptions(endpoint, 'PUT', body)
}
export async function del<T = unknown>(endpoint: string, body?: unknown): Promise<T> {
  return await requestWithOptions(endpoint, 'DELETE', body)
}
