import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PupilNewsTask from '@/components/pupil/task/PupilNewsTask.vue'

function mountTask(props = {}) {
  return mount(PupilNewsTask, {
    props: {
      taskDescription: 'Velg riktig bilde',
      fileIds: [101, 102],
      imageUrls: ['/test-image-1.png', '/test-image-2.png'],
      selectedFileId: null,
      result: null,
      ...props,
    },
    global: {
      stubs: {
        AuthenticatedImage: {
          props: ['src', 'alt'],
          template: '<img :src="src" :alt="alt" />',
        },
      },
    },
  })
}

describe('PupilNewsTask', () => {
  it('renders task description and images', () => {
    const wrapper = mountTask()

    expect(wrapper.text()).toContain('Velg riktig bilde')

    const images = wrapper.findAll('img')
    expect(images).toHaveLength(2)
    expect(images[0]?.attributes('src')).toBe('/test-image-1.png')
    expect(images[0]?.attributes('alt')).toBe('Bilde 1')
    expect(images[1]?.attributes('src')).toBe('/test-image-2.png')
    expect(images[1]?.attributes('alt')).toBe('Bilde 2')
  })

  it('shows placeholder text when an image url is missing', () => {
    const wrapper = mountTask({
      imageUrls: ['', '/test-image-2.png'],
    })

    expect(wrapper.text()).toContain('Laster…')
  })

  it('emits selected file when a picture button is clicked', async () => {
    const wrapper = mountTask()

    await wrapper.findAll('.fake-button')[1]!.trigger('click')

    expect(wrapper.emitted('selectFile')).toEqual([[102]])
  })

  it('marks the selected card as selected', () => {
    const wrapper = mountTask({
      selectedFileId: 101,
    })

    expect(wrapper.findAll('.picture-card')[0]?.classes()).toContain('selected')
  })

  it('shows answer label when selected file has a result', () => {
    const wrapper = mountTask({
      selectedFileId: 101,
      result: {
        correct: true,
        feedback: 'Riktig!',
      },
    })

    expect(wrapper.text()).toContain('Riktig svar')
  })

  it('shows confirm button when enabled and a file is selected', () => {
    const wrapper = mountTask({
      selectedFileId: 101,
      showConfirmButton: true,
      confirmButtonText: 'Bekreft valg',
    })

    expect(wrapper.find('.confirm-button').exists()).toBe(true)
    expect(wrapper.find('.confirm-button').text()).toBe('Bekreft valg')
  })

  it('emits submit when confirm button is clicked', async () => {
    const wrapper = mountTask({
      selectedFileId: 101,
      showConfirmButton: true,
    })

    await wrapper.find('.confirm-button').trigger('click')

    expect(wrapper.emitted('submit')).toBeTruthy()
  })
})
