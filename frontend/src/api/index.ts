import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? import.meta.env.VITE_API_URL ?? '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

// ── Types ─────────────────────────────────────────────────────────────────────

export interface User {
  id:          number
  uuid:        string
  email:       string
  firstName:   string
  lastName:    string
  isProfessor: boolean
  professorId: number | null
  studentId:   string | null
  createdAt:   string
  updatedAt:   string
}

export interface CreateUserPayload {
  email:       string
  password:    string
  firstName:   string
  lastName:    string
  isProfessor: boolean
  professorId?: number | null
  studentId?:  string | null
}

export interface UpdateUserPayload {
  email?:       string
  firstName?:   string
  lastName?:    string
  isProfessor?: boolean
  professorId?: number | null
  studentId?:   string | null
}

export interface Course {
  id:          number
  uuid:        string
  title:       string
  code:        string
  description: string | null
  link:        string | null
  professorId: number
  createdAt:   string
  updatedAt:   string
}

export interface CreateCoursePayload {
  title:        string
  code?:        string
  description?: string
  link?:        string
  professorId?: number
}

export interface UpdateCoursePayload {
  title?:       string
  code?:        string
  description?: string
  link?:        string
  professorId?: number
}

export interface ApiError {
  error?: string
  [field: string]: string | undefined
}

// ── Users ─────────────────────────────────────────────────────────────────────

export const userApi = {
  getAll:  (params: { professor?: boolean } = {}) =>
    api.get<User[]>('/users', { params }),

  getOne:  (uuid: string) =>
    api.get<User>(`/users/${uuid}`),

  create:  (data: CreateUserPayload) =>
    api.post<User>('/users', data),

  update:  (uuid: string, data: UpdateUserPayload) =>
    api.patch<User>(`/users/${uuid}`, data),

  remove:  (uuid: string) =>
    api.delete<void>(`/users/${uuid}`),
}

// ── Courses ───────────────────────────────────────────────────────────────────

export const courseApi = {
  getAll:  (params: { title?: string; professorId?: number } = {}) =>
    api.get<Course[]>('/courses', { params }),

  getOne:  (uuid: string) =>
    api.get<Course>(`/courses/${uuid}`),

  create:  (data: CreateCoursePayload) =>
    api.post<Course>('/courses', data),

  update:  (uuid: string, data: UpdateCoursePayload) =>
    api.patch<Course>(`/courses/${uuid}`, data),

  remove:  (uuid: string) =>
    api.delete<void>(`/courses/${uuid}`),
}

export default api
