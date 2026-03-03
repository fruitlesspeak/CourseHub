<template>
  <section class="create-course-page" aria-label="Create Course">
    <div class="create-course-card">
      <h1 class="title">Create New Course</h1>
      <p class="subtitle">Share your course content with students.</p>

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
            {{ isSubmitting ? 'Creating...' : 'Create' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useCourseStore } from '@/stores/courseStore'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const courseStore = useCourseStore()
const authStore = useAuthStore()

const isSubmitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  title: '',
  code: '',
  description: '',
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

const onSubmit = async () => {
  errorMessage.value = validateRequiredFields()
  if (errorMessage.value) return

  const linkResult = normalizeAndValidateLink(form.link)
  if (linkResult.error) {
    errorMessage.value = linkResult.error
    return
  }

  isSubmitting.value = true
  try {
    await courseStore.create({
      title: form.title.trim(),
      code: form.code.trim(),
      description: form.description.trim() || undefined,
      link: linkResult.normalized,
    })

    await router.push({
      path: authStore.defaultDashboardPath,
      query: { courseCreated: '1' },
    })
  } catch (e: unknown) {
    errorMessage.value = extractError(e) ?? 'Unable to create course right now.'
  } finally {
    isSubmitting.value = false
  }
}

const goBack = async () => {
  await router.push(authStore.defaultDashboardPath)
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
.create-course-page {
  min-height: 100vh;
  background: var(--color-bg-page);
  display: grid;
  place-items: center;
  padding: 1rem;
}

.create-course-card {
  width: min(42rem, 96vw);
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

.notice.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
  border-radius: 0.6rem;
  padding: 0.6rem 0.75rem;
  margin: 0;
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
