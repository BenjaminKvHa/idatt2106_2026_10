import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import PupilImageTask from '@/components/pupil/task/PupilImageTask.vue'

const categories = [
  { value: 'REAL', label: 'Ekte' },
  { value: 'AI_GENERATED', label: 'KI-generert' },
  { value: 'MANIPULATED', label: 'Manipulert' },
] as const

const justifications = ['Ser naturlig ut', 'Rare detaljer']

function mountTask(props = {}) {
  return mount(PupilImageTask, {
    props: {
      taskDescription: 'Er bildet ekte?',
      imageUrl: '/test-image.png',
      categories: [...categories],
      justifications,
      selectedCategory: null,
      selectedJustification: null,
      result: null,
      ...props,
    },
  })
}

describe('PupilImageTask', () => {
  it('renders task description and image', () => {
    const wrapper = mountTask()

    expect(wrapper.text()).toContain('Er bildet ekte?')

    const image = wrapper.find('img')
    expect(image.attributes('src')).toBe('/test-image.png')
    expect(image.attributes('alt')).toBe('Oppgavebilde')
  })

  it('shows placeholder when image url is missing', () => {
    const wrapper = mountTask({
      imageUrl: '',
    })

    expect(wrapper.text()).toContain('Laster bilde')
  })

  it('renders all category buttons', () => {
    const wrapper = mountTask()

    expect(wrapper.text()).toContain('Ekte')
    expect(wrapper.text()).toContain('KI-generert')
    expect(wrapper.text()).toContain('Manipulert')
  })

  it('emits selected category when category is clicked', async () => {
    const wrapper = mountTask()

    await wrapper.findAll('.choices button')[0]!.trigger('click')

    expect(wrapper.emitted('selectCategory')).toEqual([['REAL']])
  })

  it('shows justifications when category is selected', () => {
    const wrapper = mountTask({
      selectedCategory: 'REAL',
    })

    expect(wrapper.text()).toContain('Hvorfor?')
    expect(wrapper.text()).toContain('Ser naturlig ut')
    expect(wrapper.text()).toContain('Rare detaljer')
  })

  it('emits selected justification when justification is clicked', async () => {
    const wrapper = mountTask({
      selectedCategory: 'REAL',
    })

    await wrapper.findAll('.reason-box button')[0]!.trigger('click')

    expect(wrapper.emitted('selectJustification')).toEqual([['Ser naturlig ut']])
  })

  it('marks selected category as selected', () => {
    const wrapper = mountTask({
      selectedCategory: 'REAL',
    })

    const firstButton = wrapper.findAll('.choices button')[0]!

    expect(firstButton.classes()).toContain('selected')
  })

  it('marks selected justification as selected', () => {
    const wrapper = mountTask({
      selectedCategory: 'REAL',
      selectedJustification: 'Ser naturlig ut',
    })

    const firstButton = wrapper.findAll('.reason-box button')[0]!

    expect(firstButton.classes()).toContain('selected')
  })

  it('disables answer buttons when result exists', () => {
    const wrapper = mountTask({
      selectedCategory: 'REAL',
      selectedJustification: 'Ser naturlig ut',
      result: {
        correct: true,
        feedback: 'Riktig!',
      },
    })

    const buttons = wrapper.findAll('button')

    buttons.forEach((button) => {
      expect(button.attributes('disabled')).toBeDefined()
    })
  })

  it('shows submit button when showSubmitButton is true', () => {
    const wrapper = mountTask({
      showSubmitButton: true,
      submitText: 'Send svar',
    })

    expect(wrapper.find('.submit-button').exists()).toBe(true)
    expect(wrapper.find('.submit-button').text()).toBe('Send svar')
  })

  it('emits submit when submit button is clicked', async () => {
    const wrapper = mountTask({
      showSubmitButton: true,
    })

    await wrapper.find('.submit-button').trigger('click')

    expect(wrapper.emitted('submit')).toBeTruthy()
  })
})
