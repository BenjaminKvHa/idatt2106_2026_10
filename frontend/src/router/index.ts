import { createRouter, createWebHistory, type RouteLocationNormalizedLoaded } from 'vue-router'
import HomeView from '../views/shared/HomeView.vue'
import NotFoundView from '@/views/shared/NotFoundView.vue'
import LeaderboardView from '@/views/shared/LeaderboardView.vue'
import EnterClassroomCodeView from '@/views/shared/EnterClassroomCodeView.vue'
import CreateAvatarView from '@/views/shared/CreateAvatarView.vue'
import TeacherView from '@/views/teacher/TeacherView.vue'
import TeachersProfileView from '@/views/teacher/TeachersProfileView.vue'
import TeachersNotificationsView from '@/views/teacher/TeachersNotificationsView.vue'
import TeachersClassroomsView from '@/views/teacher/TeachersClassroomsView.vue'
import TeachersCreateClassroomView from '@/views/teacher/TeachersCreateClassroomView.vue'
import TeachersClassroomView from '@/views/teacher/TeachersClassroomView.vue'
import TeachersPupilsView from '@/views/teacher/TeachersPupilsView.vue'
import TeachersPupilView from '@/views/teacher/TeachersPupilView.vue'
import TeachersNotebookChapterView from '@/views/teacher/TeachersNotebookChapterView.vue'
import PupilView from '@/views/pupil/PupilView.vue'
import PupilsProfileView from '@/views/pupil/PupilsProfileView.vue'
import PupilsClassroomView from '@/views/pupil/PupilsClassroomView.vue'
import PupilsNotebookView from '@/views/pupil/PupilsNotebookView.vue'
import PupilsNotebookChapterView from '@/views/pupil/PupilsNotebookChapterView.vue'
import PupilsMysteryView from '@/views/pupil/PupilsMysteryView.vue'
import PupilsMedalsView from '@/views/pupil/PupilsMedalsView.vue'
import PupilsSettingsView from '@/views/pupil/PupilsSettingsView.vue'
import PupilsHelpView from '@/views/pupil/PupilsHelpView.vue'
import PupilsLeaderboardView from '@/views/pupil/PupilsLeaderboardView.vue'
import PupilsMapView from '@/views/pupil/PupilsMapView.vue'
import SignInView from '@/views/shared/SignInView.vue'
import SignUpView from '@/views/shared/SignUpView.vue'
import NewsView from '@/views/pupil/map_locations/NewsView.vue'
import DatacenterView from '@/views/pupil/map_locations/DatacenterView.vue'
import PostofficeView from '@/views/pupil/map_locations/PostofficeView.vue'
import PhotographerView from '@/views/pupil/map_locations/PhotographerView.vue'
import PupilsWelcomePageView from '@/views/pupil/PupilsWelcomePageView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/leaderboard',
      component: LeaderboardView,
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
          path: 'create-avatar',
          component: CreateAvatarView,
        },
        {
          path: 'welcome-classroom',
          component: PupilsWelcomePageView,
        },
      ],
    },
    {
      path: '/teacher',
      component: TeacherView,
      meta: { isTeacherRoute: true, breadcrumb: '' },
      children: [
        {
          path: 'profile',
          meta: { isTeacherRoute: true },
          component: TeachersProfileView,
        },
        {
          path: 'notifications',
          meta: { isTeacherRoute: true },
          component: TeachersNotificationsView,
        },
        {
          path: 'classrooms',
          meta: { isTeacherRoute: true, breadcrumb: 'Klasserom' },
          children: [
            {
              path: '',
              meta: { isTeacherRoute: true },
              component: TeachersClassroomsView,
            },
            {
              path: 'new',
              meta: { isTeacherRoute: true, breadcrumb: 'Nytt Klasserom' },
              component: TeachersCreateClassroomView,
            },
            {
              path: ':classroomName',
              meta: {
                isTeacherRoute: true,
                breadcrumb: (route: RouteLocationNormalizedLoaded) =>
                  String(route.params.classroomName)
                    .replace(/-/g, ' ')
                    .replace(/\b\w/g, (c) => c.toUpperCase()),
                tabs: (route: RouteLocationNormalizedLoaded) => {
                  const base = `/teacher/classrooms/${route.params.classroomName}`
                  return [
                    { label: 'Oversikt', to: `${base}/oversikt` },
                    { label: 'Elever', to: `${base}/pupils` },
                    { label: 'Innstillinger', to: `${base}/edit-classroom` },
                  ]
                },
              },
              redirect: (to) => `/teacher/classrooms/${to.params.classroomName}/oversikt`,
              children: [
                {
                  path: 'oversikt',
                  meta: { isTeacherRoute: true, breadcrumb: 'Oversikt' },
                  component: TeachersClassroomView,
                },
                {
                  path: 'edit-classroom',
                  meta: { isTeacherRoute: true, breadcrumb: 'Rediger Klasserom' },
                  component: TeachersClassroomView,
                },
                {
                  path: 'pupils',
                  meta: { isTeacherRoute: true, breadcrumb: 'Elever' },
                  component: TeachersPupilsView,
                  children: [
                    {
                      path: ':pupil_id',
                      meta: { isTeacherRoute: true },
                      children: [
                        {
                          path: '',
                          meta: { isTeacherRoute: true },
                          component: TeachersPupilView,
                        },
                        {
                          path: 'notebook',
                          meta: { isTeacherRoute: true },
                          children: [
                            {
                              path: ':chapter_id',
                              meta: { isTeacherRoute: true },
                              component: TeachersNotebookChapterView,
                            },
                          ],
                        },
                      ],
                    },
                  ],
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
          path: 'classroom',
          children: [
            {
              component: PupilsClassroomView,
              path: '',
            },
            {
              path: 'notebook',
              children: [
                {
                  component: PupilsNotebookView,
                  path: '',
                },
                {
                  path: ':chapter_id',
                  component: PupilsNotebookChapterView,
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
              path: 'map/postoffice',
              component: PostofficeView,
            },
            {
              path: 'map/photographer',
              component: PhotographerView,
            },
            {
              path: 'settings',
              component: PupilsSettingsView,
            },
            {
              path: 'help',
              component: PupilsHelpView,
            },
            {
              path: 'leaderboard',
              component: PupilsLeaderboardView,
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

export default router
