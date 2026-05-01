import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import TeachersPupilsView from '@/views/teacher/TeachersPupilsView.vue'
import { del, get, patch, put } from '@/utils/apiUtils'

const routerMocks = vi.hoisted(() => ({
  push: vi.fn(),
}))

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  patch: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      classroomCode: 'ABC123',
    },
  }),
  useRouter: () => ({
    push: routerMocks.push,
  }),
  onBeforeRouteLeave: vi.fn(),
}))

vi.mock('@/components/teacher/pupils/TeacherPupilsLeftPanel.vue', () => ({
  default: {
    name: 'TeacherPupilsLeftPanel',
    props: ['approvedPupils', 'pendingPupils', 'selectedPupilEmail'],
    emits: ['select-pupil'],
    template: `
      <div class="left-panel">
        <div v-for="pupil in approvedPupils" :key="pupil.email" class="approved-pupil">
          <span>{{ pupil.legalName }}</span>
          <button class="select-pupil" @click="$emit('select-pupil', pupil.email)">Select {{ pupil.email }}</button>
        </div>

        <div v-for="pupil in pendingPupils" :key="pupil.email" class="pending-pupil">
          <span>{{ pupil.legalName }}</span>
          <button class="select-pending-pupil" @click="$emit('select-pupil', pupil.email)">Select {{ pupil.email }}</button>
        </div>
      </div>
    `,
  },
}))

vi.mock('@/components/teacher/pupils/TeacherPupilsRightPanel.vue', () => ({
  default: {
    name: 'TeacherPupilsRightPanel',
    props: [
      'selectedPupil',
      'editUsername',
      'activePanel',
      'stopNames',
      'selectedStopName',
      'selectedChapterName',
      'selectedStopNote',
      'notebookLoading',
      'notebookError',
    ],
    emits: [
      'update:edit-username',
      'save-changes',
      'remove-pupil',
      'open-notes',
      'select-stop-note',
      'approve-pending-pupil',
      'reject-pending-pupil',
    ],
    template: `
      <div class="right-panel">
        <p v-if="selectedPupil" class="selected-pupil-name">{{ selectedPupil.legalName }}</p>
        <p v-if="selectedPupil" class="selected-pupil-username">{{ editUsername }}</p>
        <p v-if="selectedChapterName" class="chapter-name">{{ selectedChapterName }}</p>
        <p v-if="selectedStopNote" class="stop-note">{{ selectedStopNote }}</p>
        <p v-if="notebookLoading" class="notebook-loading">Laster notat...</p>
        <p v-if="notebookError" class="notebook-error">{{ notebookError }}</p>

        <button class="change-username" @click="$emit('update:edit-username', 'newUsername')">Change username</button>
        <button class="save-changes" @click="$emit('save-changes')">Save</button>
        <button class="remove-pupil" @click="$emit('remove-pupil')">Remove</button>
        <button class="open-notes" @click="$emit('open-notes')">Open notes</button>
        <button class="select-nyhetskvartalet" @click="$emit('select-stop-note', 'Nyhetskvartalet')">Nyhetskvartalet</button>
        <button class="select-fotografen" @click="$emit('select-stop-note', 'Fotografen')">Fotografen</button>
        <button class="approve-pending" @click="$emit('approve-pending-pupil')">Approve</button>
        <button class="reject-pending" @click="$emit('reject-pending-pupil')">Reject</button>
      </div>
    `,
  },
}))

describe('TeachersPupilsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routerMocks.push.mockReset()

    vi.mocked(get).mockImplementation((url: string) => {
      if (url === '/api/classrooms/ABC123/pupils') {
        return Promise.resolve([
          {
            accountEmail: 'approved@example.com',
            legalName: 'Approved Pupil',
            username: 'approvedUser',
            avatarObjectKey: 'avatars/approved.png',
            totalXp: 100,
          },
        ])
      }

      if (url === '/api/classrooms/ABC123/pupils/pending') {
        return Promise.resolve([
          {
            accountEmail: 'pending@example.com',
            legalName: 'Pending Pupil',
            username: 'pendingUser',
            avatarObjectKey: null,
          },
        ])
      }

      if (
        url === '/api/teacher/classrooms/ABC123/pupils/approved@example.com/chapters/1/notebook'
      ) {
        return Promise.resolve({
          chapterId: 1,
          chapterName: 'Nyhetskvartalet',
          tips: [],
          ownNotes: 'Elevens notat',
        })
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const mountComponent = async () => {
    const wrapper = mount(TeachersPupilsView)
    await flushPromises()
    return wrapper
  }

  it('loads and renders approved and pending pupils', async () => {
    const wrapper = await mountComponent()

    expect(get).toHaveBeenCalledWith('/api/classrooms/ABC123/pupils')
    expect(get).toHaveBeenCalledWith('/api/classrooms/ABC123/pupils/pending')

    expect(wrapper.text()).toContain('Approved Pupil')
    expect(wrapper.text()).toContain('Pending Pupil')
  })

  it('shows error when loading pupils fails', async () => {
    vi.mocked(get).mockRejectedValueOnce(new Error('Failed to load pupils'))

    const wrapper = mount(TeachersPupilsView)
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load pupils')
  })

  it('selects an approved pupil and shows details', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')

    expect(wrapper.text()).toContain('Approved Pupil')
    expect(wrapper.text()).toContain('approvedUser')
  })

  it('selects a pending pupil and shows details', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.select-pending-pupil').trigger('click')

    expect(wrapper.text()).toContain('Pending Pupil')
    expect(wrapper.text()).toContain('pendingUser')
  })

  it('loads notebook when notes panel and stop are selected for approved pupil', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.open-notes').trigger('click')
    await wrapper.find('.select-nyhetskvartalet').trigger('click')
    await flushPromises()

    expect(get).toHaveBeenCalledWith(
      '/api/teacher/classrooms/ABC123/pupils/approved@example.com/chapters/1/notebook',
    )

    expect(wrapper.text()).toContain('Nyhetskvartalet')
    expect(wrapper.text()).toContain('Elevens notat')
  })

  it('shows notebook error when notebook loading fails', async () => {
    vi.mocked(get).mockImplementation((url: string) => {
      if (url === '/api/classrooms/ABC123/pupils') {
        return Promise.resolve([
          {
            accountEmail: 'approved@example.com',
            legalName: 'Approved Pupil',
            username: 'approvedUser',
            avatarObjectKey: null,
            totalXp: 100,
          },
        ])
      }

      if (url === '/api/classrooms/ABC123/pupils/pending') {
        return Promise.resolve([])
      }

      if (url.includes('/notebook')) {
        return Promise.reject(new Error('Failed to load notebook'))
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.open-notes').trigger('click')
    await wrapper.find('.select-nyhetskvartalet').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to load notebook')
  })

  it('ignores 409 notebook error and does not show notebook error', async () => {
    vi.mocked(get).mockImplementation((url: string) => {
      if (url === '/api/classrooms/ABC123/pupils') {
        return Promise.resolve([
          {
            accountEmail: 'approved@example.com',
            legalName: 'Approved Pupil',
            username: 'approvedUser',
            avatarObjectKey: null,
            totalXp: 100,
          },
        ])
      }

      if (url === '/api/classrooms/ABC123/pupils/pending') {
        return Promise.resolve([])
      }

      if (url.includes('/notebook')) {
        return Promise.reject(new Error('409 Conflict'))
      }

      return Promise.reject(new Error('Unknown endpoint'))
    })

    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.open-notes').trigger('click')
    await wrapper.find('.select-nyhetskvartalet').trigger('click')
    await flushPromises()

    expect(wrapper.text()).not.toContain('409 Conflict')
  })

  it('saves username changes for approved pupil', async () => {
    vi.mocked(patch).mockResolvedValueOnce({
      accountEmail: 'approved@example.com',
      legalName: 'Approved Pupil',
      username: 'newUsername',
      avatarObjectKey: null,
      totalXp: 150,
    })

    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.change-username').trigger('click')
    await wrapper.find('.save-changes').trigger('click')
    await flushPromises()

    expect(patch).toHaveBeenCalledWith(
      '/api/classrooms/ABC123/pupils/approved@example.com/username',
      {
        username: 'newUsername',
      },
    )
  })

  it('shows error when saving username fails', async () => {
    vi.mocked(patch).mockRejectedValueOnce(new Error('Failed to save pupil changes'))

    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.change-username').trigger('click')
    await wrapper.find('.save-changes').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to save pupil changes')
  })

  it('opens a custom confirmation popup before removing an approved pupil', async () => {
    vi.mocked(del).mockResolvedValueOnce(undefined)

    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.remove-pupil').trigger('click')
    await flushPromises()

    expect(window.confirm).not.toHaveBeenCalled()
    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(true)
    expect(wrapper.text()).toContain('Er du sikker på at du vil fjerne')
    expect(wrapper.text()).toContain('Approved Pupil')
    expect(del).not.toHaveBeenCalled()

    await wrapper.find('.confirm-remove-pupil').trigger('click')
    await flushPromises()

    expect(del).toHaveBeenCalledWith('/api/classrooms/ABC123/pupils/approved@example.com')
  })

  it('closes remove pupil popup without removing when cancelled', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.remove-pupil').trigger('click')
    await flushPromises()

    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(true)
    await wrapper.find('.cancel-remove-pupil').trigger('click')
    await flushPromises()

    expect(window.confirm).not.toHaveBeenCalled()
    expect(del).not.toHaveBeenCalled()
    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(false)
  })

  it('shows error when removing pupil fails', async () => {
    vi.mocked(del).mockRejectedValueOnce(new Error('Failed to remove pupil'))

    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.remove-pupil').trigger('click')
    await wrapper.find('.confirm-remove-pupil').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to remove pupil')
  })

  it('approves pending pupil', async () => {
    vi.mocked(put).mockResolvedValueOnce(undefined)

    const wrapper = await mountComponent()

    await wrapper.find('.select-pending-pupil').trigger('click')
    await wrapper.find('.approve-pending').trigger('click')
    await flushPromises()

    expect(put).toHaveBeenCalledWith('/api/classrooms/ABC123/pupils/pending@example.com/accept', {})
  })

  it('shows error when approving pending pupil fails', async () => {
    vi.mocked(put).mockRejectedValueOnce(new Error('Failed to approve pupil'))

    const wrapper = await mountComponent()

    await wrapper.find('.select-pending-pupil').trigger('click')
    await wrapper.find('.approve-pending').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to approve pupil')
  })

  it('rejects pending pupil after confirmation', async () => {
    vi.mocked(del).mockResolvedValueOnce(undefined)

    const wrapper = await mountComponent()

    await wrapper.find('.select-pending-pupil').trigger('click')
    await wrapper.find('.reject-pending').trigger('click')
    await flushPromises()

    expect(window.confirm).not.toHaveBeenCalled()
    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(true)
    expect(wrapper.text()).toContain('Er du sikker på at du vil avslå')
    expect(wrapper.text()).toContain('Pending Pupil')
    expect(del).not.toHaveBeenCalled()

    await wrapper.find('.confirm-reject-pending').trigger('click')
    await flushPromises()

    expect(del).toHaveBeenCalledWith('/api/classrooms/ABC123/pupils/pending@example.com')
  })

  it('closes reject pending pupil popup without rejecting when cancelled', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.select-pending-pupil').trigger('click')
    await wrapper.find('.reject-pending').trigger('click')
    await flushPromises()

    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(true)
    await wrapper.find('.cancel-reject-pending').trigger('click')
    await flushPromises()

    expect(window.confirm).not.toHaveBeenCalled()
    expect(del).not.toHaveBeenCalled()
    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(false)
  })

  it('shows error when rejecting pending pupil fails', async () => {
    vi.mocked(del).mockRejectedValueOnce(new Error('Failed to reject pupil'))

    const wrapper = await mountComponent()

    await wrapper.find('.select-pending-pupil').trigger('click')
    await wrapper.find('.reject-pending').trigger('click')
    await wrapper.find('.confirm-reject-pending').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Failed to reject pupil')
  })

  it('asks before switching pupil when username changes are unsaved', async () => {
    const wrapper = await mountComponent()

    await wrapper.find('.select-pupil').trigger('click')
    await wrapper.find('.change-username').trigger('click')
    await wrapper.find('.select-pending-pupil').trigger('click')

    expect(window.confirm).not.toHaveBeenCalled()
    expect(wrapper.find('.teacher-confirm-popup').exists()).toBe(true)
    expect(wrapper.text()).toContain('Du har endret brukernavnet uten å lagre')
    expect(wrapper.find('.selected-pupil-name').text()).toBe('Approved Pupil')

    await wrapper.find('.confirm-discard-username').trigger('click')
    await flushPromises()

    expect(wrapper.find('.selected-pupil-name').text()).toBe('Pending Pupil')
  })
})
