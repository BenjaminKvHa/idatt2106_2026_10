import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TeachersEditTaskView from '@/views/teacher/TeachersEditTasksView.vue'
import { get, patchForm, postForm } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  postForm: vi.fn(),
  patchForm: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      classroomCode: 'ABC123',
    },
  }),
}))

vi.mock('@/components/teacher/mystery/TeacherNyhetskvartaletTaskForm.vue', () => ({
  default: {
    name: 'TeacherNyhetskvartaletTaskForm',
    props: ['task', 'parsedTask', 'submitting', 'resetToken', 'title', 'lockedDifficulty', 'hideDifficulty'],
    emits: ['submit', 'cancel-edit'],
    template: `
      <div class="nyhets-form">
        <button class="submit-nyhets" @click="$emit('submit', {
          difficulty: 'EASY',
          taskDescription: 'Nyhetsoppgave',
          correctFileSlot: 'file1',
          correctDescription: 'Riktig',
          incorrectDescription: 'Feil'
        })">
          Submit nyhets
        </button>
        <button class="cancel-nyhets" @click="$emit('cancel-edit')">Cancel</button>
      </div>
    `,
  },
}))

vi.mock('@/components/teacher/mystery/TeacherFotografenTaskForm.vue', () => ({
  default: {
    name: 'TeacherFotografenTaskForm',
    props: ['task', 'parsedTask', 'submitting', 'resetToken', 'title', 'lockedDifficulty', 'hideDifficulty'],
    emits: ['submit', 'cancel-edit'],
    template: `
      <div class="fotografen-form">
        <button class="submit-fotografen" @click="$emit('submit', {
          difficulty: 'MEDIUM',
          taskDescription: 'Fotografenoppgave',
          correctCategory: 'REAL',
          correctJustification: 'Ser ekte ut',
          justifications: ['begrunnelse'],
          correctDescription: 'Riktig bilde',
          falseDescription: 'Feil bilde'
        })">
          Submit fotografen
        </button>
        <button class="cancel-fotografen" @click="$emit('cancel-edit')">Cancel</button>
      </div>
    `,
  },
}))

vi.mock('@/components/teacher/mystery/TeacherMysteryTaskCard.vue', () => ({
  default: {
    name: 'TeacherMysteryTaskCard',
    props: ['chapter', 'task', 'imageTask', 'newsTask', 'format', 'selected'],
    emits: ['edit'],
    template: `
      <div class="task-card">
        <span>Task #{{ task.id }}</span>
        <button class="edit-task" @click="$emit('edit', task.id)">Edit</button>
      </div>
    `,
  },
}))

vi.mock('@/components/teacher/mystery/types', () => ({
  CHAPTER_ID_BY_TASK_KEY: {
    nyhetskvartalet: 1,
    fotografen: 2,
    datasenteret: 3,
  },
  CHAPTER_LABELS: {
    nyhetskvartalet: 'Nyhetskvartalet',
    fotografen: 'Fotografen',
    datasenteret: 'Datasenteret',
  },
  DATACENTER_FORMAT_LABELS: {
    NEWS_VERIFICATION: 'Nyhetsverifisering',
    IMAGE_VERIFICATION: 'Bildeverifisering',
  },
  parseDatacenterTask: vi.fn((taskJson: string) => JSON.parse(taskJson)),
  parseImageVerificationTask: vi.fn((taskJson: string) => JSON.parse(taskJson)),
  parseNewsVerificationTask: vi.fn((taskJson: string) => JSON.parse(taskJson)),
}))

describe('TeachersEditTaskView', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    vi.mocked(get).mockImplementation((url: string) => {
      if (url.includes('/api/admin/tasks/nyhetskvartalet')) {
        return Promise.resolve([
          {
            id: 1,
            taskJson: JSON.stringify({
              taskDescription: 'Nyhetsoppgave',
            }),
          },
        ])
      }

      if (url.includes('/api/admin/tasks/fotografen')) {
        return Promise.resolve([
          {
            id: 2,
            taskJson: JSON.stringify({
              taskDescription: 'Fotografenoppgave',
            }),
          },
        ])
      }

      if (url.includes('/api/admin/tasks/datasenteret')) {
        return Promise.resolve([
          {
            id: 3,
            taskJson: JSON.stringify({
              format: 'NEWS_VERIFICATION',
              newsTask: {
                taskDescription: 'Datasenter nyhetsoppgave',
              },
              imageTask: null,
            }),
          },
        ])
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })
  })

  const mountComponent = async () => {
    const wrapper = mount(TeachersEditTaskView)
    await flushPromises()
    return wrapper
  }

  it('loads and renders page title and default nyhetskvartalet tasks', async () => {
    const wrapper = await mountComponent()

    expect(wrapper.text()).toContain('Rediger oppgaver')
    expect(wrapper.text()).toContain('Kapitteloppgaver')
    expect(wrapper.text()).toContain('Task #1')
    expect(get).toHaveBeenCalledWith('/api/admin/tasks/nyhetskvartalet?classroomCode=ABC123')
    expect(get).toHaveBeenCalledWith('/api/admin/tasks/fotografen?classroomCode=ABC123')
    expect(get).toHaveBeenCalledWith('/api/admin/tasks/datasenteret?classroomCode=ABC123')
  })

  it('switches to fotografen tab and shows fotografen tasks', async () => {
    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Fotografen')?.trigger('click')

    expect(wrapper.text()).toContain('Task #2')
    expect(wrapper.find('.fotografen-form').exists()).toBe(true)
  })

  it('switches to datasenteret tab and shows datasenteret tasks', async () => {
    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Datasenteret')?.trigger('click')

    expect(wrapper.text()).toContain('Task #3')
    expect(wrapper.text()).toContain('Nyhetsverifisering')
  })

  it('successfully creates a nyhetskvartalet task', async () => {
    vi.mocked(postForm).mockResolvedValueOnce({
      id: 10,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.find('.submit-nyhets').trigger('click')
    await flushPromises()

    expect(postForm).toHaveBeenCalledWith('/api/admin/tasks/nyhetskvartalet', expect.any(FormData))
    expect(wrapper.text()).toContain('Nyhetskvartalet-oppgave #10 ble opprettet.')
  })

  it('successfully creates a fotografen task', async () => {
    vi.mocked(postForm).mockResolvedValueOnce({
      id: 20,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Fotografen')?.trigger('click')
    await wrapper.find('.submit-fotografen').trigger('click')
    await flushPromises()

    expect(postForm).toHaveBeenCalledWith('/api/admin/tasks/fotografen', expect.any(FormData))
    expect(wrapper.text()).toContain('Fotografen-oppgave #20 ble opprettet.')
  })

  it('successfully creates a datasenteret news task', async () => {
    vi.mocked(postForm).mockResolvedValueOnce({
      id: 30,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Datasenteret')?.trigger('click')
    await wrapper.find('.submit-nyhets').trigger('click')
    await flushPromises()

    expect(postForm).toHaveBeenCalledWith('/api/admin/tasks/datasenteret/news', expect.any(FormData))
    expect(wrapper.text()).toContain('Datasenteret nyhetsoppgave #30 ble opprettet.')
  })

  it('successfully creates a datasenteret image task', async () => {
    vi.mocked(postForm).mockResolvedValueOnce({
      id: 40,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Datasenteret')?.trigger('click')
    await wrapper.findAll('button').find((button) => button.text() === 'Bildeverifisering')?.trigger('click')
    await wrapper.find('.submit-fotografen').trigger('click')
    await flushPromises()

    expect(postForm).toHaveBeenCalledWith('/api/admin/tasks/datasenteret/image', expect.any(FormData))
    expect(wrapper.text()).toContain('Datasenteret fotografen-oppgave #40 ble opprettet.')
  })

  it('successfully updates an existing nyhetskvartalet task', async () => {
    vi.mocked(patchForm).mockResolvedValueOnce({
      id: 1,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.find('.edit-task').trigger('click')
    await wrapper.find('.submit-nyhets').trigger('click')
    await flushPromises()

    expect(patchForm).toHaveBeenCalledWith('/api/admin/tasks/nyhetskvartalet/1', expect.any(FormData))
    expect(wrapper.text()).toContain('Nyhetskvartalet-oppgave #1 ble oppdatert.')
  })

  it('successfully updates an existing fotografen task', async () => {
    vi.mocked(patchForm).mockResolvedValueOnce({
      id: 2,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Fotografen')?.trigger('click')
    await wrapper.find('.edit-task').trigger('click')
    await wrapper.find('.submit-fotografen').trigger('click')
    await flushPromises()

    expect(patchForm).toHaveBeenCalledWith('/api/admin/tasks/fotografen/2', expect.any(FormData))
    expect(wrapper.text()).toContain('Fotografen-oppgave #2 ble oppdatert.')
  })

  it('successfully updates an existing datasenteret news task', async () => {
    vi.mocked(patchForm).mockResolvedValueOnce({
      id: 3,
      taskJson: '{}',
    })

    const wrapper = await mountComponent()

    await wrapper.findAll('button').find((button) => button.text() === 'Datasenteret')?.trigger('click')
    await wrapper.find('.edit-task').trigger('click')
    await wrapper.find('.submit-nyhets').trigger('click')
    await flushPromises()

    expect(patchForm).toHaveBeenCalledWith('/api/admin/tasks/datasenteret/news/3', expect.any(FormData))
    expect(wrapper.text()).toContain('Datasenteret nyhetsoppgave #3 ble oppdatert.')
  })

  it('clears editor when cancel edit is emitted', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.edit-task').trigger('click')
    await wrapper.find('.cancel-nyhets').trigger('click')

    expect(wrapper.text()).not.toContain('ble oppdatert')
  })

  it('shows error when loading tasks fails', async () => {
    vi.mocked(get).mockRejectedValueOnce(new Error('Failed to load tasks'))

    const wrapper = mount(TeachersEditTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load tasks')
  })

  it('shows fallback error when loading tasks fails with unknown error', async () => {
    vi.mocked(get).mockRejectedValueOnce('Unknown failure')

    const wrapper = mount(TeachersEditTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load tasks')
  })

  it('shows error when creating nyhetskvartalet task fails', async () => {
    vi.mocked(postForm).mockRejectedValueOnce(new Error('Kunne ikke lagre oppgave'))

    const wrapper = await mountComponent()

    await wrapper.find('.submit-nyhets').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke lagre oppgave')
  })

  it('shows fallback error when saving task fails with unknown error', async () => {
    vi.mocked(postForm).mockRejectedValueOnce('Unknown save error')

    const wrapper = await mountComponent()

    await wrapper.find('.submit-nyhets').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to save task')
  })

  it('shows empty state when active chapter has no tasks', async () => {
    vi.mocked(get).mockImplementation((url: string) => {
      if (url.includes('/api/admin/tasks/nyhetskvartalet')) {
        return Promise.resolve([])
      }

      if (url.includes('/api/admin/tasks/fotografen')) {
        return Promise.resolve([])
      }

      if (url.includes('/api/admin/tasks/datasenteret')) {
        return Promise.resolve([])
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    const wrapper = mount(TeachersEditTaskView)
    await flushPromises()

    expect(wrapper.text()).toContain('Ingen oppgaver funnet for Nyhetskvartalet.')
  })
})
