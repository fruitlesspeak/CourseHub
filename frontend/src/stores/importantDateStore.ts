import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  importantDateApi,
  type ImportantDate,
  type CreateImportantDatePayload,
  type UpdateImportantDatePayload,
} from '../api/index.ts'

export const useImportantDateStore = defineStore('importantDates', () => {
  const importantDates = ref<ImportantDate[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchByCourse(courseId: number) {
    loading.value = true
    error.value = null
    try {
      const { data } = await importantDateApi.getAll({ courseId })
      importantDates.value = data
    } catch (e) {
      error.value = extractError(e) ?? 'Failed to load important dates.'
    } finally {
      loading.value = false
    }
  }

  async function fetchByCourses(courseIds: number[]) {
    loading.value = true
    error.value = null
    try {
      if (!courseIds.length) {
        importantDates.value = []
        return
      }

      const responses = await Promise.all(courseIds.map((courseId) => importantDateApi.getAll({ courseId })))
      importantDates.value = responses.flatMap((res: { data: ImportantDate[] }) => res.data)
    } catch (e) {
      error.value = extractError(e) ?? 'Failed to load important dates.'
    } finally {
      loading.value = false
    }
  }

  async function create(courseId: number, payload: CreateImportantDatePayload): Promise<ImportantDate> {
    const { data } = await importantDateApi.create(courseId, payload)
    importantDates.value.push(data)
    return data
  }

  async function update(id: number, payload: UpdateImportantDatePayload): Promise<ImportantDate> {
    const { data } = await importantDateApi.update(id, payload)
    const idx = importantDates.value.findIndex((d) => d.id === id)
    if (idx !== -1) importantDates.value[idx] = data
    return data
  }

  async function remove(id: number): Promise<void> {
    await importantDateApi.remove(id)
    importantDates.value = importantDates.value.filter((d) => d.id !== id)
  }

  return {
    importantDates,
    loading,
    error,
    fetchByCourse,
    fetchByCourses,
    create,
    update,
    remove,
  }
})

function extractError(e: unknown): string | null {
  if (e && typeof e === 'object' && 'response' in e) {
    const resp = (e as { response?: { data?: { error?: string } } }).response
    return resp?.data?.error ?? null
  }
  return null
}
