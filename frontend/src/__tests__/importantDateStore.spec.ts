import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useImportantDateStore } from '@/stores/importantDateStore'
import { importantDateApi, type ImportantDate } from '@/api'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'

vi.mock('@/api', () => ({
  importantDateApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
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

function buildImportantDate(overrides: Partial<ImportantDate> = {}): ImportantDate {
  return {
    id: 1,
    courseId: 10,
    createdByUserId: 7,
    title: 'Midterm Review',
    description: 'Bring your notes',
    dueAt: '2099-04-01T10:00:00Z',
    createdAt: '2026-03-20T12:00:00Z',
    updatedAt: '2026-03-20T12:00:00Z',
    ...overrides,
  }
}

describe('useImportantDateStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchByCourse populates important dates and resets loading', async () => {
    const store = useImportantDateStore()
    vi.mocked(importantDateApi.getAll).mockResolvedValueOnce(
      buildAxiosResponse([buildImportantDate()]),
    )

    await store.fetchByCourse(10)

    expect(importantDateApi.getAll).toHaveBeenCalledWith({ courseId: 10 })
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
    expect(store.importantDates).toHaveLength(1)
    expect(store.importantDates[0]?.title).toBe('Midterm Review')
  })

  it('fetchByCourses aggregates important dates across courses', async () => {
    const store = useImportantDateStore()
    vi.mocked(importantDateApi.getAll)
      .mockResolvedValueOnce(buildAxiosResponse([
        buildImportantDate({ id: 1, courseId: 10, title: 'Course 10 Date' }),
      ]))
      .mockResolvedValueOnce(buildAxiosResponse([
        buildImportantDate({ id: 2, courseId: 11, title: 'Course 11 Date' }),
      ]))

    await store.fetchByCourses([10, 11])

    expect(importantDateApi.getAll).toHaveBeenCalledTimes(2)
    expect(importantDateApi.getAll).toHaveBeenNthCalledWith(1, { courseId: 10 })
    expect(importantDateApi.getAll).toHaveBeenNthCalledWith(2, { courseId: 11 })
    expect(store.importantDates.map((item) => item.title)).toEqual(['Course 10 Date', 'Course 11 Date'])
  })

  it('fetchByCourses clears state immediately when no course ids are provided', async () => {
    const store = useImportantDateStore()
    store.importantDates = [buildImportantDate()]

    await store.fetchByCourses([])

    expect(importantDateApi.getAll).not.toHaveBeenCalled()
    expect(store.loading).toBe(false)
    expect(store.importantDates).toEqual([])
  })

  it('fetchByCourse stores an api error message and clears loading on failure', async () => {
    const store = useImportantDateStore()
    vi.mocked(importantDateApi.getAll).mockRejectedValueOnce({
      response: {
        data: {
          error: 'Important dates are temporarily unavailable.',
        },
      },
    })

    await store.fetchByCourse(10)

    expect(store.loading).toBe(false)
    expect(store.error).toBe('Important dates are temporarily unavailable.')
    expect(store.importantDates).toEqual([])
  })

  it('create appends a new important date to the store', async () => {
    const store = useImportantDateStore()
    const created = buildImportantDate({ id: 3, title: 'Final Demo' })
    vi.mocked(importantDateApi.create).mockResolvedValueOnce(buildAxiosResponse(created))

    const result = await store.create(10, {
      title: 'Final Demo',
      description: 'Bring slides',
      dueAt: '2099-04-15T15:00:00Z',
    })

    expect(result).toEqual(created)
    expect(store.importantDates).toEqual([created])
  })

  it('update replaces an existing important date in the store', async () => {
    const store = useImportantDateStore()
    store.importantDates = [buildImportantDate({ id: 3, title: 'Original Title' })]
    const updated = buildImportantDate({ id: 3, title: 'Updated Title' })
    vi.mocked(importantDateApi.update).mockResolvedValueOnce(buildAxiosResponse(updated))

    const result = await store.update(3, {
      title: 'Updated Title',
    })

    expect(result).toEqual(updated)
    expect(store.importantDates[0]?.title).toBe('Updated Title')
  })

  it('remove deletes the important date from the store', async () => {
    const store = useImportantDateStore()
    store.importantDates = [
      buildImportantDate({ id: 3 }),
      buildImportantDate({ id: 4, title: 'Keep me' }),
    ]
    vi.mocked(importantDateApi.remove).mockResolvedValueOnce(buildAxiosResponse(undefined))

    await store.remove(3)

    expect(importantDateApi.remove).toHaveBeenCalledWith(3)
    expect(store.importantDates.map((item) => item.id)).toEqual([4])
  })
})
