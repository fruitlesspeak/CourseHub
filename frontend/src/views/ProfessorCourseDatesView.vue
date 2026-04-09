<template>
  <section class="course-dates-page" aria-label="Manage Course Dates">
    <div class="course-dates-card">
      <div class="page-head">
        <div>
          <h1 class="title">Manage Dates</h1>
          <p class="subtitle">
            <span v-if="courseTitle">{{ courseTitle }}</span>
            <span v-else>Update assignment, quiz, or lab dates for this course.</span>
          </p>
        </div>
        <div class="header-actions">
          <button type="button" class="btn secondary" @click="onEditCourse" :disabled="isLoadingCourse">
            Edit Course
          </button>
          <button type="button" class="btn secondary" @click="goBack">
            Back to Courses
          </button>
        </div>
      </div>

      <div v-if="isLoadingCourse" class="notice info" role="status">
        Loading course details...
      </div>

      <template v-else>
        <form class="form important-date-form" @submit.prevent="onImportantDateSubmit">
          <label class="field">
            <span>Title *</span>
            <input
              v-model.trim="importantDateForm.title"
              type="text"
              maxlength="255"
              placeholder="e.g., Assignment 1"
              required
            />
          </label>

          <label class="field">
            <span>Description</span>
            <textarea
              v-model.trim="importantDateForm.description"
              rows="3"
              placeholder="Optional notes"
            />
          </label>

          <label class="field">
            <span>Due At *</span>
            <input
              v-model="importantDateForm.dueAt"
              type="datetime-local"
              required
            />
          </label>

          <p v-if="importantDateError" class="notice error" role="alert">{{ importantDateError }}</p>

          <div class="actions">
            <button
              v-if="editingImportantDateId !== null"
              type="button"
              class="btn secondary"
              :disabled="isSavingImportantDate"
              @click="resetImportantDateForm"
            >
              Cancel Edit
            </button>
            <button type="submit" class="btn primary" :disabled="isSavingImportantDate">
              {{
                isSavingImportantDate
                  ? (editingImportantDateId !== null ? 'Saving...' : 'Adding...')
                  : (editingImportantDateId !== null ? 'Save Date' : 'Add Date')
              }}
            </button>
          </div>
        </form>

        <div v-if="importantDateStore.loading" class="notice info" role="status">
          Loading important dates...
        </div>

        <div v-else-if="sortedImportantDates.length" class="important-date-list">
          <article
            v-for="importantDate in sortedImportantDates"
            :key="importantDate.id"
            class="important-date-item"
          >
            <div>
              <h2 class="important-date-title">{{ importantDate.title }}</h2>
              <p class="important-date-meta">{{ formatDateTime(importantDate.dueAt) }}</p>
              <p v-if="importantDate.description" class="important-date-meta">
                {{ importantDate.description }}
              </p>
            </div>
            <div class="important-date-actions">
              <button
                type="button"
                class="btn secondary small-btn"
                :disabled="isSavingImportantDate || deletingImportantDateId === importantDate.id"
                @click="startEditingImportantDate(importantDate.id)"
              >
                Edit
              </button>
              <button
                type="button"
                class="btn secondary small-btn danger-btn"
                :disabled="isSavingImportantDate || deletingImportantDateId === importantDate.id"
                @click="onDeleteImportantDate(importantDate.id)"
              >
                {{ deletingImportantDateId === importantDate.id ? 'Deleting...' : 'Delete' }}
              </button>
            </div>
          </article>
        </div>

        <div v-else class="notice info">No important dates added yet.</div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseApi } from '@/api'
import { useImportantDateStore } from '@/stores/importantDateStore'
import { useAuthStore } from '@/stores/authStore'
import { extractApiErrorMessage } from '@/utils/apiErrors'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const importantDateStore = useImportantDateStore()

const courseUuid = computed(() => (typeof route.params.uuid === 'string' ? route.params.uuid : ''))
const courseId = ref<number | null>(null)
const courseTitle = ref('')
const isLoadingCourse = ref(false)
const isSavingImportantDate = ref(false)
const importantDateError = ref('')
const editingImportantDateId = ref<number | null>(null)
const deletingImportantDateId = ref<number | null>(null)

const importantDateForm = reactive({
  title: '',
  description: '',
  dueAt: '',
})

const sortedImportantDates = computed(() => {
  return [...importantDateStore.importantDates].sort(
    (a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime(),
  )
})

const validateImportantDateFields = (): string => {
  if (!importantDateForm.title.trim()) return 'Title is required.'
  if (!importantDateForm.dueAt.trim()) return 'Due date is required.'
  return ''
}

const normalizeAndValidateDueDate = (rawDueDate: string): { iso?: string; error?: string } => {
  const trimmed = rawDueDate.trim()
  if (!trimmed) {
    return { iso: undefined }
  }

  const parsed = new Date(trimmed)
  if (Number.isNaN(parsed.getTime())) {
    return { error: 'Enter a valid due date and time.' }
  }

  return { iso: parsed.toISOString() }
}

const onImportantDateSubmit = async () => {
  importantDateError.value = validateImportantDateFields()
  if (importantDateError.value) return

  const dueDateResult = normalizeAndValidateDueDate(importantDateForm.dueAt)
  if (dueDateResult.error) {
    importantDateError.value = dueDateResult.error
    return
  }

  if (courseId.value === null || !dueDateResult.iso) {
    importantDateError.value = 'Course details are still loading.'
    return
  }

  isSavingImportantDate.value = true
  try {
    const payload = {
      title: importantDateForm.title.trim(),
      description: importantDateForm.description.trim(),
      dueAt: dueDateResult.iso,
    }

    if (editingImportantDateId.value !== null) {
      await importantDateStore.update(editingImportantDateId.value, payload)
    } else {
      await importantDateStore.create(courseId.value, payload)
    }

    resetImportantDateForm()
  } catch (e: unknown) {
    importantDateError.value = extractApiErrorMessage(e) ?? "We couldn't save this important date right now. Please try again."
  } finally {
    isSavingImportantDate.value = false
  }
}

const startEditingImportantDate = (id: number) => {
  const importantDate = importantDateStore.importantDates.find((item) => item.id === id)
  if (!importantDate) {
    importantDateError.value = 'Important date not found.'
    return
  }

  editingImportantDateId.value = id
  importantDateError.value = ''
  importantDateForm.title = importantDate.title
  importantDateForm.description = importantDate.description ?? ''
  importantDateForm.dueAt = toDateTimeLocal(importantDate.dueAt)
}

const resetImportantDateForm = () => {
  editingImportantDateId.value = null
  importantDateError.value = ''
  importantDateForm.title = ''
  importantDateForm.description = ''
  importantDateForm.dueAt = ''
}

const onDeleteImportantDate = async (id: number) => {
  const confirmed = window.confirm('Delete this important date?')
  if (!confirmed) return

  deletingImportantDateId.value = id
  importantDateError.value = ''
  try {
    await importantDateStore.remove(id)
    if (editingImportantDateId.value === id) {
      resetImportantDateForm()
    }
  } catch (e: unknown) {
    importantDateError.value = extractApiErrorMessage(e) ?? "We couldn't delete this important date right now. Please try again."
  } finally {
    deletingImportantDateId.value = null
  }
}

const onEditCourse = async () => {
  await router.push(`/professor/courses/${encodeURIComponent(courseUuid.value)}/edit`)
}

const goBack = async () => {
  await router.push(authStore.defaultDashboardPath)
}

onMounted(async () => {
  isLoadingCourse.value = true
  importantDateStore.importantDates = []

  try {
    const { data } = await courseApi.getOne(courseUuid.value)
    courseId.value = data.id
    courseTitle.value = data.title
    await importantDateStore.fetchByCourse(data.id)
  } catch (e: unknown) {
    importantDateError.value = extractApiErrorMessage(e) ?? "We couldn't load this course right now. Please try again."
  } finally {
    isLoadingCourse.value = false
  }
})

function toDateTimeLocal(iso: string | null): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''

  const pad = (n: number) => String(n).padStart(2, '0')
  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())

  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString()
}
</script>

<style scoped>
.course-dates-page {
  min-height: 100vh;
  background: var(--color-bg-page);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.course-dates-card {
  width: min(52rem, 96vw);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.85rem;
  padding: 1.25rem;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.title {
  margin: 0;
  font-size: 1.5rem;
}

.subtitle {
  margin-top: 0.35rem;
  color: var(--color-text-secondary);
}

.header-actions {
  display: flex;
  gap: 0.6rem;
}

.form {
  margin-top: 1rem;
  display: grid;
  gap: 0.9rem;
}

.field {
  display: grid;
  gap: 0.35rem;
}

.field span {
  font-weight: 600;
}

input,
textarea {
  border: 1px solid var(--color-border);
  border-radius: 0.6rem;
  padding: 0.65rem 0.75rem;
  font: inherit;
}

.notice {
  border-radius: 0.6rem;
  padding: 0.6rem 0.75rem;
  margin: 1rem 0 0;
}

.notice.info {
  border: 1px solid #bfdbfe;
  background: #eff6ff;
  color: #1e3a8a;
}

.notice.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

.important-date-list {
  margin-top: 1rem;
  display: grid;
  gap: 0.75rem;
}

.important-date-item {
  border: 1px solid var(--color-border);
  border-radius: 0.6rem;
  padding: 0.85rem;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
}

.important-date-title {
  margin: 0;
  font-size: 1rem;
}

.important-date-meta {
  margin: 0.35rem 0 0;
  color: var(--color-text-secondary);
}

.important-date-actions {
  display: flex;
  gap: 0.5rem;
  align-items: flex-start;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
}

.btn {
  border-radius: 0.6rem;
  height: 2.5rem;
  padding: 0 1rem;
  font-weight: 600;
  cursor: pointer;
}

.btn.primary {
  border: 0;
  background: var(--color-brand-500);
  color: #fff;
}

.btn.primary:hover:enabled {
  background: var(--color-brand-600);
}

.btn.secondary {
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
}

.btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.small-btn {
  height: 2.15rem;
  padding: 0 0.8rem;
}

.danger-btn {
  border-color: #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

@media (max-width: 720px) {
  .page-head,
  .header-actions,
  .important-date-item {
    flex-direction: column;
  }
}
</style>
