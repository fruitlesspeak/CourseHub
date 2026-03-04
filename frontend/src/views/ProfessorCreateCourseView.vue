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

      <form v-else class="form" @submit.prevent="onSubmit">
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
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { courseApi } from '@/api'
import { useCourseStore } from '@/stores/courseStore'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const courseStore = useCourseStore()
const authStore = useAuthStore()

const isSubmitting = ref(false)
const isLoadingCourse = ref(false)
const errorMessage = ref('')

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
    form.title = data.title ?? ''
    form.code = data.code ?? ''
    form.description = data.description ?? ''
    form.tags = data.tags ?? ''
    form.material = data.material ?? ''
    form.dueDate = toDateTimeLocal(data.dueDate)
    form.link = data.link ?? ''
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
</style>
