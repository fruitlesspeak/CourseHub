import { defineStore } from 'pinia'
import { ref } from 'vue'
import { courseApi, type Course, type CreateCoursePayload, type UpdateCoursePayload } from '../api/index.ts'
import { extractApiErrorMessage } from '@/utils/apiErrors'

export const useCourseStore = defineStore('courses', () => {
  // ── State ──────────────────────────────────────────────────────────────────
  const courses = ref<Course[]>([])
  const loading = ref(false)
  const error   = ref<string | null>(null)

  // ── Actions ────────────────────────────────────────────────────────────────
  async function fetchAll(params: { title?: string; professorId?: number } = {}) {
    loading.value = true; error.value = null
    try {
      const { data } = await courseApi.getAll(params)
      courses.value = data
    } catch (e) {
      error.value = extractApiErrorMessage(e) ?? "We couldn't load courses right now. Please try again."
    } finally {
      loading.value = false
    }
  }

  async function create(payload: CreateCoursePayload): Promise<Course> {
    const { data } = await courseApi.create(payload)
    courses.value.push(data)
    return data
  }

  async function update(uuid: string, payload: UpdateCoursePayload): Promise<Course> {
    const { data } = await courseApi.update(uuid, payload)
    const idx = courses.value.findIndex(c => c.uuid === uuid)
    if (idx !== -1) courses.value[idx] = data
    return data
  }

  async function remove(uuid: string): Promise<void> {
    await courseApi.remove(uuid)
    courses.value = courses.value.filter(c => c.uuid !== uuid)
  }

  return { courses, loading, error, fetchAll, create, update, remove }
})
