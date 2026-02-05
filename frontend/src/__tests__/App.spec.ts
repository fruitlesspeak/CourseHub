import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import App from '../App.vue'

describe('App', () => {
  it('mounts and renders router-view', () => {
    const wrapper = mount(App, {
      global: {
        stubs: ['router-view']
      }
    })
    expect(wrapper.findComponent({ name: 'router-view' }).exists()).toBe(true)
  })
})
