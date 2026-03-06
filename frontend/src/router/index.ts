import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import HelloBackend from '../components/HelloBackend.vue'
import RegisterView from '@/views/RegisterView.vue'
import LoginView from '@/views/LoginView.vue'
import DashboardView from '@/views/DashboardView.vue'
import StudentDashboardView from '@/views/StudentDashboardView.vue'
import ProfessorCreateCourseView from '@/views/ProfessorCreateCourseView.vue'
import { useAuthStore } from '@/stores/authStore'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'HomeView',
    component: () => import('../views/HomeView.vue'),
  },
  {
    path: '/hello',
    name: 'HelloBackend',
    component: HelloBackend,
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { guestOnly: true },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { guestOnly: true },
  },
  {
    path: '/professor/dashboard/:userId?',
    name: 'professor-dashboard',
    component: DashboardView,
    meta: { requiresAuth: true, requiredRole: 'PROFESSOR' },
  },
  {
    path: '/professor/courses/new',
    name: 'professor-course-create',
    component: ProfessorCreateCourseView,
    meta: { requiresAuth: true, requiredRole: 'PROFESSOR' },
  },
  {
    path: '/professor/courses/:uuid/edit',
    name: 'professor-course-edit',
    component: ProfessorCreateCourseView,
    meta: { requiresAuth: true, requiredRole: 'PROFESSOR' },
  },
  {
    path: '/student/dashboard/:userId?',
    name: 'student-dashboard',
    component: StudentDashboardView,
    meta: { requiresAuth: true, requiredRole: 'STUDENT' },
  },
  {
    path: '/dashboard/:userId?',
    name: 'dashboard-legacy',
    meta: { requiresAuth: true },
    beforeEnter: () => {
      const authStore = useAuthStore()
      return authStore.defaultDashboardPath
    },
    component: StudentDashboardView,
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const mustValidateSession =
    Boolean(to.meta.requiresAuth) ||
    Boolean(to.meta.guestOnly) ||
    to.path.startsWith('/dashboard')

  await authStore.fetchSession(mustValidateSession)

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return authStore.defaultDashboardPath
  }

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  const requiredRole = to.meta.requiredRole as 'STUDENT' | 'PROFESSOR' | undefined
  if (requiredRole && authStore.session?.role !== requiredRole) {
    return authStore.defaultDashboardPath
  }

  return true
})

export default router
