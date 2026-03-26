<template>
  <div class="course-detail-page">
    <DashboardLayout
      role-label="Student"
      default-name="Student"
      :title="course?.title ?? 'Course'"
      primary-action-text="My Courses"
      empty-message=""
      empty-action-text=""
      @primary-action="router.push({ name: 'student-dashboard' })"
    >
      <template #content>
        <div v-if="loading" class="state loading">Loading course...</div>

        <div v-else-if="error" class="state error">{{ error }}</div>

        <div v-else-if="course" class="detail-body">
          <div class="detail-header">
            <div class="header-band" :style="{ background: bandColor }" aria-hidden="true">
              <span class="header-initial">{{ initial }}</span>
            </div>
            <div class="header-meta">
              <span class="course-code">{{ course.code }}</span>
              <h2 class="course-title">{{ course.title }}</h2>
              <div v-if="tags.length" class="tag-list">
                <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
              </div>
            </div>
          </div>

          <section v-if="course.description" class="detail-section">
            <h3>Description</h3>
            <p>{{ course.description }}</p>
          </section>

          <section v-if="course.material" class="detail-section">
            <h3>Materials</h3>
            <p class="material-content">{{ course.material }}</p>
          </section>

          <section v-if="course.link" class="detail-section">
            <h3>Course Link</h3>
            <a
              :href="toCourseHref(course.link)"
              target="_blank"
              rel="noopener noreferrer"
              class="course-link"
            >
              Open course link ↗
            </a>
          </section>

          <section v-if="course.dueDate" class="detail-section">
            <h3>Due Date</h3>
            <p>{{ formatDateTime(course.dueDate) }}</p>
          </section>

          <div class="detail-footer">
            <router-link :to="backRoute" class="back-link">
              ← {{ backLabel }}
            </router-link>
          </div>
        </div>
      </template>
    </DashboardLayout>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DashboardLayout from '@/components/dashboard/DashboardLayout.vue'
import { courseApi } from '@/api'
import type { Course } from '@/api'
import { parseTags } from '@/stores/enrollmentStore'

const route = useRoute()
const router = useRouter()

const course = ref<Course | null>(null)
const loading = ref(false)
const error = ref('')

const BANDS = [
  'linear-gradient(135deg,#1e3a8a,#3b82f6)',
  'linear-gradient(135deg,#14532d,#22c55e)',
  'linear-gradient(135deg,#581c87,#a855f7)',
  'linear-gradient(135deg,#7c2d12,#f97316)',
  'linear-gradient(135deg,#134e4a,#14b8a6)',
  'linear-gradient(135deg,#831843,#ec4899)',
  'linear-gradient(135deg,#1e3a5f,#0ea5e9)',
  'linear-gradient(135deg,#3b0764,#8b5cf6)',
]

const from = route.query.from as string | undefined
const backRoute = computed(() =>
  from === 'dashboard' ? { name: 'student-dashboard' } : { name: 'student-catalog' }
)
const backLabel = computed(() =>
  from === 'dashboard' ? 'Back to Dashboard' : 'Back to Catalog'
)

const bandColor = computed(() => BANDS[(course.value?.id ?? 0) % BANDS.length])
const initial = computed(() => (course.value?.title ?? '?')[0]?.toUpperCase() ?? '?')
const tags = computed(() => parseTags(course.value?.tags ?? null))

onMounted(async () => {
  const uuid = route.params.uuid as string
  loading.value = true
  try {
    const { data } = await courseApi.getOne(uuid)
    course.value = data
  } catch (err: any) {
    const status = err?.response?.status
    if (status === 404) {
      error.value = 'Course not found.'
    } else if (status === 403) {
      error.value = 'You must be enrolled to access this course.'
    } else {
      error.value = 'Failed to load course. Please try again.'
    }
  } finally {
    loading.value = false
  }
})

const formatDateTime = (isoDate: string) => new Date(isoDate).toLocaleString()

const toCourseHref = (link: string) =>
  link.startsWith('www.') ? `https://${link}` : link
</script>

<style scoped>
.course-detail-page {
  background: var(--color-bg-page);
}

.state {
  margin-top: 1.2rem;
  border-radius: 0.75rem;
  padding: 0.85rem 1rem;
}

.state.loading {
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}

.state.error {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.detail-header {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border);
}

.header-band {
  width: 5rem;
  height: 5rem;
  border-radius: 0.75rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-initial {
  font-size: 2.5rem;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.25);
  user-select: none;
}

.header-meta {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.course-code {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-brand-600);
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  padding: 0.1rem 0.45rem;
  align-self: flex-start;
}

.course-title {
  margin: 0;
  font-size: 1.4rem;
  line-height: 1.2;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.3rem;
  margin-top: 0.2rem;
}

.tag {
  font-size: 0.67rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.35px;
  padding: 0.14rem 0.48rem;
  border-radius: 999px;
  background: var(--color-bg-soft);
  color: var(--color-brand-600);
  border: 1px solid var(--color-border-muted);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.detail-section h3 {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--color-text-secondary);
}

.detail-section p {
  margin: 0;
  color: var(--color-text-primary);
  line-height: 1.6;
}

.material-content {
  white-space: pre-wrap;
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border);
  border-radius: 0.6rem;
  padding: 0.85rem 1rem;
  font-size: 0.9rem;
}

.course-link {
  color: var(--color-brand-500);
  text-decoration: none;
  font-size: 0.9rem;
}

.course-link:hover {
  text-decoration: underline;
}

.detail-footer {
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.back-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 0.88rem;
}

.back-link:hover {
  color: var(--color-text-primary);
}
</style>
