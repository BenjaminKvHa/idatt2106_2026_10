import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/shared/HomeView.vue'
import NotFoundView from '@/views/shared/NotFoundView.vue'
import LeaderboardView from '@/views/shared/LeaderboardView.vue'
import EnterClassroomCodeView from '@/views/shared/EnterClassroomCodeView.vue'
import CreateAvatarView from '@/views/shared/CreateAvatarView.vue'
import TeachersProfileView from '@/views/teacher/TeachersProfileView.vue'
import TeachersNotificationsView from '@/views/teacher/TeachersNotificationsView.vue'
import TeachersClassroomsView from '@/views/teacher/TeachersClassroomsView.vue'
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
import PupilsMapView from '@/views/pupil/PupilsMapView.vue'
import SignInView from '@/views/shared/SignInView.vue'
import SignUpView from '@/views/shared/SignUpView.vue'


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
      ],
    },
    {
      path: '/teacher',
      children: [
        {
          path: 'profile',
          component: TeachersProfileView,
        },
        {
          path: 'notifications',
          component: TeachersNotificationsView,
        },
        {
          path: 'classrooms',
          children: [
            {
              path: '',
              component: TeachersClassroomsView,
            },
            {
              path: ':classroom_id',
              component: TeachersClassroomView,
              children: [
                // {
                //   path: 'censorship',
                //   component: HomeView,
                // },
                {
                  path: 'pupils',
                  component: TeachersPupilsView,
                  children: [
                    {
                      path: ':pupil_id',
                      children: [
                        {
                          path: '',
                          component: TeachersPupilView,
                        },
                        {
                          path: 'notbook',
                          children: [
                            {
                              path: ':chapter_id',
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
          path: ''
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
              path: ''
            },
            {
              path: 'notebook',
              children: [
                {
                  component: PupilsNotebookView,
                  path: ''
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
