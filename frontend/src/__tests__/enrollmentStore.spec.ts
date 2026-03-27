import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useEnrollmentStore, parseTags } from '@/stores/enrollmentStore'
import { courseApi, enrollmentApi, type Course } from '../api/index.ts'

vi.mock('../api/index.ts', () => ({
  courseApi: {
    getAll: vi.fn(),
  },
  enrollmentApi: {
    getMyCourses: vi.fn(),
    enroll: vi.fn(),
  },
}))

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

function buildCourse(overrides: Partial<Course> = {}): Course {
  return {
    id: 1,
    uuid: 'course-uuid',
    title: 'Intro to Java',
    code: 'COMP1000',
    description: null,
    link: null,
    tags: 'java,backend',
    material: null,
    dueDate: null,
    professorId: 7,
    createdAt: '2026-03-20T12:00:00Z',
    updatedAt: '2026-03-20T12:00:00Z',
    ...overrides,
  }
}

describe('useEnrollmentStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('parseTags trims commas and drops blank entries', () => {
    expect(parseTags(' java, spring , , testing ')).toEqual(['java', 'spring', 'testing'])
    expect(parseTags(null)).toEqual([])
  })

  it('fetchCourses populates courses and resets the loading flag', async () => {
    const store = useEnrollmentStore()
    const pending = deferred<{ data: Course[] }>()
    vi.mocked(courseApi.getAll).mockReturnValueOnce(pending.promise)

    const fetchPromise = store.fetchCourses({ tag: 'java' })

    expect(store.loading).toBe(true)
    pending.resolve({ data: [buildCourse()] })
    await fetchPromise

    expect(courseApi.getAll).toHaveBeenCalledWith({ tag: 'java' })
    expect(store.loading).toBe(false)
    expect(store.courses).toHaveLength(1)
    expect(store.courses[0].title).toBe('Intro to Java')
  })

  it('fetchMyEnrollments stores enrolled course uuids and updates the count', async () => {
    const store = useEnrollmentStore()
    vi.mocked(enrollmentApi.getMyCourses).mockResolvedValueOnce({
      data: [
        buildCourse({ uuid: 'course-a' }),
        buildCourse({ uuid: 'course-b' }),
      ],
    })

    await store.fetchMyEnrollments()

    expect(store.isEnrolled('course-a')).toBe(true)
    expect(store.isEnrolled('course-b')).toBe(true)
    expect(store.enrolledCount).toBe(2)
  })

  it('enroll adds the course uuid and clears the enrolling flag after success', async () => {
    const store = useEnrollmentStore()
    vi.mocked(enrollmentApi.enroll).mockResolvedValueOnce({ data: {} as never })

    await store.enroll('course-uuid')

    expect(enrollmentApi.enroll).toHaveBeenCalledWith('course-uuid')
    expect(store.isEnrolled('course-uuid')).toBe(true)
    expect(store.isEnrolling('course-uuid')).toBe(false)
  })

  it('enroll ignores duplicate in-flight requests for the same course', async () => {
    const store = useEnrollmentStore()
    const pending = deferred<{ data: unknown }>()
    vi.mocked(enrollmentApi.enroll).mockReturnValueOnce(pending.promise)

    const firstAttempt = store.enroll('course-uuid')
    const secondAttempt = store.enroll('course-uuid')

    expect(store.isEnrolling('course-uuid')).toBe(true)
    expect(enrollmentApi.enroll).toHaveBeenCalledTimes(1)

    pending.resolve({ data: {} })
    await Promise.all([firstAttempt, secondAttempt])

    expect(store.isEnrolled('course-uuid')).toBe(true)
    expect(store.isEnrolling('course-uuid')).toBe(false)
  })
})
