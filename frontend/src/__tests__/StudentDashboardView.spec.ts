import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import StudentDashboardView from '@/views/StudentDashboardView.vue'
import { enrollmentApi, importantDateApi, type Course, type ImportantDate } from '@/api'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'

const { routerPush } = vi.hoisted(() => ({
  routerPush: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: routerPush,
  }),
}))

vi.mock('@/api', () => ({
  enrollmentApi: {
    getMyCourses: vi.fn(),
    drop: vi.fn(),
  },
  importantDateApi: {
    getAll: vi.fn(),
  },
}))

vi.mock('@/components/dashboard/DashboardLayout.vue', () => ({
  default: {
    name: 'DashboardLayoutStub',
    props: {
      title: { type: String, required: true },
      primaryActionText: { type: String, required: true },
    },
    emits: ['primaryAction'],
    template: `
      <div>
        <h1>{{ title }}</h1>
        <button data-test="primary-action" @click="$emit('primaryAction')">{{ primaryActionText }}</button>
        <slot name="content" />
      </div>
    `,
  },
}))

function buildAxiosResponse<T>(data: T): AxiosResponse<T> {
  return {
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config: { headers: {} } as InternalAxiosRequestConfig,
  }
}

function buildCourse(overrides: Partial<Course> = {}): Course {
  return {
    id: 1,
    uuid: 'course-uuid',
    title: 'Distributed Systems',
    code: 'COMP4350',
    description: 'Advanced distributed systems topics',
    link: null,
    tags: 'systems,distributed',
    material: 'Week 1 notes',
    dueDate: null,
    professorId: 7,
    createdAt: '2026-03-20T12:00:00Z',
    updatedAt: '2026-03-20T12:00:00Z',
    ...overrides,
  }
}

function buildImportantDate(overrides: Partial<ImportantDate> = {}): ImportantDate {
  return {
    id: 1,
    courseId: 1,
    createdByUserId: 7,
    title: 'Midterm Review',
    description: 'Bring your notes',
    dueAt: '2099-04-01T10:00:00Z',
    createdAt: '2026-03-20T12:00:00Z',
    updatedAt: '2026-03-20T12:00:00Z',
    ...overrides,
  }
}

async function renderDashboard() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(StudentDashboardView, {
    global: {
      plugins: [pinia],
    },
  })

  await flushPromises()
  await flushPromises()

  return wrapper
}

describe('StudentDashboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('confirm', vi.fn(() => true))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.restoreAllMocks()
  })

  it('loads enrolled courses and important dates on mount and renders summary counts', async () => {
    vi.mocked(enrollmentApi.getMyCourses).mockResolvedValueOnce(
      buildAxiosResponse([buildCourse()]),
    )
    vi.mocked(importantDateApi.getAll).mockResolvedValueOnce(
      buildAxiosResponse([buildImportantDate()]),
    )

    const wrapper = await renderDashboard()

    expect(enrollmentApi.getMyCourses).toHaveBeenCalledTimes(1)
    expect(importantDateApi.getAll).toHaveBeenCalledWith({ courseId: 1 })
    expect(wrapper.find('h1').text()).toBe('My Dashboard')
    expect(wrapper.findAll('.summary-value').map((node) => node.text())).toEqual(['1', '1', '1'])
    expect(wrapper.text()).toContain('Distributed Systems')
    expect(wrapper.text()).toContain('Midterm Review')
  })

  it('renders the empty state when the student has no enrolled courses', async () => {
    vi.mocked(enrollmentApi.getMyCourses).mockResolvedValueOnce(
      buildAxiosResponse([]),
    )

    const wrapper = await renderDashboard()

    expect(importantDateApi.getAll).not.toHaveBeenCalled()
    expect(wrapper.findAll('.summary-value').map((node) => node.text())).toEqual(['0', '0', '0'])
    expect(wrapper.text()).toContain('No enrolled courses yet.')
    expect(wrapper.text()).toContain('No upcoming dates.')
  })

  it('drops a course and refreshes the dashboard state', async () => {
    vi.mocked(enrollmentApi.getMyCourses).mockResolvedValueOnce(
      buildAxiosResponse([buildCourse()]),
    )
    vi.mocked(importantDateApi.getAll).mockResolvedValueOnce(
      buildAxiosResponse([buildImportantDate()]),
    )
    vi.mocked(enrollmentApi.drop).mockResolvedValueOnce(buildAxiosResponse(undefined))

    const wrapper = await renderDashboard()

    await wrapper.find('button.course-btn.manage').trigger('click')
    await flushPromises()
    await wrapper.find('button.course-menu-item.drop').trigger('click')
    await flushPromises()

    expect(enrollmentApi.drop).toHaveBeenCalledWith('course-uuid')
    expect(wrapper.findAll('.summary-value').map((node) => node.text())).toEqual(['0', '0', '0'])
    expect(wrapper.text()).toContain('No enrolled courses yet.')
  })

  it('routes to course detail with the dashboard query flag', async () => {
    vi.mocked(enrollmentApi.getMyCourses).mockResolvedValueOnce(
      buildAxiosResponse([buildCourse()]),
    )
    vi.mocked(importantDateApi.getAll).mockResolvedValueOnce(
      buildAxiosResponse([]),
    )

    const wrapper = await renderDashboard()

    await wrapper.find('button.course-btn.manage').trigger('click')
    await flushPromises()
    await wrapper.findAll('button.course-menu-item')[0]!.trigger('click')

    expect(routerPush).toHaveBeenCalledWith({
      name: 'CourseDetail',
      params: { uuid: 'course-uuid' },
      query: { from: 'dashboard' },
    })
  })
})
