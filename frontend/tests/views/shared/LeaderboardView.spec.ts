import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import LeaderboardView from '@/views/shared/LeaderboardView.vue'
import { get } from '@/utils/apiUtils'

vi.mock('@/utils/apiUtils', () => ({
  get: vi.fn(),
}))

const mockedGet = vi.mocked(get)

function mountLeaderboardView() {
  return mount(LeaderboardView, {
    global: {
      stubs: {
        TopBar: true,
      },
    },
  })
}

describe('LeaderboardView', () => {
  beforeEach(() => {
    mockedGet.mockReset()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the loading state before leaderboard data resolves', () => {
    mockedGet.mockReturnValue(new Promise(() => undefined))

    const wrapper = mountLeaderboardView()

    expect(wrapper.text()).toContain('Laster ledertavle...')
  })

  it('fetches classroom leaderboard entries from the backend endpoint', async () => {
    mockedGet
      .mockResolvedValueOnce([{ classroomCode: 'ABCABC', classroomName: 'A-klassen', totalXp: 90 }])
      .mockResolvedValueOnce({ classroomCode: 'ABCABC' })

    mountLeaderboardView()

    await flushPromises()

    expect(mockedGet).toHaveBeenCalledWith('/api/leaderboards/classrooms')
    expect(mockedGet).toHaveBeenCalledWith('/api/me')
  })

  it('renders classroom entries sorted by XP descending', async () => {
    mockedGet
      .mockResolvedValueOnce([
        { classroomCode: 'LOWLOW', classroomName: 'Lav score', totalXp: 30 },
        { classroomCode: 'TOPTOP', classroomName: 'Høy score', totalXp: 180 },
        { classroomCode: 'MIDMID', classroomName: 'Midt score', totalXp: 90 },
      ])
      .mockResolvedValueOnce({ classroomCode: null })

    const wrapper = mountLeaderboardView()

    await flushPromises()

    const entries = wrapper.findAll('li')
    expect(entries.map((entry) => entry.find('.classroom-name').text())).toEqual([
      'Høy score',
      'Midt score',
      'Lav score',
    ])
    expect(entries.map((entry) => entry.find('.xp-value').text())).toEqual([
      '180 XP',
      '90 XP',
      '30 XP',
    ])
  })

  it('shows five classrooms per page', async () => {
    mockedGet
      .mockResolvedValueOnce([
        { classroomCode: 'CLASSA', classroomName: 'Klasse A', totalXp: 60 },
        { classroomCode: 'CLASSB', classroomName: 'Klasse B', totalXp: 50 },
        { classroomCode: 'CLASSC', classroomName: 'Klasse C', totalXp: 40 },
        { classroomCode: 'CLASSD', classroomName: 'Klasse D', totalXp: 30 },
        { classroomCode: 'CLASSE', classroomName: 'Klasse E', totalXp: 20 },
        { classroomCode: 'CLASSF', classroomName: 'Klasse F', totalXp: 10 },
      ])
      .mockResolvedValueOnce({ classroomCode: null })

    const wrapper = mountLeaderboardView()

    await flushPromises()

    expect(wrapper.findAll('li')).toHaveLength(5)
  })

  it('pins the current classroom with its global position at the bottom', async () => {
    mockedGet
      .mockResolvedValueOnce([
        { classroomCode: 'TOPTOP', classroomName: 'Toppklasse', totalXp: 300 },
        { classroomCode: 'MYCLAS', classroomName: 'Min klasse', totalXp: 120 },
        { classroomCode: 'LOWLOW', classroomName: 'Lav klasse', totalXp: 30 },
      ])
      .mockResolvedValueOnce({ classroomCode: 'MYCLAS' })

    const wrapper = mountLeaderboardView()

    await flushPromises()

    const currentClassroom = wrapper.find('.current-classroom-summary')
    expect(currentClassroom.text()).toContain('Din kl.')
    expect(currentClassroom.text()).toContain('2.')
    expect(currentClassroom.text()).toContain('Min klasse')
    expect(currentClassroom.text()).toContain('120 XP')
  })

  it('shows the empty state when the backend returns no classrooms', async () => {
    mockedGet.mockResolvedValue([])

    const wrapper = mountLeaderboardView()

    await flushPromises()

    expect(wrapper.findAll('li')).toHaveLength(0)
    expect(wrapper.text()).toContain('Ingen klasserom funnet...')
  })

  it('shows an error when the backend request fails', async () => {
    mockedGet.mockRejectedValue(new Error('Leaderboard unavailable'))

    const wrapper = mountLeaderboardView()

    await flushPromises()

    expect(wrapper.text()).toContain('Leaderboard unavailable')
    expect(wrapper.findAll('li')).toHaveLength(0)
  })

  it('shows an error when the leaderboard response is not an array', async () => {
    mockedGet.mockResolvedValue({ classroomCode: 'ABCABC', classroomName: 'Wrong shape', totalXp: 10 })

    const wrapper = mountLeaderboardView()

    await flushPromises()

    expect(wrapper.text()).toContain('Unexpected leaderboard response format')
    expect(wrapper.findAll('li')).toHaveLength(0)
  })
})
