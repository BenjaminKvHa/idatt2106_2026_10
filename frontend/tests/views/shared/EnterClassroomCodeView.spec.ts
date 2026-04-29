import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import EnterClassroomCodeView from '@/views/shared/EnterClassroomCodeView.vue'
import { post } from '@/utils/apiUtils'

const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  post: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

const mockedPost = vi.mocked(post)

function mountView() {
  return mount(EnterClassroomCodeView)
}

describe('EnterClassroomCodeView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedPost.mockResolvedValue({})
  })

  it('renders title and input', () => {
    const wrapper = mountView()

    expect(wrapper.text()).toContain('Bli med i et klasserom')
    expect(wrapper.find('#classroom-code').exists()).toBe(true)
  })

  it('disables confirm button when code is too short', async () => {
    const wrapper = mountView()

    await wrapper.find('#classroom-code').setValue('ABC')

    expect(wrapper.find('.btn-green').attributes('disabled')).toBeDefined()
  })

  it('formats input to uppercase letters only and max 6 characters', async () => {
    const wrapper = mountView()

    await wrapper.find('#classroom-code').setValue('ab12cdEFG')

    const input = wrapper.find('#classroom-code').element as HTMLInputElement

    expect(input.value).toBe('ABCDEF')
  })

  it('enables confirm button when code has 6 letters', async () => {
    const wrapper = mountView()

    await wrapper.find('#classroom-code').setValue('ABCDEF')

    expect(wrapper.find('.btn-green').attributes('disabled')).toBeUndefined()
  })

  it('does not submit when code is shorter than 6 letters', async () => {
    const wrapper = mountView()

    await wrapper.find('#classroom-code').setValue('ABC')
    await wrapper.find('.btn-green').trigger('click')

    expect(mockedPost).not.toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()
  })

  it('joins classroom and navigates to set avatar when code is valid', async () => {
    const wrapper = mountView()

    await wrapper.find('#classroom-code').setValue('ABCDEF')
    await wrapper.find('.btn-green').trigger('click')
    await flushPromises()

    expect(mockedPost).toHaveBeenCalledWith('/api/pupil/classrooms/ABCDEF/join', {})
    expect(routerPush).toHaveBeenCalledWith({
      path: '/join-classroom/set-avatar',
      query: {
        classroomCode: 'ABCDEF',
      },
    })
  })

  it('shows error and does not navigate when join fails', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    mockedPost.mockRejectedValueOnce(new Error('Join failed'))

    const wrapper = mountView()

    await wrapper.find('#classroom-code').setValue('ABCDEF')
    await wrapper.find('.btn-green').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke bli med i klasserommet. Sjekk koden og prøv igjen.',)
    expect(routerPush).not.toHaveBeenCalled()
  })
})
