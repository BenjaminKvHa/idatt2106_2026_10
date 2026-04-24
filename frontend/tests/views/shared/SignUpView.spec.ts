import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import SignUpView from '@/views/shared/SignUpView.vue'
import { signupWithCredentials } from '@/utils/apiUtils'

const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  signupWithCredentials: vi.fn(),
}))

vi.mock('@/utils/jwtUtils', () => ({
  decodeJwtPayload: vi.fn(() => ({ role: 'TEACHER', email: 'teacher@example.com' })),
  getDefaultRouteForRole: vi.fn(() => '/ignored-after-signup-route'),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
  RouterLink: {
    props: ['to'],
    template: '<a :href="to"><slot /></a>',
  },
}))

const mockedSignupWithCredentials = vi.mocked(signupWithCredentials)

function mountSignUpView() {
  return mount(SignUpView, {
    global: {
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="to"><slot /></a>',
        },
      },
    },
  })
}

async function fillValidSignupForm(role = 'Lærer') {
  const wrapper = mountSignUpView()

  await wrapper.find('#reg-legal-name').setValue('Ola Nordmann')
  await wrapper.find('#reg-email').setValue(' ola@example.com ')
  await wrapper.find('#reg-password').setValue('Test1234')
  await wrapper.find('#reg-confirm-password').setValue('Test1234')
  await wrapper.find('#reg-role').setValue(role)

  return wrapper
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

describe('SignUpView', () => {
  beforeEach(() => {
    mockedSignupWithCredentials.mockReset()
    routerPush.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the signup form with Lærer selected by default and a login link', () => {
    const wrapper = mountSignUpView()

    expect(wrapper.text()).toContain('Opprett bruker')
    expect((wrapper.find('#reg-role').element as HTMLSelectElement).value).toBe('Lærer')
    expect(wrapper.find('a[href="/sign-in"]').text()).toBe('Logg inn her')
  })

  it('blocks registration when legal name is missing', async () => {
    const wrapper = mountSignUpView()

    await wrapper.find('#reg-email').setValue('ola@example.com')
    await wrapper.find('#reg-password').setValue('Test1234')
    await wrapper.find('#reg-confirm-password').setValue('Test1234')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('[role="alert"]').text()).toContain('Du må skrive inn fullt navn.')
    expect(mockedSignupWithCredentials).not.toHaveBeenCalled()
  })

  it('blocks registration when email is invalid', async () => {
    const wrapper = mountSignUpView()

    await wrapper.find('#reg-legal-name').setValue('Ola Nordmann')
    await wrapper.find('#reg-email').setValue('not-an-email')
    await wrapper.find('#reg-password').setValue('Test1234')
    await wrapper.find('#reg-confirm-password').setValue('Test1234')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('[role="alert"]').text()).toContain('Du må skrive inn en gyldig e-postadresse.')
    expect(mockedSignupWithCredentials).not.toHaveBeenCalled()
  })

  it('blocks registration when password is too weak', async () => {
    const wrapper = mountSignUpView()

    await wrapper.find('#reg-legal-name').setValue('Ola Nordmann')
    await wrapper.find('#reg-email').setValue('ola@example.com')
    await wrapper.find('#reg-password').setValue('password')
    await wrapper.find('#reg-confirm-password').setValue('password')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('[role="alert"]').text()).toContain('Passordet må inneholde minst én stor bokstav.')
    expect(mockedSignupWithCredentials).not.toHaveBeenCalled()
  })

  it('blocks registration when passwords do not match', async () => {
    const wrapper = mountSignUpView()

    await wrapper.find('#reg-legal-name').setValue('Ola Nordmann')
    await wrapper.find('#reg-email').setValue('ola@example.com')
    await wrapper.find('#reg-password').setValue('Test1234')
    await wrapper.find('#reg-confirm-password').setValue('Test5678')
    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('Passordene er ikke like.')
    expect(mockedSignupWithCredentials).not.toHaveBeenCalled()
  })

  it('submits teacher signup data with the backend TEACHER role value', async () => {
    mockedSignupWithCredentials.mockResolvedValue('token')
    const wrapper = await fillValidSignupForm('Lærer')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedSignupWithCredentials).toHaveBeenCalledWith({
      email: 'ola@example.com',
      password: 'Test1234',
      legalName: 'Ola Nordmann',
      role: 'TEACHER',
    })
  })

  it('submits pupil signup data with the backend PUPIL role value', async () => {
    mockedSignupWithCredentials.mockResolvedValue('token')
    const wrapper = await fillValidSignupForm('Elev')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(mockedSignupWithCredentials).toHaveBeenCalledWith({
      email: 'ola@example.com',
      password: 'Test1234',
      legalName: 'Ola Nordmann',
      role: 'PUPIL',
    })
  })

  it('disables the register button and shows loading text while signup is pending', async () => {
    const signupRequest = deferred<string>()
    mockedSignupWithCredentials.mockReturnValue(signupRequest.promise)
    const wrapper = await fillValidSignupForm()

    await wrapper.find('form').trigger('submit')

    expect(wrapper.find('.signup-btn-primary').attributes('disabled')).toBeDefined()
    expect(wrapper.find('.signup-btn-primary').text()).toBe('Oppretter bruker...')

    signupRequest.resolve('token')
    await flushPromises()

    expect(wrapper.find('.signup-btn-primary').attributes('disabled')).toBeUndefined()
  })

  it('clears the form after a successful signup', async () => {
    mockedSignupWithCredentials.mockResolvedValue('token')
    const wrapper = await fillValidSignupForm('Elev')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect((wrapper.find('#reg-legal-name').element as HTMLInputElement).value).toBe('')
    expect((wrapper.find('#reg-email').element as HTMLInputElement).value).toBe('')
    expect((wrapper.find('#reg-password').element as HTMLInputElement).value).toBe('')
    expect((wrapper.find('#reg-confirm-password').element as HTMLInputElement).value).toBe('')
    expect((wrapper.find('#reg-role').element as HTMLSelectElement).value).toBe('Lærer')
  })

  it('shows the backend-safe signup error when registration fails', async () => {
    mockedSignupWithCredentials.mockRejectedValue(new Error('Email already exists'))
    const wrapper = await fillValidSignupForm()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.find('[role="alert"]').text()).toContain('Email already exists')
    expect((wrapper.find('#reg-email').element as HTMLInputElement).value).toBe('ola@example.com')
  })
})
