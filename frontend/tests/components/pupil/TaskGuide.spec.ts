import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TaskGuide from '@/components/pupil/TaskGuide.vue'

describe('TaskGuide', () => {
  it('renders slot content', () => {
    const wrapper = mount(TaskGuide, {
      slots: {
        default: '<h2>Velkommen!</h2><p>Klar for oppgaven?</p>',
      },
    })

    expect(wrapper.text()).toContain('Velkommen!')
    expect(wrapper.text()).toContain('Klar for oppgaven?')
  })

  it('shows default button text', () => {
    const wrapper = mount(TaskGuide)

    expect(wrapper.find('.task-guide-button').text()).toBe('Jeg er klar!')
  })

  it('shows custom button text', () => {
    const wrapper = mount(TaskGuide, {
      props: {
        buttonText: 'Start',
      },
    })

    expect(wrapper.find('.task-guide-button').text()).toBe('Start')
  })

  it('emits close when confirm button is clicked', async () => {
    const wrapper = mount(TaskGuide)

    await wrapper.find('.task-guide-button').trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })

  it('renders overlay and guide container', () => {
    const wrapper = mount(TaskGuide)

    expect(wrapper.find('.overlay').exists()).toBe(true)
    expect(wrapper.find('.center-container').exists()).toBe(true)
    expect(wrapper.find('.guide-text').exists()).toBe(true)
  })
})
