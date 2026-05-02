import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import LeaderboardOfPupils from '@/components/ui/LeaderboardOfPupils.vue'

describe('LeaderboardOfPupils', () => {
  it('renders legal name under username for leaderboard entries', () => {
    const wrapper = mount(LeaderboardOfPupils, {
      props: {
        entries: [
          {
            username: 'komplett.oskar',
            legalName: 'Oskar Olsen',
            totalXp: 420,
          },
        ],
      },
    })

    expect(wrapper.find('.username').text()).toBe('komplett.oskar')
    expect(wrapper.find('.legal-name').text()).toBe('Oskar Olsen')
    expect(wrapper.find('.xp').text()).toBe('420 XP')
  })

  it('uses the username fallback when an entry has no username', () => {
    const wrapper = mount(LeaderboardOfPupils, {
      props: {
        entries: [
          {
            username: null,
            legalName: 'Maja Test',
            totalXp: 0,
          },
        ],
      },
    })

    expect(wrapper.find('.username').text()).toBe('Uten kallenavn')
    expect(wrapper.find('.legal-name').text()).toBe('Maja Test')
  })
})
