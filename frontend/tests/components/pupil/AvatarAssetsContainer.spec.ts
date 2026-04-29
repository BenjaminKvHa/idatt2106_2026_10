import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AvatarAssetsContainer from '@/components/pupil/AvatarAssetsContainer.vue'

const assets = [
  {
    assetType: 'hair',
    name: 'brown-hair',
    filetype: 'png',
  },
  {
    assetType: 'eyes',
    name: 'blue-eyes',
    filetype: 'svg',
  },
]

function mountContainer(props = {}) {
  return mount(AvatarAssetsContainer, {
    props: {
      title: 'Hår',
      assets,
      selectedAssetName: null,
      ...props,
    },
  })
}

describe('AvatarAssetsContainer', () => {
  it('renders title', () => {
    const wrapper = mountContainer()

    expect(wrapper.text()).toContain('Hår')
  })

  it('renders all assets as buttons', () => {
    const wrapper = mountContainer()

    expect(wrapper.findAll('.asset-button')).toHaveLength(2)
  })

  it('renders correct image src and alt text', () => {
    const wrapper = mountContainer()

    const images = wrapper.findAll('img')

    const first = images[0]!
    const second = images[1]!

    expect(first.attributes('src')).toBe('/api/avatar-assets/hair/brown-hair.png')
    expect(first.attributes('alt')).toBe('brown-hair')

    expect(second.attributes('src')).toBe('/api/avatar-assets/eyes/blue-eyes.svg')
    expect(second.attributes('alt')).toBe('blue-eyes')
  })

  it('marks selected asset as active', () => {
    const wrapper = mountContainer({
      selectedAssetName: 'blue-eyes',
    })

    const buttons = wrapper.findAll('.asset-button')

    const first = buttons[0]!
    const second = buttons[1]!

    expect(first.classes()).not.toContain('active')
    expect(second.classes()).toContain('active')
  })

  it('emits selected asset when asset is clicked', async () => {
    const wrapper = mountContainer()

    const button = wrapper.findAll('.asset-button')[0]!

    await button.trigger('click')

    expect(wrapper.emitted('select')).toBeTruthy()
    expect(wrapper.emitted('select')?.[0]).toEqual([assets[0]])
  })

  it('renders no asset buttons when assets list is empty', () => {
    const wrapper = mountContainer({
      assets: [],
    })

    expect(wrapper.findAll('.asset-button')).toHaveLength(0)
  })
})
