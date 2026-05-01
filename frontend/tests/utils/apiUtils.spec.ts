import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearAuthSessionStorage,
  del,
  get,
  getAccessToken,
  getBlob,
  getNewTokens,
  hasAccessToken,
  loginWithCredentials,
  patch,
  patchForm,
  post,
  postForm,
  put,
  setAccessToken,
  signupWithCredentials,
} from '@/utils/apiUtils'

const mockFetch = vi.fn()
const locationReplace = vi.fn()

function createResponse({
                          status = 200,
                          ok = status >= 200 && status < 300,
                          jsonData,
                          textData = '',
                          contentType = 'application/json',
                          blobData = 'blob-data',
                        }: {
  status?: number
  ok?: boolean
  jsonData?: unknown
  textData?: string
  contentType?: string
  blobData?: unknown
}) {
  return {
    status,
    ok,
    headers: {
      get: vi.fn((key: string) => {
        if (key.toLowerCase() === 'content-type') return contentType
        return null
      }),
    },
    json: vi.fn().mockResolvedValue(jsonData),
    text: vi.fn().mockResolvedValue(textData),
    blob: vi.fn().mockResolvedValue(blobData),
  } as unknown as Response
}

describe('apiUtils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()

    vi.stubGlobal('fetch', mockFetch)
    Object.defineProperty(window, 'location', {
      value: {
        replace: locationReplace,
      },
      configurable: true,
    })
  })

  it('sets, gets, checks and clears access token', () => {
    expect(getAccessToken()).toBe('')
    expect(hasAccessToken()).toBe(false)

    setAccessToken('token-123')

    expect(getAccessToken()).toBe('token-123')
    expect(hasAccessToken()).toBe(true)

    clearAuthSessionStorage()

    expect(getAccessToken()).toBe('')
    expect(hasAccessToken()).toBe(false)
  })

  it('refreshes token successfully', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          accessToken: 'fresh-token',
        },
      }),
    )

    await getNewTokens()

    expect(mockFetch).toHaveBeenCalledWith('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
    expect(getAccessToken()).toBe('fresh-token')
  })

  it('clears session and throws when refresh returns 401', async () => {
    setAccessToken('old-token')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 401,
        ok: false,
        jsonData: {},
      }),
    )

    await expect(getNewTokens()).rejects.toThrow('Unable to refresh session')

    expect(getAccessToken()).toBe('')
    expect(locationReplace).toHaveBeenCalledWith('/sign-in')
  })

  it('throws when refresh response is missing access token', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {},
      }),
    )

    await expect(getNewTokens()).rejects.toThrow('Refresh response missing access token')

    expect(locationReplace).toHaveBeenCalledWith('/sign-in')
  })

  it('reuses refresh promise when multiple refresh calls happen at the same time', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          accessToken: 'shared-token',
        },
      }),
    )

    await Promise.all([getNewTokens(), getNewTokens()])

    expect(mockFetch).toHaveBeenCalledTimes(1)
    expect(getAccessToken()).toBe('shared-token')
  })

  it('performs GET request with bearer token', async () => {
    setAccessToken('token-123')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          value: 'ok',
        },
      }),
    )

    const result = await get<{ value: string }>('/api/test')

    expect(result).toEqual({ value: 'ok' })
    expect(mockFetch).toHaveBeenCalledWith('/api/test', {
      method: 'GET',
      credentials: 'include',
      headers: {
        Authorization: 'Bearer token-123',
      },
    })
  })

  it('refreshes token before request when no token exists', async () => {
    mockFetch
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            accessToken: 'fresh-token',
          },
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            data: 'ok',
          },
        }),
      )

    const result = await get('/api/protected')

    expect(result).toEqual({ data: 'ok' })
    expect(mockFetch).toHaveBeenNthCalledWith(1, '/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
    })
    expect(mockFetch).toHaveBeenNthCalledWith(2, '/api/protected', {
      method: 'GET',
      credentials: 'include',
      headers: {
        Authorization: 'Bearer fresh-token',
      },
    })
  })

  it('sends POST request with JSON body', async () => {
    setAccessToken('token-123')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          created: true,
        },
      }),
    )

    const result = await post('/api/items', { name: 'Test' })

    expect(result).toEqual({ created: true })
    expect(mockFetch).toHaveBeenCalledWith('/api/items', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer token-123',
      },
      body: JSON.stringify({ name: 'Test' }),
    })
  })

  it('supports PATCH, PUT and DELETE helpers', async () => {
    setAccessToken('token-123')

    mockFetch
      .mockResolvedValueOnce(createResponse({ jsonData: { patched: true } }))
      .mockResolvedValueOnce(createResponse({ jsonData: { updated: true } }))
      .mockResolvedValueOnce(createResponse({ jsonData: { deleted: true } }))

    await expect(patch('/api/item/1', { name: 'A' })).resolves.toEqual({ patched: true })
    await expect(put('/api/item/1', { name: 'B' })).resolves.toEqual({ updated: true })
    await expect(del('/api/item/1')).resolves.toEqual({ deleted: true })

    expect(mockFetch).toHaveBeenNthCalledWith(
      1,
      '/api/item/1',
      expect.objectContaining({ method: 'PATCH' }),
    )
    expect(mockFetch).toHaveBeenNthCalledWith(
      2,
      '/api/item/1',
      expect.objectContaining({ method: 'PUT' }),
    )
    expect(mockFetch).toHaveBeenNthCalledWith(
      3,
      '/api/item/1',
      expect.objectContaining({ method: 'DELETE' }),
    )
  })

  it('parses 204 response as null', async () => {
    setAccessToken('token-123')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 204,
        ok: true,
        contentType: '',
      }),
    )

    const result = await del('/api/item/1')

    expect(result).toBeNull()
  })

  it('parses text response when response is not JSON', async () => {
    setAccessToken('token-123')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        contentType: 'text/plain',
        textData: 'plain text response',
      }),
    )

    const result = await get('/api/text')

    expect(result).toBe('plain text response')
  })

  it('retries request after 401 by refreshing token', async () => {
    setAccessToken('expired-token')

    mockFetch
      .mockResolvedValueOnce(
        createResponse({
          status: 401,
          ok: false,
          jsonData: {},
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            accessToken: 'new-token',
          },
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            success: true,
          },
        }),
      )

    const result = await get('/api/protected')

    expect(result).toEqual({ success: true })
    expect(getAccessToken()).toBe('new-token')
    expect(mockFetch).toHaveBeenCalledTimes(3)
  })

  it('throws Unauthorized when retry refresh fails to provide token', async () => {
    setAccessToken('expired-token')

    mockFetch
      .mockResolvedValueOnce(
        createResponse({
          status: 401,
          ok: false,
          jsonData: {},
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          status: 401,
          ok: false,
          jsonData: {},
        }),
      )

    await expect(get('/api/protected')).rejects.toThrow('Unable to refresh session')
  })

  it('throws request failed error for non-ok response', async () => {
    setAccessToken('token-123')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 500,
        ok: false,
        jsonData: {},
      }),
    )

    await expect(get('/api/fails')).rejects.toThrow('Request failed with status 500')
  })

  it('clears session and redirects on final 401 response', async () => {
    setAccessToken('token-123')

    mockFetch
      .mockResolvedValueOnce(
        createResponse({
          status: 401,
          ok: false,
          jsonData: {},
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            accessToken: 'new-token',
          },
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          status: 401,
          ok: false,
          jsonData: {},
        }),
      )

    await expect(get('/api/protected')).rejects.toThrow('Request failed with status 401')

    expect(getAccessToken()).toBe('')
    expect(locationReplace).toHaveBeenCalledWith('/sign-in')
  })

  it('gets blob response', async () => {
    setAccessToken('token-123')

    const blobResult = { fake: 'blob' }

    mockFetch.mockResolvedValueOnce(
      createResponse({
        contentType: 'application/octet-stream',
        blobData: blobResult,
      }),
    )

    await expect(getBlob('/api/file')).resolves.toBe(blobResult)
  })

  it('posts form data successfully', async () => {
    setAccessToken('token-123')

    const formData = new FormData()
    formData.append('metadata', 'test')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          id: 1,
        },
      }),
    )

    const result = await postForm('/api/form', formData)

    expect(result).toEqual({ id: 1 })
    expect(mockFetch).toHaveBeenCalledWith('/api/form', {
      method: 'POST',
      credentials: 'include',
      headers: {
        Authorization: 'Bearer token-123',
      },
      body: formData,
    })
  })

  it('patches form data successfully', async () => {
    setAccessToken('token-123')

    const formData = new FormData()
    formData.append('metadata', 'test')

    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          id: 2,
        },
      }),
    )

    await expect(patchForm('/api/form/2', formData)).resolves.toEqual({ id: 2 })

    expect(mockFetch).toHaveBeenCalledWith('/api/form/2', {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        Authorization: 'Bearer token-123',
      },
      body: formData,
    })
  })

  it('retries form request after 401', async () => {
    setAccessToken('expired-token')

    const formData = new FormData()

    mockFetch
      .mockResolvedValueOnce(
        createResponse({
          status: 401,
          ok: false,
          jsonData: {},
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            accessToken: 'fresh-token',
          },
        }),
      )
      .mockResolvedValueOnce(
        createResponse({
          jsonData: {
            ok: true,
          },
        }),
      )

    await expect(postForm('/api/form', formData)).resolves.toEqual({ ok: true })

    expect(mockFetch).toHaveBeenCalledTimes(3)
  })

  it('throws parsed JSON error from form request', async () => {
    setAccessToken('token-123')

    const formData = new FormData()

    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 400,
        ok: false,
        jsonData: {
          error: 'Bad form request',
        },
      }),
    )

    await expect(postForm('/api/form', formData)).rejects.toThrow('Bad form request')
  })

  it('throws first field error from form request', async () => {
    setAccessToken('token-123')

    const formData = new FormData()

    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 400,
        ok: false,
        jsonData: {
          username: 'Username is invalid',
        },
      }),
    )

    await expect(postForm('/api/form', formData)).rejects.toThrow('Username is invalid')
  })

  it('throws text error from form request', async () => {
    setAccessToken('token-123')

    const formData = new FormData()

    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 400,
        ok: false,
        contentType: 'text/plain',
        textData: 'Plain form error',
      }),
    )

    await expect(postForm('/api/form', formData)).rejects.toThrow('Plain form error')
  })

  it('logs in and stores access token', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          accessToken: 'login-token',
        },
      }),
    )

    const token = await loginWithCredentials({
      email: 'test@example.com',
      password: 'password',
    })

    expect(token).toBe('login-token')
    expect(getAccessToken()).toBe('login-token')
    expect(mockFetch).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: 'test@example.com',
        password: 'password',
      }),
    })
  })

  it('throws parsed error when login fails', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 400,
        ok: false,
        jsonData: {
          error: 'Invalid login',
        },
      }),
    )

    await expect(
      loginWithCredentials({
        email: 'test@example.com',
        password: 'wrong',
      }),
    ).rejects.toThrow('Invalid login')
  })

  it('throws when login response is missing access token', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {},
      }),
    )

    await expect(
      loginWithCredentials({
        email: 'test@example.com',
        password: 'password',
      }),
    ).rejects.toThrow('Auth response missing access token')
  })

  it('signs up and stores access token', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        jsonData: {
          accessToken: 'signup-token',
        },
      }),
    )

    const token = await signupWithCredentials({
      email: 'new@example.com',
      password: 'password',
      legalName: 'New User',
      role: 'PUPIL',
    })

    expect(token).toBe('signup-token')
    expect(getAccessToken()).toBe('signup-token')
    expect(mockFetch).toHaveBeenCalledWith('/api/auth/signup', {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: 'new@example.com',
        password: 'password',
        legalName: 'New User',
        role: 'PUPIL',
      }),
    })
  })

  it('throws parsed error when signup fails', async () => {
    mockFetch.mockResolvedValueOnce(
      createResponse({
        status: 400,
        ok: false,
        jsonData: {
          email: 'Email already exists',
        },
      }),
    )

    await expect(
      signupWithCredentials({
        email: 'new@example.com',
        password: 'password',
        legalName: 'New User',
        role: 'TEACHER',
      }),
    ).rejects.toThrow('Email already exists')
  })
})
