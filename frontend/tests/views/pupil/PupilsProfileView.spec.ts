import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsProfileView from '@/views/pupil/PupilsProfileView.vue'
import { del, get, post, put } from '@/utils/apiUtils'

const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  clearAuthSessionStorage: vi.fn(),
  del: vi.fn(),
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}))

vi.mock('@/router', () => ({
  clearMeCache: vi.fn(),
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :href="to"><slot /></a>',
  },
  useRouter: () => ({
    push: routerPush,
  }),
}))

const mockedGet = vi.mocked(get)
const mockedPut = vi.mocked(put)
const mockedPost = vi.mocked(post)
const mockedDel = vi.mocked(del)

const profileWithClassroom = {
  email: 'pupil@example.com',
  legalName: 'Pupil Name',
  role: 'PUPIL',
  username: 'detektiv',
  classroomCode: 'MAPAAA',
  avatarFileObjectId: null,
}

function mountProfileView() {
  return mount(PupilsProfileView, {
    global: {
      stubs: {
        TopBar: true,
        VolumeControl: true,
      },
    },
  })
}

describe('PupilsProfileView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    mockedGet.mockReset()
    mockedPut.mockReset()
    mockedPost.mockReset()
    mockedDel.mockReset()
  })

  it('loads the authenticated pupil profile on mount', async () => {
    mockedGet.mockResolvedValue({
      ...profileWithClassroom,
      classroomCode: null,
    })

    mountProfileView()

    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/me')
  })

  it('loads classroom metadata when the profile has a classroom code', async () => {
    mockedGet
      .mockResolvedValueOnce(profileWithClassroom)
      .mockResolvedValueOnce({
        classroomCode: 'MAPAAA',
        classroomName: 'Kart testklasse',
        classroomDescription: 'Utviklingsklasse for kartvisning',
      })

    const wrapper = mountProfileView()

    await flushPromises()

    expect(mockedGet).toHaveBeenNthCalledWith(1, '/api/me')
    expect(mockedGet).toHaveBeenNthCalledWith(2, '/api/classrooms/MAPAAA')
    expect(wrapper.text()).toContain('Kart testklasse (MAPAAA)')
  })

  it('falls back to classroom code when classroom metadata fails to load', async () => {
    mockedGet
      .mockResolvedValueOnce(profileWithClassroom)
      .mockRejectedValueOnce(new Error('Classroom unavailable'))

    const wrapper = mountProfileView()

    await flushPromises()

    expect(wrapper.text()).toContain('MAPAAA')
    expect(wrapper.text()).toContain('Kunne ikke laste klasseromsnavn.')
  })

  it('does not request classroom metadata when the pupil has no classroom code', async () => {
    mockedGet.mockResolvedValue({
      ...profileWithClassroom,
      classroomCode: null,
    })

    const wrapper = mountProfileView()

    await flushPromises()

    expect(mockedGet).toHaveBeenCalledTimes(1)
    expect(mockedGet).toHaveBeenCalledWith('/api/me')
    expect(wrapper.text()).not.toContain('Klasserom')
  })
})
