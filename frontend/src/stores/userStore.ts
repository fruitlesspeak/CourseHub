import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi, type User, type CreateUserPayload, type UpdateUserPayload } from '../api/index.ts'
import { extractApiErrorMessage } from '@/utils/apiErrors'

export const useUserStore = defineStore('users', () => {
  // ── State ──────────────────────────────────────────────────────────────────
  const users      = ref<User[]>([])
  const loading    = ref(false)
  const error      = ref<string | null>(null)

  // ── Getters ────────────────────────────────────────────────────────────────
  const professors = computed(() => users.value.filter(u => u.isProfessor))
  const students   = computed(() => users.value.filter(u => !u.isProfessor))

  // ── Actions ────────────────────────────────────────────────────────────────
  async function fetchAll(professorFilter?: boolean) {
    loading.value = true; error.value = null
    try {
      const params = professorFilter === undefined ? {} : { professor: professorFilter }
      const { data } = await userApi.getAll(params)
      users.value = data
    } catch (e) {
      error.value = extractApiErrorMessage(e) ?? "We couldn't load users right now. Please try again."
    } finally {
      loading.value = false
    }
  }

  async function create(payload: CreateUserPayload): Promise<User | null> {
    const { data } = await userApi.create(payload)
    users.value.push(data)
    return data
  }

  async function update(uuid: string, payload: UpdateUserPayload): Promise<User | null> {
    const { data } = await userApi.update(uuid, payload)
    const idx = users.value.findIndex(u => u.uuid === uuid)
    if (idx !== -1) users.value[idx] = data
    return data
  }

  async function remove(uuid: string): Promise<void> {
    await userApi.remove(uuid)
    users.value = users.value.filter(u => u.uuid !== uuid)
  }

  return { users, loading, error, professors, students, fetchAll, create, update, remove }
})
