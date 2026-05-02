import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TeacherView from '@/views/teacher/TeacherView.vue'

function mountTeacherView() {
  return mount(TeacherView, {
    global: {
      stubs: {
        TeacherMenu: {
          template: '<nav data-test="teacher-menu" />',
        },
        TeacherTopbar: {
          template: '<header data-test="teacher-topbar" />',
        },
        RouterView: {
          template: '<section data-test="teacher-router-view" />',
        },
      },
    },
  })
}

describe('TeacherView', () => {
  it('renders the teacher sidebar, topbar, and routed content area', () => {
    const wrapper = mountTeacherView()

    expect(wrapper.find('.teacher-page').exists()).toBe(true)
    expect(wrapper.find('.teacher-sidebar').exists()).toBe(true)
    expect(wrapper.find('.teacher-main').exists()).toBe(true)
    expect(wrapper.find('.teacher-content').exists()).toBe(true)
    expect(wrapper.find('[data-test="teacher-menu"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="teacher-topbar"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="teacher-router-view"]').exists()).toBe(true)
  })
})
