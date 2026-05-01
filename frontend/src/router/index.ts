import { createRouter, createWebHistory, type RouteLocationNormalizedLoaded } from 'vue-router'
import HomeView from '../views/shared/HomeView.vue'
import NotFoundView from '@/views/shared/NotFoundView.vue'
import LeaderboardView from '@/views/shared/LeaderboardView.vue'
import EnterClassroomCodeView from '@/views/shared/EnterClassroomCodeView.vue'
import TeacherView from '@/views/teacher/TeacherView.vue'
import CreateAvatarView from '@/views/shared/SetAvatarView.vue'
import TeachersClassroomsView from '@/views/teacher/TeachersClassroomsView.vue'
import TeachersCreateClassroomView from '@/views/teacher/TeachersCreateClassroomView.vue'
import TeachersPupilsView from '@/views/teacher/TeachersPupilsView.vue'
import TeachersLeaderboardView from '@/views/teacher/TeachersLeaderboardView.vue'
import TeachersEditTasksView from '@/views/teacher/TeachersEditTasksView.vue'
import TeachersMysteryView from '@/views/teacher/TeachersMysteryView.vue'
import PupilView from '@/views/pupil/PupilView.vue'
import PupilsNotebookView from '@/views/pupil/PupilsNotebookView.vue'
import PupilsMysteryView from '@/views/pupil/PupilsMysteryView.vue'
import PupilsMedalsView from '@/views/pupil/PupilsMedalsView.vue'
import PupilsHelpView from '@/views/pupil/PupilsHelpView.vue'
import PupilsMapView from '@/views/pupil/PupilsMapView.vue'
import SignInView from '@/views/shared/SignInView.vue'
import SignUpView from '@/views/shared/SignUpView.vue'
import NewsView from '@/views/pupil/map_locations/NewsView.vue'
import DatacenterView from '@/views/pupil/map_locations/DatacenterView.vue'
import PhotographerView from '@/views/pupil/map_locations/PhotographerView.vue'
import PupilsWaitingPageView from '@/views/pupil/PupilsWaitingPageView.vue'
import PupilsWelcomeGuideView from '@/views/pupil/PupilsWelcomeGuideView.vue'
import PupilsProfileView from '@/views/pupil/PupilsProfileView.vue'
import PupilsEditAvatarView from '@/views/pupil/PupilsEditAvatarView.vue'
import TeachersEditClassroom from '@/views/teacher/TeachersEditClassroom.vue'
import { getJwtDataWithRefresh } from '@/utils/jwtUtils'
import { clearMeCache, fetchMeCached, getPupilMembershipRoute } from '@/utils/authRedirect'

export { clearMeCache } from '@/utils/authRedirect'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/sign-in',
      component: SignInView,
    },
    {
      path: '/sign-up',
      component: SignUpView,
    },
    {
      path: '/join-classroom',
      children: [
        {
          path: 'enter-classroom-code',
          component: EnterClassroomCodeView,
        },
        {
          path: 'set-avatar',
          component: CreateAvatarView,
        },
        {
          path: 'waiting-classroom',
          component: PupilsWaitingPageView,
        },
      ],
    },
    {
      path: '/teacher',
      component: TeacherView,
      meta: { breadcrumb: '' },
      children: [
        {
          path: 'classrooms',
          meta: { breadcrumb: 'Klasserom' },
          children: [
            {
              path: '',
              component: TeachersClassroomsView,
            },
            {
              path: 'new',
              meta: { breadcrumb: 'Nytt Klasserom' },
              component: TeachersCreateClassroomView,
            },
            {
              path: ':classroomCode',
              meta: {
                breadcrumb: (route: RouteLocationNormalizedLoaded) =>
                  String(route.params.classroomCode)
                    .replace(/-/g, ' ')
                    .replace(/\b\w/g, (c) => c.toUpperCase()),
                tabs: (route: RouteLocationNormalizedLoaded) => {
                  const base = `/teacher/classrooms/${route.params.classroomCode}`
                  return [
                    { label: 'Ledertavle', to: `${base}/leaderboard` },
                    { label: 'Elever', to: `${base}/pupils` },
                    { label: 'Rediger klasserom', to: `${base}/edit-classroom` },
                    { label: 'Rediger oppgaver', to: `${base}/edit-tasks` },
                    { label: 'Ukens mysterium', to: `${base}/mystery` },
                  ]
                },
              },
              redirect: (to) => `/teacher/classrooms/${to.params.classroomCode}/leaderboard`,
              children: [
                {
                  path: 'edit-classroom',
                  meta: { breadcrumb: 'Rediger Klasserom' },
                  component: TeachersEditClassroom,
                },
                {
                  path: 'pupils',
                  meta: { breadcrumb: 'Elever' },
                  component: TeachersPupilsView,
                },
                {
                  path: 'leaderboard',
                  meta: { breadcrumb: 'Ledertavle' },
                  component: TeachersLeaderboardView,
                },
                {
                  path: 'edit-tasks',
                  meta: { breadcrumb: 'Rediger oppgaver' },
                  component: TeachersEditTasksView,
                },
                {
                  path: 'mystery',
                  meta: { breadcrumb: 'Ukens mysterium' },
                  component: TeachersMysteryView,
                },
              ],
            },
          ],
        },
      ],
    },
    {
      path: '/pupil',
      children: [
        {
          component: PupilView,
          path: '',
        },
        {
          path: 'profile',
          component: PupilsProfileView,
        },
        {
          path: 'profile/avatar',
          component: PupilsEditAvatarView,
        },
        {
          path: 'welcome-guide',
          component: PupilsWelcomeGuideView,
        },
        {
          path: 'classroom',
          children: [
            {
              path: 'leaderboard',
              component: LeaderboardView,
            },
            {
              path: 'notebook',
              children: [
                {
                  component: PupilsNotebookView,
                  path: '',
                },
              ],
            },
            {
              path: 'mystery',
              component: PupilsMysteryView,
            },
            {
              path: 'medals',
              component: PupilsMedalsView,
            },
            {
              path: 'map',
              component: PupilsMapView,
            },
            {
              path: 'map/news',
              component: NewsView,
            },
            {
              path: 'map/datacenter',
              component: DatacenterView,
            },
            {
              path: 'map/photographer',
              component: PhotographerView,
            },
            {
              path: 'help',
              component: PupilsHelpView,
            },
            {
              path: 'leaderboard',
              component: LeaderboardView,
            },
          ],
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView,
    },
  ],
})

router.beforeEach(async (to) => {
  const requiresPupil = to.path.startsWith('/pupil') || to.path.startsWith('/join-classroom')
  const requiresTeacher = to.path.startsWith('/teacher')

  if (!requiresPupil && !requiresTeacher) return true

  const jwt = await getJwtDataWithRefresh()
  if (!jwt?.role) return '/sign-in'

  if (requiresTeacher && jwt.role !== 'TEACHER') return '/pupil'
  if (requiresPupil && jwt.role !== 'PUPIL') return '/teacher/classrooms'

  if (jwt.role === 'PUPIL') {
    const me = await fetchMeCached({ force: true })
    if (!me) return '/sign-in'

    const pupilRoute = getPupilMembershipRoute(me)

    if (to.path.startsWith('/pupil') && pupilRoute !== '/pupil' && to.path !== pupilRoute) {
      return pupilRoute
    }

    if (to.path.startsWith('/join-classroom')) {
      const isSetAvatarRoute = to.path.startsWith('/join-classroom/set-avatar')

      if (pupilRoute.startsWith('/pupil')) {
        return pupilRoute
      }

      if (pupilRoute === '/join-classroom/enter-classroom-code' && isSetAvatarRoute) {
        return '/join-classroom/enter-classroom-code'
      }

      if (
        pupilRoute === '/join-classroom/waiting-classroom' &&
        !to.path.startsWith('/join-classroom/waiting-classroom') &&
        !isSetAvatarRoute
      ) {
        return '/join-classroom/waiting-classroom'
      }

      if (
        pupilRoute === '/join-classroom/enter-classroom-code' &&
        to.path.startsWith('/join-classroom/waiting-classroom')
      ) {
        return '/join-classroom/enter-classroom-code'
      }
    }
  }

  return true
})

export default router
