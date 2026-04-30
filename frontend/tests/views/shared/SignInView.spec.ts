import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import SignInView from '@/views/shared/SignInView.vue'
import { loginWithCredentials } from '@/utils/apiUtils'
import { resolvePostAuthRoute } from '@/utils/authRedirect'

const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  loginWithCredentials: vi.fn(),
}))

vi.mock('@/utils/jwtUtils', () => ({
  decodeJwtPayload: vi.fn(() => ({ role: 'PUPIL', email: 'pupil@example.com' })),
}))

vi.mock('@/utils/authRedirect', () => ({
  resolvePostAuthRoute: vi.fn(() => Promise.resolve('/resolved-after-login-route')),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

const mockedLoginWithCredentials = vi.mocked(loginWithCredentials)
const mockedResolvePostAuthRoute = vi.mocked(resolvePostAuthRoute)

function mountSignInView() {
  return mount(SignInView)
}

async function goToEmailStep(wrapper: ReturnType<typeof mountSignInView>) {
  await wrapper.find('.primary-option').trigger('click')
}

async function goToPasswordStep(wrapper: ReturnType<typeof mountSignInView>, email = 'pupil@example.com') {
  await goToEmailStep(wrapper)
  await wrapper.find('input[type="email"]').setValue(email)
  await wrapper.find('form').trigger('submit')
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

describe('SignInView', () => {
  beforeEach(() => {
    mockedLoginWithCredentials.mockReset()
    mockedResolvePostAuthRoute.mockReset()
    mockedResolvePostAuthRoute.mockResolvedValue('/resolved-after-login-route')
    routerPush.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('starts on the Feide choice step and moves to the email step when the work or school account button is clicked', async () => {
    const wrapper = mountSignInView()

    expect(wrapper.text()).toContain('Logg inn med Feide')
    expect(wrapper.text()).toContain('Bruk arbeids- eller skolekonto')

    await goToEmailStep(wrapper)

    expect(wrapper.text()).toContain('Logg på')
    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
  })

  it('moves from the email step to the password step and shows the selected email', async () => {
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper, ' pupil@example.com ')

    expect(wrapper.text()).toContain('pupil@example.com')
    expect(wrapper.text()).toContain('Skriv inn passord')
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
  })

  it('goes back to the Feide choice step from the email step', async () => {
    const wrapper = mountSignInView()

    await goToEmailStep(wrapper)
    await wrapper.find('.feide-btn-secondary').trigger('click')

    expect(wrapper.text()).toContain('Logg inn med Feide')
    expect(wrapper.find('input[type="email"]').exists()).toBe(false)
  })

  it('clears the password and returns to the email step when changing account', async () => {
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper)
    await wrapper.find('input[type="password"]').setValue('Secret123')
    await wrapper.find('.clickable').trigger('click')

    expect(wrapper.find('input[type="email"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(false)

    await wrapper.find('form').trigger('submit')

    expect((wrapper.find('input[type="password"]').element as HTMLInputElement).value).toBe('')
  })

  it('opens the signup page when the signup button is clicked', async () => {
    const wrapper = mountSignInView()

    await goToEmailStep(wrapper)
    await wrapper.find('.signup-btn').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/sign-up')
  })

  it('submits trimmed email and password to the login API', async () => {
    mockedLoginWithCredentials.mockResolvedValue('token')
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper, ' pupil@example.com ')
    await wrapper.find('input[type="password"]').setValue('Secret123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedLoginWithCredentials).toHaveBeenCalledWith({
      email: 'pupil@example.com',
      password: 'Secret123',
    })
    expect(wrapper.text()).toContain('Innlogging registrert.')
  })

  it('disables the login button and shows loading text while login is pending', async () => {
    const loginRequest = deferred<string>()
    mockedLoginWithCredentials.mockReturnValue(loginRequest.promise)
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper)
    await wrapper.find('input[type="password"]').setValue('Secret123')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('.login-button-row .feide-btn-primary').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.login-button-row .feide-btn-primary').text()).toBe('Logger inn...')

    loginRequest.resolve('token')
    await flushPromises()

    expect(wrapper.find('.login-button-row .feide-btn-primary').attributes('disabled')).toBeUndefined()
  })

  it('shows the backend-safe login error and does not mark login as registered when login fails', async () => {
    mockedLoginWithCredentials.mockRejectedValue(new Error('Invalid credentials'))
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper)
    await wrapper.find('input[type="password"]').setValue('Wrong123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Invalid credentials')
    expect(wrapper.text()).not.toContain('Innlogging registrert.')
  })

  it('does not call login API before submitting password form', async () => {
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper)
    await wrapper.find('input[type="password"]').setValue('Secret123')

    expect(mockedLoginWithCredentials).not.toHaveBeenCalled()
  })

  it('keeps the email visible when changing account back and forth', async () => {
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper, 'pupil@example.com')
    await wrapper.find('.clickable').trigger('click')

    expect((wrapper.find('input[type="email"]').element as HTMLInputElement).value).toBe(
      'pupil@example.com',
    )
  })

  it('clears previous error when moving from email step to password step', async () => {
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper)
    await wrapper.find('input[type="password"]').setValue('Wrong123')

    mockedLoginWithCredentials.mockRejectedValueOnce(new Error('Invalid credentials'))

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)

    await wrapper.find('.clickable').trigger('click')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })

  it('navigates to resolved post-auth route after successful login', async () => {
    mockedLoginWithCredentials.mockResolvedValue('token')
    const wrapper = mountSignInView()

    await goToPasswordStep(wrapper)
    await wrapper.find('input[type="password"]').setValue('Secret123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedResolvePostAuthRoute).toHaveBeenCalledWith('PUPIL')
    expect(routerPush).toHaveBeenCalledWith('/resolved-after-login-route')
  })

  it('shows disabled ID-porten button', () => {
    const wrapper = mountSignInView()

    const disabledButton = wrapper.find('.disabled-option')

    expect(disabledButton.exists()).toBe(true)
    expect(disabledButton.attributes('disabled')).toBeDefined()
    expect(disabledButton.text()).toContain('Logg inn med ID-porten')
  })
})
