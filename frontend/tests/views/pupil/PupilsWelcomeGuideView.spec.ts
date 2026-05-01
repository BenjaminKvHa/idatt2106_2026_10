import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import PupilsWelcomeGuideView from '@/views/pupil/PupilsWelcomeGuideView.vue'
import { post } from '@/utils/apiUtils'
import { clearMeCache } from '@/utils/authRedirect'

const routerReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({
    replace: routerReplace,
  }),
}))

vi.mock('@/utils/apiUtils', () => ({
  post: vi.fn(),
}))

vi.mock('@/utils/authRedirect', () => ({
  clearMeCache: vi.fn(),
}))

const mockedPost = vi.mocked(post)
const mockedClearMeCache = vi.mocked(clearMeCache)

function mountWelcomeGuide() {
  return mount(PupilsWelcomeGuideView)
}

describe('PupilsWelcomeGuideView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedPost.mockResolvedValue(null)
  })

  it('completes the intro before routing pupils to the dashboard', async () => {
    const wrapper = mountWelcomeGuide()

    await wrapper.find('.ready-button').trigger('click')
    await flushPromises()

    expect(mockedPost).toHaveBeenCalledWith('/api/pupil/intro/complete', undefined)
    expect(mockedClearMeCache).toHaveBeenCalledTimes(1)
    expect(routerReplace).toHaveBeenCalledWith('/pupil')
  })

  it('keeps pupils on the intro page when completion fails', async () => {
    mockedPost.mockRejectedValueOnce(new Error('Kunne ikke lagre intro'))
    const wrapper = mountWelcomeGuide()

    await wrapper.find('.ready-button').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Kunne ikke lagre intro')
    expect(mockedClearMeCache).not.toHaveBeenCalled()
    expect(routerReplace).not.toHaveBeenCalled()
  })
})
