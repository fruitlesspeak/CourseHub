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

          <section v-if="course.dueDate" class="detail-section">
            <h3>Due Date</h3>
            <p>{{ formatDateTime(course.dueDate) }}</p>
          </section>

          <!-- Enrolled-only content -->
          <template v-if="enrolled">
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
          </template>

          <!-- Not enrolled gate -->
          <div v-else class="enroll-gate">
            <svg viewBox="0 0 24 24" fill="none" width="28" height="28" aria-hidden="true">
              <rect x="3" y="11" width="18" height="11" rx="2" stroke="currentColor" stroke-width="1.6"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
            <div>
              <p class="gate-title">Enroll to access course materials</p>
              <p class="gate-sub">Materials and course links are only available to enrolled students.</p>
            </div>
            <button class="enroll-btn" :disabled="enrolling" @click="handleEnroll">
              <span v-if="enrolling" class="spinner" aria-hidden="true"></span>
              {{ enrolling ? 'Enrolling…' : 'Enroll Now' }}
            </button>
          </div>

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
import { courseApi, enrollmentApi } from '@/api'
import type { Course } from '@/api'
import { parseTags } from '@/stores/enrollmentStore'
import { extractApiErrorStatus } from '@/utils/apiErrors'

const route = useRoute()
const router = useRouter()

const course = ref<Course | null>(null)
const loading = ref(false)
const error = ref('')
const enrolled = ref(false)
const enrolling = ref(false)

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
    const [courseRes, enrolledCourses] = await Promise.all([
      courseApi.getOne(uuid),
      enrollmentApi.getMyCourses(),
    ])
    course.value = courseRes.data
    enrolled.value = enrolledCourses.data.some((c: Course) => c.uuid === uuid)
  } catch (err: unknown) {
    const status = extractApiErrorStatus(err)
    if (status === 404) {
      error.value = 'Course not found.'
    } else {
      error.value = "We couldn't load this course right now. Please try again."
    }
  } finally {
    loading.value = false
  }
})

async function handleEnroll() {
  if (!course.value) return
  enrolling.value = true
  try {
    await enrollmentApi.enroll(course.value.uuid)
    enrolled.value = true
  } catch (err: unknown) {
    const status = extractApiErrorStatus(err)
    if (status === 409) {
      enrolled.value = true
    } else {
      error.value = "We couldn't enroll you in this course. Please try again."
    }
  } finally {
    enrolling.value = false
  }
}

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

.course-link:hover { text-decoration: underline; }

/* ── Enroll gate ── */
.enroll-gate {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.1rem;
  border: 1px dashed var(--color-border);
  border-radius: 0.75rem;
  background: var(--color-bg-soft);
  color: var(--color-text-secondary);
}

.gate-title {
  margin: 0 0 0.15rem;
  font-weight: 600;
  color: var(--color-text-primary);
  font-size: 0.9rem;
}

.gate-sub {
  margin: 0;
  font-size: 0.82rem;
}

.enroll-btn {
  margin-left: auto;
  flex-shrink: 0;
  border: 0;
  border-radius: 0.6rem;
  padding: 0.5rem 1rem;
  background: var(--color-brand-500);
  color: #fff;
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}
.enroll-btn:hover:not(:disabled) { background: var(--color-brand-600); }
.enroll-btn:disabled { opacity: 0.65; cursor: not-allowed; }

.spinner {
  width: 0.75rem;
  height: 0.75rem;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.detail-footer {
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border);
}

.back-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 0.88rem;
}

.back-link:hover { color: var(--color-text-primary); }
</style>
