import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsEditAvatarView from '@/views/pupil/PupilsEditAvatarView.vue'
import { get, patch } from '@/utils/apiUtils'
import { clearMeCache } from '@/utils/authRedirect'

const routerBack = vi.fn()
const routerPush = vi.fn()

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
  patch: vi.fn(),
}))

vi.mock('@/utils/authRedirect', () => ({
  clearMeCache: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    back: routerBack,
    push: routerPush,
  }),
}))

vi.mock('@/components/pupil/AvatarAssetsContainer.vue', () => ({
  default: {
    props: ['title', 'assets', 'selectedAssetName'],
    emits: ['select'],
    template: `
      <section>
        <h2>{{ title }}</h2>
        <button
          v-for="asset in assets"
          :key="asset.name"
          class="asset-button"
          :class="{ active: selectedAssetName === asset.name }"
          type="button"
          @click="$emit('select', asset)"
        >
          {{ asset.name }}
        </button>
      </section>
    `,
  },
}))

const mockedGet = vi.mocked(get)
const mockedPatch = vi.mocked(patch)
const mockedClearMeCache = vi.mocked(clearMeCache)

const avatarAssets = [
  { assetType: 'person', name: 'brown', filetype: 'png' },
  { assetType: 'person', name: 'white', filetype: 'png' },
  { assetType: 'hair', name: 'messy-hair-brown', filetype: 'png' },
  { assetType: 'hair', name: 'redditor-black', filetype: 'png' },
  { assetType: 'costume', name: 'detective', filetype: 'png' },
  { assetType: 'costume', name: 'sweater', filetype: 'png' },
]

const profile = {
  email: 'pupil@example.com',
  legalName: 'Pupil Name',
  role: 'PUPIL',
  username: 'detektiv',
  classroomCode: 'MAPAAA',
  avatarFileObjectId: 42,
  avatarSelection: 'white_redditor-black_sweater',
}

function mockAvatarAssetsFetch() {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(avatarAssets),
    }),
  )
}

async function mountLoadedView() {
  const wrapper = mount(PupilsEditAvatarView)

  await flushPromises()

  return wrapper
}

describe('PupilsEditAvatarView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedGet.mockResolvedValue(profile)
    mockedPatch.mockResolvedValue({})
    mockAvatarAssetsFetch()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('initializes selected assets from the saved avatar selection', async () => {
    const wrapper = await mountLoadedView()

    const activeButtons = wrapper.findAll('.asset-button.active').map((button) => button.text())

    expect(mockedGet).toHaveBeenCalledWith('/api/me')
    expect(fetch).toHaveBeenCalledWith('/api/avatar-assets', {
      credentials: 'include',
    })
    expect(activeButtons).toEqual(['white', 'redditor-black', 'sweater'])
  })

  it('falls back to the first assets when the saved avatar selection is missing', async () => {
    mockedGet.mockResolvedValue({
      ...profile,
      avatarSelection: null,
    })

    const wrapper = await mountLoadedView()

    const activeButtons = wrapper.findAll('.asset-button.active').map((button) => button.text())

    expect(activeButtons).toEqual(['brown', 'messy-hair-brown', 'detective'])
  })

  it('falls back to the first assets when the saved avatar selection does not match assets', async () => {
    mockedGet.mockResolvedValue({
      ...profile,
      avatarSelection: 'missing_missing_missing',
    })

    const wrapper = await mountLoadedView()

    const activeButtons = wrapper.findAll('.asset-button.active').map((button) => button.text())

    expect(activeButtons).toEqual(['brown', 'messy-hair-brown', 'detective'])
  })

  it('saves the selected avatar, clears me cache, and navigates back to profile', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.find('.primary-button').trigger('click')
    await flushPromises()

    expect(mockedPatch).toHaveBeenCalledWith('/api/me', {
      avatar: 'white_redditor-black_sweater',
    })
    expect(mockedClearMeCache).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith('/pupil/profile')
  })
})
