<template>
  <section class="course-form-page" :aria-label="isEditMode ? 'Edit Course' : 'Create Course'">
    <div class="course-form-card">
      <h1 class="title">{{ isEditMode ? 'Edit Course' : 'Create New Course' }}</h1>
      <p class="subtitle">
        {{
          isEditMode
            ? 'Update your course content for students.'
            : 'Share your course content with students.'
        }}
      </p>

      <div v-if="isEditMode && isLoadingCourse" class="notice info" role="status">
        Loading course details...
      </div>

      <div v-else>
        <form class="form" @submit.prevent="onSubmit">
          <label class="field">
            <span>Title *</span>
            <input
              v-model.trim="form.title"
              type="text"
              maxlength="255"
              placeholder="e.g., Introduction to Databases"
              required
            />
          </label>

          <label class="field">
            <span>Code *</span>
            <input
              v-model.trim="form.code"
              type="text"
              maxlength="50"
              placeholder="e.g., COMP-4350"
              required
            />
          </label>

          <label class="field">
            <span>Description</span>
            <textarea
              v-model.trim="form.description"
              rows="4"
              placeholder="Optional course description"
            />
          </label>

          <label class="field">
            <span>Tags</span>
            <input
              v-model.trim="form.tags"
              type="text"
              maxlength="1000"
              placeholder="e.g., databases, sql, backend"
            />
          </label>

          <label class="field">
            <span>Material</span>
            <textarea
              v-model.trim="form.material"
              rows="3"
              placeholder="Optional material summary or notes"
            />
          </label>

          <label class="field">
            <span>Due Date</span>
            <input
              v-model="form.dueDate"
              type="datetime-local"
            />
          </label>

          <label class="field">
            <span>Link</span>
            <input
              v-model.trim="form.link"
              type="text"
              maxlength="1000"
              placeholder="Optional course link (https://... or www...)"
            />
          </label>

          <p v-if="errorMessage" class="notice error" role="alert">{{ errorMessage }}</p>

          <div class="actions">
            <button type="button" class="btn secondary" @click="goBack" :disabled="isSubmitting">
              Cancel
            </button>
            <button type="submit" class="btn primary" :disabled="isSubmitting">
              {{
                isSubmitting
                  ? (isEditMode ? 'Saving...' : 'Creating...')
                  : (isEditMode ? 'Save Changes' : 'Create')
              }}
            </button>
          </div>
        </form>

        <section v-if="isEditMode" class="important-dates-section">
          <h2 class="section-title">Important Dates</h2>
          <p class="section-subtitle">Add assignment, quiz, or lab due dates for this course.</p>

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
                <h3 class="important-date-title">{{ importantDate.title }}</h3>
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
        </section>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseApi } from '@/api'
import { useCourseStore } from '@/stores/courseStore'
import { useImportantDateStore } from '@/stores/importantDateStore'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const importantDateStore = useImportantDateStore()
const authStore = useAuthStore()

const isSubmitting = ref(false)
const isLoadingCourse = ref(false)
const isSavingImportantDate = ref(false)
const errorMessage = ref('')
const importantDateError = ref('')
const editingImportantDateId = ref<number | null>(null)
const deletingImportantDateId = ref<number | null>(null)
const courseId = ref<number | null>(null)

const courseUuid = computed(() => (typeof route.params.uuid === 'string' ? route.params.uuid : ''))
const isEditMode = computed(() => Boolean(courseUuid.value))

const form = reactive({
  title: '',
  code: '',
  description: '',
  tags: '',
  material: '',
  dueDate: '',
  link: '',
})

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

const validateRequiredFields = (): string => {
  if (!form.title.trim()) return 'Title is required.'
  if (!form.code.trim()) return 'Code is required.'
  return ''
}

const normalizeAndValidateLink = (rawLink: string): { normalized?: string; error?: string } => {
  const trimmed = rawLink.trim()
  if (!trimmed) {
    return { normalized: undefined }
  }

  const normalized = trimmed.startsWith('www.') ? `https://${trimmed}` : trimmed
  if (!normalized.startsWith('https://')) {
    return { error: 'Link must start with https:// or www.' }
  }

  try {
    const parsed = new URL(normalized)
    if (parsed.protocol !== 'https:' || !parsed.hostname) {
      return { error: 'Link is invalid.' }
    }
  } catch {
    return { error: 'Link is invalid.' }
  }

  return { normalized }
}

const normalizeAndValidateDueDate = (rawDueDate: string): { iso?: string; error?: string } => {
  const trimmed = rawDueDate.trim()
  if (!trimmed) {
    return { iso: undefined }
  }

  const parsed = new Date(trimmed)
  if (Number.isNaN(parsed.getTime())) {
    return { error: 'Due date is invalid.' }
  }

  return { iso: parsed.toISOString() }
}

const onSubmit = async () => {
  errorMessage.value = validateRequiredFields()
  if (errorMessage.value) return

  const linkResult = normalizeAndValidateLink(form.link)
  if (linkResult.error) {
    errorMessage.value = linkResult.error
    return
  }

  const dueDateResult = normalizeAndValidateDueDate(form.dueDate)
  if (dueDateResult.error) {
    errorMessage.value = dueDateResult.error
    return
  }

  isSubmitting.value = true
  try {
    const payload = {
      title: form.title.trim(),
      code: form.code.trim(),
      description: form.description,
      tags: form.tags,
      material: form.material,
      dueDate: dueDateResult.iso,
      link: linkResult.normalized ?? '',
    }

    if (isEditMode.value) {
      await courseStore.update(courseUuid.value, payload)
      await router.push(authStore.defaultDashboardPath)
      return
    }

    await courseStore.create(payload)
    await router.push({
      path: authStore.defaultDashboardPath,
      query: { courseCreated: '1' },
    })
  } catch (e: unknown) {
    errorMessage.value = extractError(e) ?? 'Unable to save this course right now.'
  } finally {
    isSubmitting.value = false
  }
}

const validateImportantDateFields = (): string => {
  if (!importantDateForm.title.trim()) return 'Title is required.'
  if (!importantDateForm.dueAt.trim()) return 'Due date is required.'
  return ''
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
    importantDateError.value = 'Course is not ready yet.'
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
    importantDateError.value = extractError(e) ?? 'Unable to save this important date right now.'
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
    importantDateError.value = extractError(e) ?? 'Unable to delete this important date right now.'
  } finally {
    deletingImportantDateId.value = null
  }
}

const goBack = async () => {
  await router.push(authStore.defaultDashboardPath)
}

onMounted(async () => {
  if (!isEditMode.value) {
    return
  }

  isLoadingCourse.value = true
  try {
    const { data } = await courseApi.getOne(courseUuid.value)
    courseId.value = data.id
    form.title = data.title ?? ''
    form.code = data.code ?? ''
    form.description = data.description ?? ''
    form.tags = data.tags ?? ''
    form.material = data.material ?? ''
    form.dueDate = toDateTimeLocal(data.dueDate)
    form.link = data.link ?? ''
    await importantDateStore.fetchByCourse(data.id)
  } catch (e: unknown) {
    errorMessage.value = extractError(e) ?? 'Unable to load course details.'
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

function extractError(e: unknown): string | null {
  if (e && typeof e === 'object' && 'response' in e) {
    const response = (e as { response?: { data?: { error?: string; message?: string } } }).response
    return response?.data?.error ?? response?.data?.message ?? null
  }
  return null
}
</script>

<style scoped>
.course-form-page {
  min-height: 100vh;
  background: var(--color-bg-page);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.course-form-card {
  width: min(46rem, 96vw);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.85rem;
  padding: 1.25rem;
}

.title {
  margin: 0;
  font-size: 1.5rem;
}

.subtitle {
  margin-top: 0.35rem;
  color: var(--color-text-secondary);
}

.form {
  margin-top: 1rem;
  display: grid;
  gap: 0.9rem;
}

.important-dates-section {
  margin-top: 1.5rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--color-border);
}

.section-title {
  margin: 0;
  font-size: 1.2rem;
}

.section-subtitle {
  margin: 0.35rem 0 0;
  color: var(--color-text-secondary);
}

.important-date-form {
  margin-top: 0.9rem;
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
  margin: 0;
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

@media (max-width: 640px) {
  .important-date-item {
    flex-direction: column;
  }
}
</style>
