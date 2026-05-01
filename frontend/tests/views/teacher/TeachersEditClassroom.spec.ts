import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TeachersEditClassroom from '@/views/teacher/TeachersEditClassroom.vue'
import { del, get, post, put } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}))

const pushMock = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      classroomCode: 'ABC123',
    },
  }),
  useRouter: () => ({
    push: pushMock,
  }),
}))

describe('TeachersEditClassroom', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(get).mockImplementation((url: string) => {
      if (url === '/api/classrooms/ABC123/leaderboard') {
        return Promise.resolve({
          classroomCode: 'ABC123',
          classroomName: 'Tulleberg 5A',
          classroomDescription: 'Et hyggelig klasserom',
        })
      }

      if (url === '/api/classrooms/ABC123/teachers/me/can-leave') {
        return Promise.resolve({
          canLeave: true,
        })
      }

      if (url === '/api/classrooms/ABC123/teachers') {
        return Promise.resolve([
          {
            email: 'teacher@example.com',
            legalName: 'Test Teacher',
          },
        ])
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const mountComponent = async () => {
    const wrapper = mount(TeachersEditClassroom)
    await flushPromises()
    return wrapper
  }

  it('loads and renders classroom details', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.text()).toContain('Tulleberg 5A')
    expect(wrapper.text()).toContain('Et hyggelig klasserom')
    expect(wrapper.text()).toContain('ABC123')
  })

  it('loads and renders teachers', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.text()).toContain('Lærere i klasserommet')
    expect(wrapper.text()).toContain('Test Teacher')
    expect(wrapper.text()).toContain('teacher@example.com')
  })

  it('opens edit form with existing classroom values', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.edit-classroom-btn').trigger('click')

    const nameInput = wrapper.find('input.teacher-input')
    const descriptionTextarea = wrapper.find('textarea.teacher-textarea')

    expect((nameInput.element as HTMLInputElement).value).toBe('Tulleberg 5A')
    expect((descriptionTextarea.element as HTMLTextAreaElement).value).toBe('Et hyggelig klasserom')
  })

  it('successfully updates classroom details', async () => {
    vi.mocked(put).mockResolvedValueOnce({
      classroomCode: 'ABC123',
      classroomName: 'Ny klasse',
      classroomDescription: 'Ny beskrivelse',
    })

    const wrapper = await mountComponent()

    await wrapper.find('.edit-classroom-btn').trigger('click')

    const nameInput = wrapper.find('input.teacher-input')
    const descriptionTextarea = wrapper.find('textarea.teacher-textarea')

    await nameInput.setValue('Ny klasse')
    await descriptionTextarea.setValue('Ny beskrivelse')

    await wrapper.findAll('button').find((button) => button.text() === 'Lagre endringer')?.trigger('click')
    await flushPromises()

    expect(put).toHaveBeenCalledWith('/api/classrooms/ABC123', {
      name: 'Ny klasse',
      description: 'Ny beskrivelse',
    })

    expect(wrapper.text()).toContain('Ny klasse')
    expect(wrapper.text()).toContain('Ny beskrivelse')
    expect(wrapper.find('.edit-card').exists()).toBe(false)
  })

  it('disables save button when classroom name is empty while editing', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.edit-classroom-btn').trigger('click')

    const nameInput = wrapper.find('input.teacher-input')
    await nameInput.setValue('   ')

    const saveButton = wrapper.findAll('button').find((button) => button.text() === 'Lagre endringer')

    expect(saveButton?.attributes('disabled')).toBeDefined()
    expect(put).not.toHaveBeenCalled()
  })

  it('shows error when updating classroom fails', async () => {
    vi.mocked(put).mockRejectedValueOnce(new Error('Kunne ikke oppdatere klasserom'))

    const wrapper = await mountComponent()

    await wrapper.find('.edit-classroom-btn').trigger('click')

    await wrapper.find('input.teacher-input').setValue('Ny klasse')
    await wrapper.find('textarea.teacher-textarea').setValue('Ny beskrivelse')

    await wrapper.findAll('button').find((button) => button.text() === 'Lagre endringer')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke oppdatere klasserom')
  })

  it('cancels edit and closes edit form', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.edit-classroom-btn').trigger('click')
    expect(wrapper.find('.edit-card').exists()).toBe(true)

    await wrapper.findAll('button').find((button) => button.text() === 'Avbryt')?.trigger('click')

    expect(wrapper.find('.edit-card').exists()).toBe(false)
  })

  it('successfully invites a teacher', async () => {
    vi.mocked(post).mockResolvedValueOnce(undefined)

    const wrapper = await mountComponent()

    const emailInput = wrapper.find('input[type="email"]')
    await emailInput.setValue('new.teacher@example.com')

    await wrapper.findAll('button').find((button) => button.text() === 'Legg til lærer')?.trigger('click')
    await flushPromises()

    expect(post).toHaveBeenCalledWith('/api/classrooms/ABC123/teachers', {
      email: 'new.teacher@example.com',
    })

    expect((emailInput.element as HTMLInputElement).value).toBe('')
  })

  it('does not invite teacher when email is empty', async () => {
    const wrapper = await mountComponent()

    const inviteButton = wrapper.findAll('button').find((button) => button.text() === 'Legg til lærer')

    expect(inviteButton?.attributes('disabled')).toBeDefined()

    await inviteButton?.trigger('click')

    expect(post).not.toHaveBeenCalled()
  })

  it('shows error when inviting teacher fails', async () => {
    vi.mocked(post).mockRejectedValueOnce(new Error('Kunne ikke invitere lærer'))

    const wrapper = await mountComponent()

    await wrapper.find('input[type="email"]').setValue('new.teacher@example.com')

    await wrapper.findAll('button').find((button) => button.text() === 'Legg til lærer')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke invitere lærer')
  })

  it('successfully deletes classroom after confirmation', async () => {
    vi.mocked(del).mockResolvedValueOnce(undefined)

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Slett klasserom')?.trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledWith(
      'Er du sikker på at du vil slette klasserommet? Dette kan ikke angres.',
    )

    expect(del).toHaveBeenCalledWith('/api/classrooms/ABC123')
    expect(pushMock).toHaveBeenCalledWith('/teacher/classrooms')
  })

  it('does not delete classroom when confirmation is cancelled', async () => {
    vi.mocked(window.confirm).mockReturnValueOnce(false)

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Slett klasserom')?.trigger('click')
    await flushPromises()

    expect(del).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('shows error when deleting classroom fails', async () => {
    vi.mocked(del).mockRejectedValueOnce(new Error('Kunne ikke slette klasserom'))

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Slett klasserom')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke slette klasserom')
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('successfully leaves classroom after confirmation', async () => {
    vi.mocked(del).mockResolvedValueOnce(undefined)

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Forlat klasserom')?.trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledWith('Er du sikker på at du vil forlate klasserommet?')
    expect(del).toHaveBeenCalledWith('/api/classrooms/ABC123/teachers/me')
    expect(pushMock).toHaveBeenCalledWith('/teacher/classrooms')
  })

  it('does not leave classroom when confirmation is cancelled', async () => {
    vi.mocked(window.confirm).mockReturnValueOnce(false)

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Forlat klasserom')?.trigger('click')
    await flushPromises()

    expect(del).not.toHaveBeenCalled()
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('shows error when leaving classroom fails', async () => {
    vi.mocked(del).mockRejectedValueOnce(new Error('Kunne ikke forlate klasserom'))

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Forlat klasserom')?.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke forlate klasserom')
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('shows error when loading classroom fails', async () => {
    vi.mocked(get).mockRejectedValueOnce(new Error('Failed to load classroom'))

    const wrapper = mount(TeachersEditClassroom)
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load classroom')
  })

  it('shows error when loading teachers fails', async () => {
    vi.mocked(get).mockImplementation((url: string) => {
      if (url === '/api/classrooms/ABC123/leaderboard') {
        return Promise.resolve({
          classroomCode: 'ABC123',
          classroomName: 'Tulleberg 5A',
          classroomDescription: 'Et hyggelig klasserom',
        })
      }

      if (url === '/api/classrooms/ABC123/teachers/me/can-leave') {
        return Promise.resolve({
          canLeave: true,
        })
      }

      if (url === '/api/classrooms/ABC123/teachers') {
        return Promise.reject(new Error('Failed to load teachers'))
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    const wrapper = mount(TeachersEditClassroom)
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load teachers')
  })

  it('hides leave button when teacher cannot leave classroom', async () => {
    vi.mocked(get).mockImplementation((url: string) => {
      if (url === '/api/classrooms/ABC123/leaderboard') {
        return Promise.resolve({
          classroomCode: 'ABC123',
          classroomName: 'Tulleberg 5A',
          classroomDescription: 'Et hyggelig klasserom',
        })
      }

      if (url === '/api/classrooms/ABC123/teachers/me/can-leave') {
        return Promise.resolve({
          canLeave: false,
        })
      }

      if (url === '/api/classrooms/ABC123/teachers') {
        return Promise.resolve([])
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    const wrapper = mount(TeachersEditClassroom)
    await flushPromises()

    expect(wrapper.text()).not.toContain('Forlat klasserom')
  })
})
