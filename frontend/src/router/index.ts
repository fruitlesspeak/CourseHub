import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import HelloBackend from '../components/HelloBackend.vue'

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
    path: '/login',
    name: 'LoginView',
    component: () => import('../views/LoginView.vue'),
  },
  {
    path: '/dashboard/:userId?',
    name: 'DashboardView',
    component: () => import('../views/DashboardView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
