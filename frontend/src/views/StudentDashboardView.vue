<template>
  <div class="student-dashboard">
    <DashboardLayout
      role-label="Student"
      default-name="Student"
      title="My Courses"
      primary-action-text="Browse Catalog"
      empty-message="You haven't enrolled in any courses yet."
      empty-action-text="Browse Catalog"
      @primary-action="onBrowseCatalog"
    >
      <template #content>
        <div v-if="loading" class="state loading">Loading your courses...</div>

        <div v-else-if="error" class="state error">{{ error }}</div>

        <div v-else-if="courses.length" class="course-list">
          <article v-for="course in courses" :key="course.uuid" class="course-item">
            <div class="course-main">
              <h3>{{ course.title }}</h3>
              <p class="course-code">{{ course.code }}</p>
              <p v-if="course.description" class="course-desc">{{ course.description }}</p>
              <a
                v-if="course.link"
                class="course-link"
                :href="toCourseHref(course.link)"
                target="_blank"
                rel="noopener noreferrer"
              >
                Open course link
              </a>
              <p v-if="course.tags" class="course-meta"><strong>Tags:</strong> {{ course.tags }}</p>
              <p v-if="course.dueDate" class="course-meta">
                <strong>Due:</strong> {{ formatDateTime(course.dueDate) }}
              </p>
              <div class="course-actions">
                <router-link
                  :to="{ name: 'CourseDetail', params: { uuid: course.uuid }, query: { from: 'dashboard' } }"
                  class="course-btn view"
                >
                  View Course
                </router-link>
              </div>
            </div>
            <time class="enrolled-at">Enrolled {{ formatDate(course.createdAt) }}</time>
          </article>
        </div>

        <div v-else class="state empty">
          <p>You haven't enrolled in any courses yet.</p>
          <button class="browse-btn" type="button" @click="onBrowseCatalog">Browse Catalog</button>
        </div>
      </template>
    </DashboardLayout>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import DashboardLayout from '@/components/dashboard/DashboardLayout.vue'
import { enrollmentApi } from '@/api'
import type { Course } from '@/api'

const router = useRouter()

const courses = ref<Course[]>([])
const loading = ref(false)
const error = ref('')

const onBrowseCatalog = () => {
  router.push({ name: 'student-catalog' })
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await enrollmentApi.getMyCourses()
    courses.value = data
  } catch {
    error.value = 'Failed to load your courses. Please try again.'
  } finally {
    loading.value = false
  }
})

const formatDate = (isoDate: string) => new Date(isoDate).toLocaleDateString()

const formatDateTime = (isoDate: string) => new Date(isoDate).toLocaleString()

const toCourseHref = (link: string) =>
  link.startsWith('www.') ? `https://${link}` : link
</script>

<style scoped>
.student-dashboard {
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

.state.empty {
  display: grid;
  justify-items: start;
  gap: 0.75rem;
  border: 1px dashed var(--color-border);
  color: var(--color-text-secondary);
}

.browse-btn {
  border: 0;
  border-radius: 0.6rem;
  padding: 0.55rem 0.95rem;
  background: var(--color-brand-500);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.course-list {
  margin-top: 1rem;
  display: grid;
  gap: 0.75rem;
}

.course-item {
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  padding: 0.85rem 0.95rem;
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.course-main h3 {
  margin: 0;
  font-size: 1.05rem;
}

.course-code {
  margin: 0.2rem 0 0;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-brand-600);
}

.course-desc {
  margin: 0.45rem 0;
  color: var(--color-text-secondary);
}

.course-link {
  font-size: 0.9rem;
  text-decoration: none;
  color: var(--color-brand-500);
}

.course-link:hover {
  text-decoration: underline;
}

.course-meta {
  margin: 0.4rem 0 0;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.course-actions {
  margin-top: 0.7rem;
  display: flex;
  gap: 0.5rem;
}

.course-btn {
  border-radius: 0.55rem;
  padding: 0.35rem 0.7rem;
  border: 1px solid transparent;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.9rem;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.course-btn.view {
  background: #e0f2fe;
  border-color: #bae6fd;
  color: #075985;
}

.enrolled-at {
  white-space: nowrap;
  color: var(--color-text-secondary);
  font-size: 0.84rem;
}

@media (max-width: 720px) {
  .course-item {
    flex-direction: column;
  }

  .enrolled-at {
    align-self: flex-start;
  }
}
</style>
