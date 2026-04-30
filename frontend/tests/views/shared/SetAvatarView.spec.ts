import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import SetAvatarView from '@/views/shared/SetAvatarView.vue'
import { put } from '@/utils/apiUtils'
import { clearMeCache } from '@/utils/authRedirect'

const routerBack = vi.fn()
const routerPush = vi.fn()
const routeState = {
  query: {
    classroomCode: 'MAPAAA',
  } as Record<string, string | undefined>,
}

vi.mock('@/utils/apiUtils', () => ({
  put: vi.fn(),
}))

vi.mock('@/utils/authRedirect', () => ({
  clearMeCache: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
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
          type="button"
          @click="$emit('select', asset)"
        >
          {{ asset.name }}
        </button>
      </section>
    `,
  },
}))

const mockedPut = vi.mocked(put)
const mockedClearMeCache = vi.mocked(clearMeCache)

const avatarAssets = [
  {
    assetType: 'person',
    name: 'person1',
    filetype: 'png',
  },
  {
    assetType: 'hair',
    name: 'hair1',
    filetype: 'png',
  },
  {
    assetType: 'costume',
    name: 'costume1',
    filetype: 'png',
  },
]

function mockAvatarAssetsFetch() {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: () => Promise.resolve(avatarAssets),
  }))
}

async function mountLoadedView() {
  const wrapper = mount(SetAvatarView)

  await flushPromises()

  return wrapper
}

describe('SetAvatarView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.query.classroomCode = 'MAPAAA'
    mockedPut.mockResolvedValue({})
    mockAvatarAssetsFetch()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('loads avatar choices from the backend', async () => {
    await mountLoadedView()

    expect(fetch).toHaveBeenCalledWith('/api/avatar-assets', {
      credentials: 'include',
    })
  })

  it('saves membership setup, clears me cache, and navigates to waiting page', async () => {
    const wrapper = await mountLoadedView()

    await wrapper.find('#classroom-username').setValue('detektiv')
    await wrapper.find('.primary-button').trigger('click')
    await flushPromises()

    expect(mockedPut).toHaveBeenCalledWith('/api/pupil/classrooms/MAPAAA/membership', {
      username: 'detektiv',
      avatar: 'person1_hair1_costume1',
    })
    expect(mockedClearMeCache).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith('/join-classroom/waiting-classroom')
  })

  it('does not save without a classroom code', async () => {
    routeState.query.classroomCode = undefined

    const wrapper = await mountLoadedView()

    await wrapper.find('#classroom-username').setValue('detektiv')
    await wrapper.find('.primary-button').trigger('click')
    await flushPromises()

    expect(mockedPut).not.toHaveBeenCalled()
    expect(mockedClearMeCache).not.toHaveBeenCalled()
  })

  it('does not clear me cache when membership save fails', async () => {
    mockedPut.mockRejectedValue(new Error('Save failed'))
    const wrapper = await mountLoadedView()

    await wrapper.find('#classroom-username').setValue('detektiv')
    await wrapper.find('.primary-button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Save failed')
    expect(mockedClearMeCache).not.toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()
  })
})
