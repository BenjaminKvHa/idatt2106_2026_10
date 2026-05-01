import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  getAccessToken: vi.fn(),
  getNewTokens: vi.fn(),
}))

vi.mock('@/utils/apiUtils', () => ({
  getAccessToken: mocks.getAccessToken,
  getNewTokens: mocks.getNewTokens,
}))

function createJwtPayload(payload: Record<string, unknown>): string {
  const encodedPayload = btoa(JSON.stringify(payload))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '')

  return `header.${encodedPayload}.signature`
}

describe('jwtUtils', () => {
  beforeEach(() => {
    mocks.getAccessToken.mockReset()
    mocks.getNewTokens.mockReset()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-05-01T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('uses the current token when it is not expired', async () => {
    const token = createJwtPayload({
      sub: 'teacher@example.com',
      role: 'TEACHER',
      exp: Math.floor(Date.now() / 1000) + 60,
    })
    mocks.getAccessToken.mockReturnValue(token)

    const { getJwtDataWithRefresh } = await import('@/utils/jwtUtils')

    await expect(getJwtDataWithRefresh()).resolves.toMatchObject({
      email: 'teacher@example.com',
      role: 'TEACHER',
    })
    expect(mocks.getNewTokens).not.toHaveBeenCalled()
  })

  it('refreshes when the current token is expired', async () => {
    const expiredToken = createJwtPayload({
      sub: 'teacher@example.com',
      role: 'TEACHER',
      exp: Math.floor(Date.now() / 1000) - 60,
    })
    const refreshedToken = createJwtPayload({
      sub: 'teacher@example.com',
      role: 'TEACHER',
      exp: Math.floor(Date.now() / 1000) + 60,
    })
    mocks.getAccessToken.mockReturnValueOnce(expiredToken).mockReturnValueOnce(refreshedToken)
    mocks.getNewTokens.mockResolvedValue(undefined)

    const { getJwtDataWithRefresh } = await import('@/utils/jwtUtils')

    await expect(getJwtDataWithRefresh()).resolves.toMatchObject({
      email: 'teacher@example.com',
      role: 'TEACHER',
    })
    expect(mocks.getNewTokens).toHaveBeenCalledOnce()
  })

  it('returns null when refresh fails', async () => {
    const expiredToken = createJwtPayload({
      sub: 'teacher@example.com',
      role: 'TEACHER',
      exp: Math.floor(Date.now() / 1000) - 60,
    })
    mocks.getAccessToken.mockReturnValue(expiredToken)
    mocks.getNewTokens.mockRejectedValue(new Error('Unable to refresh session'))

    const { getJwtDataWithRefresh } = await import('@/utils/jwtUtils')

    await expect(getJwtDataWithRefresh()).resolves.toBeNull()
  })
})
