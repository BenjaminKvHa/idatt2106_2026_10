const ACCESS_TOKEN_KEY = 'access_token'

export type BackendAccountRole = 'TEACHER' | 'PUPIL'

type AuthResponse = {
  accessToken?: string
}

export function clearAuthSessionStorage() {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function setAccessToken(token: string) {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function getAccessToken(): string {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY) ?? ''
}
export function hasAccessToken(): boolean {
  return getAccessToken().length > 0
}
function redirectToSignIn() {
  if (typeof window !== 'undefined') {
    // window.location.href = '/sign-in'
    alert("/sign-in redirectToSignIn")
  }
}

let refreshPromise: Promise<void> | null = null

type LoginRequest = {
  email: string
  password: string
}

type SignupRequest = {
  email: string
  password: string
  legalName: string
  role: BackendAccountRole
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

    const data = (await res.json()) as AuthResponse
    if (!data.accessToken) {
      console.info(
        'Refreshing tokens failed because of missing access token. Clearing session storage & redirecting to signin',
      )
      clearAuthSessionStorage()
      redirectToSignIn()
      throw new Error('Refresh response missing access token')
    }

    setAccessToken(data.accessToken)
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

async function rawRequest(
  endpoint: string,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
  body?: unknown,
): Promise<Response> {
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

  return res
}

async function requestWithOptions<T = unknown>(
  endpoint: string,
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
  body?: unknown,
): Promise<T> {
  const res = await rawRequest(endpoint, method, body)
  return (await parseResponse(res)) as T
}

export async function getBlob(endpoint: string): Promise<Blob> {
  const res = await rawRequest(endpoint, 'GET')
  return await res.blob()
}

async function requestForm<T = unknown>(
  endpoint: string,
  method: 'POST' | 'PATCH',
  formData: FormData,
): Promise<T> {
  if (!hasAccessToken()) {
    await getNewTokens()
  }

  const buildInit = (): RequestInit => {
    const headers: Record<string, string> = {}
    const token = getAccessToken()
    if (token) headers.Authorization = `Bearer ${token}`
    return { method, credentials: 'include', headers, body: formData }
  }

  let res = await fetch(endpoint, buildInit())
  if (res.status === 401) {
    await getNewTokens()
    if (!getAccessToken()) {
      clearAuthSessionStorage()
      throw new Error('Unauthorized')
    }
    res = await fetch(endpoint, buildInit())
  }

  if (!res.ok) {
    if (res.status === 401) {
      clearAuthSessionStorage()
    }
    throw new Error(await parseErrorMessage(res))
  }

  return (await parseResponse(res)) as T
}

export async function postForm<T = unknown>(endpoint: string, formData: FormData): Promise<T> {
  return await requestForm(endpoint, 'POST', formData)
}

export async function patchForm<T = unknown>(endpoint: string, formData: FormData): Promise<T> {
  return await requestForm(endpoint, 'PATCH', formData)
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

async function extractAuthResponse(res: Response): Promise<string> {
  const data = (await parseResponse(res)) as AuthResponse
  if (!data?.accessToken) {
    throw new Error('Auth response missing access token')
  }

  setAccessToken(data.accessToken)
  return data.accessToken
}

async function parseErrorMessage(res: Response): Promise<string> {
  const data = await parseResponse(res)

  if (typeof data === 'string' && data.trim().length > 0) {
    return data
  }

  if (data && typeof data === 'object') {
    const mapped = data as Record<string, unknown>

    if (typeof mapped.error === 'string' && mapped.error.trim().length > 0) {
      return mapped.error
    }

    const firstFieldError = Object.values(mapped).find(
      (value): value is string => typeof value === 'string' && value.trim().length > 0,
    )

    if (firstFieldError) {
      return firstFieldError
    }
  }

  return `Request failed with status ${res.status}`
}

export async function loginWithCredentials(request: LoginRequest): Promise<string> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!res.ok) {
    throw new Error(await parseErrorMessage(res))
  }

  return await extractAuthResponse(res)
}

export async function signupWithCredentials(request: SignupRequest): Promise<string> {
  const res = await fetch('/api/auth/signup', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!res.ok) {
    throw new Error(await parseErrorMessage(res))
  }

  return await extractAuthResponse(res)
}
