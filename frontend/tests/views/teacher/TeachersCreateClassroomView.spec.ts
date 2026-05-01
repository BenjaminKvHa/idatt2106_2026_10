import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import TeachersCreateClassroomView from '@/views/teacher/TeachersCreateClassroomView.vue'
import { post } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  post: vi.fn(),
}))

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

describe('TeachersCreateClassroomView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const mountComponent = () => {
    return mount(TeachersCreateClassroomView)
  }

  it('renders form fields and submit button', () => {
    const wrapper = mountComponent()

    expect(wrapper.text()).toContain('Opprett nytt klasserom')
    expect(wrapper.find('#classroom-name').exists()).toBe(true)
    expect(wrapper.find('#classroom-description').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').text()).toBe('Opprett klasserom')
  })

  it('shows error when classroom name is missing', async () => {
    const wrapper = mountComponent()

    await wrapper.find('#classroom-description').setValue('Dette er en beskrivelse')
    await wrapper.find('form').trigger('submit.prevent')

    expect(wrapper.text()).toContain('Klasserommets navn er påkrevd')
    expect(post).not.toHaveBeenCalled()
  })

  it('shows error when description is missing', async () => {
    const wrapper = mountComponent()

    await wrapper.find('#classroom-name').setValue('Tulleberg 5A')
    await wrapper.find('form').trigger('submit.prevent')

    expect(wrapper.text()).toContain('Beskrivelse er påkrevd')
    expect(post).not.toHaveBeenCalled()
  })

  it('successfully creates a classroom and shows success message', async () => {
    vi.mocked(post).mockResolvedValueOnce({
      classroomName: 'Tulleberg 5A',
      classroomCode: 'ABC123',
    })

    const wrapper = mountComponent()

    await wrapper.find('#classroom-name').setValue('Tulleberg 5A')
    await wrapper.find('#classroom-description').setValue('Beskrivelse av klasserommet')
    await wrapper.find('form').trigger('submit.prevent')

    expect(post).toHaveBeenCalledWith('/api/classrooms', {
      name: 'Tulleberg 5A',
      description: 'Beskrivelse av klasserommet',
    })

    expect(wrapper.text()).toContain(
      'Klasserom "Tulleberg 5A" opprettet! Klasserommets kode: ABC123',
    )

    expect(wrapper.find('form').exists()).toBe(false)

    vi.advanceTimersByTime(2000)

    expect(pushMock).toHaveBeenCalledWith('/teacher/classrooms')
  })

  it('hides the form after successful creation', async () => {
    vi.mocked(post).mockResolvedValueOnce({
      classroomName: 'Tulleberg 5A',
      classroomCode: 'ABC123',
    })

    const wrapper = mountComponent()

    await wrapper.find('#classroom-name').setValue('Tulleberg 5A')
    await wrapper.find('#classroom-description').setValue('Beskrivelse av klasserommet')
    await wrapper.find('form').trigger('submit.prevent')

    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.text()).toContain('Klasserom "Tulleberg 5A" opprettet!')
  })

  it('shows loading text while request is pending', async () => {
    vi.mocked(post).mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              classroomName: 'Tulleberg 5A',
              classroomCode: 'ABC123',
            })
          }, 1000)
        }),
    )

    const wrapper = mountComponent()

    await wrapper.find('#classroom-name').setValue('Tulleberg 5A')
    await wrapper.find('#classroom-description').setValue('Beskrivelse av klasserommet')
    await wrapper.find('form').trigger('submit.prevent')

    expect(wrapper.find('button[type="submit"]').text()).toBe('Oppretter...')
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('shows API error message when creating classroom fails with Error', async () => {
    vi.mocked(post).mockRejectedValueOnce(new Error('Kunne ikke opprette klasserom'))

    const wrapper = mountComponent()

    await wrapper.find('#classroom-name').setValue('Tulleberg 5A')
    await wrapper.find('#classroom-description').setValue('Beskrivelse av klasserommet')
    await wrapper.find('form').trigger('submit.prevent')

    expect(wrapper.text()).toContain('Kunne ikke opprette klasserom')
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('shows fallback error message when creating classroom fails with unknown error', async () => {
    vi.mocked(post).mockRejectedValueOnce('Ukjent feil')

    const wrapper = mountComponent()

    await wrapper.find('#classroom-name').setValue('Tulleberg 5A')
    await wrapper.find('#classroom-description').setValue('Beskrivelse av klasserommet')
    await wrapper.find('form').trigger('submit.prevent')

    expect(wrapper.text()).toContain('Feil ved oppretting av klasserom')
    expect(pushMock).not.toHaveBeenCalled()
  })
})
