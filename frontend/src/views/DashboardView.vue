<template>
  <div class="professor-dashboard">
    <transition name="notice-fade">
      <p v-if="showCourseCreatedNotice" class="notice success" role="status">Course Created</p>
    </transition>
    <p v-if="actionError" class="notice error" role="alert">{{ actionError }}</p>

    <DashboardLayout
      role-label="Professor"
      default-name="Professor"
      :title="activeTab === 'courses' ? 'My Courses' : 'Reviews'"
      :primary-action-text="activeTab === 'courses' ? 'Create New Course' : ''"
      empty-message="You haven't created any courses yet."
      empty-action-text="Create Your First Course"
      @primary-action="onCreateCourse"
    >
      <template #content>
        <div class="tab-bar">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'courses' }"
            type="button"
            @click="activeTab = 'courses'"
          >My Courses</button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'reviews' }"
            type="button"
            @click="onReviewsTab"
          >Reviews</button>
        </div>

        <template v-if="activeTab === 'courses'">
          <div v-if="courseStore.loading" class="state loading">Loading courses...</div>

          <div v-else-if="courseStore.error" class="state error">{{ courseStore.error }}</div>

          <div v-else-if="courseStore.courses.length" class="course-list">
            <article v-for="course in courseStore.courses" :key="course.uuid" class="course-item">
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
                <p v-if="course.material" class="course-meta"><strong>Material:</strong> {{ course.material }}</p>
                <p v-if="course.dueDate" class="course-meta">
                  <strong>Due:</strong> {{ formatDateTime(course.dueDate) }}
                </p>
                <div class="course-actions">
                  <button type="button" class="course-btn edit" @click="onEditCourse(course.uuid)">Edit</button>
                  <button type="button" class="course-btn delete" @click="onDeleteCourse(course.uuid)">Delete</button>
                </div>
              </div>
              <time class="created-at">{{ formatDate(course.createdAt) }}</time>
            </article>
          </div>

          <div v-else class="state empty">
            <p>You haven't created any courses yet.</p>
            <button class="create-first" type="button" @click="onCreateCourse">Create Your First Course</button>
          </div>
        </template>

        <template v-else>
          <div v-if="reviewStore.loading" class="state loading">Loading reviews...</div>
          <div v-else-if="reviewStore.error" class="state error">{{ reviewStore.error }}</div>
          <div v-else-if="reviewStore.professorReviews.length === 0" class="state empty">
            <p>No reviews yet for your courses.</p>
          </div>
          <div v-else class="review-groups">
            <section
              v-for="group in groupedProfessorReviews"
              :key="group.courseId"
              class="review-group"
            >
              <div class="review-group-header">
                <h3 class="review-group-title">{{ group.courseTitle }}</h3>
                <span class="review-group-count">
                  {{ group.reviews.length }} {{ group.reviews.length === 1 ? 'review' : 'reviews' }}
                </span>
              </div>
              <ul class="review-list">
                <li
                  v-for="review in group.reviews"
                  :key="review.id"
                  class="review-item"
                >
                  <div class="review-header">
                    <span class="review-stars">
                      <span
                        v-for="n in 5"
                        :key="n"
                        class="star"
                        :class="n <= review.rating ? 'star-filled' : 'star-empty'"
                      >&#9733;</span>
                    </span>
                    <time class="review-date">{{ formatDate(review.createdAt) }}</time>
                  </div>
                  <p class="review-author">{{ review.reviewerFirstName }} {{ review.reviewerLastName }}</p>
                  <p v-if="review.comment" class="review-comment">{{ review.comment }}</p>
                </li>
              </ul>
            </section>
          </div>
        </template>
      </template>
    </DashboardLayout>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DashboardLayout from '@/components/dashboard/DashboardLayout.vue'
import type { Course, Review } from '@/api'
import { useAuthStore } from '@/stores/authStore'
import { useCourseStore } from '@/stores/courseStore'
import { useReviewStore } from '@/stores/reviewStore'
import { extractApiErrorMessage } from '@/utils/apiErrors'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const courseStore = useCourseStore()
const reviewStore = useReviewStore()

const activeTab = ref<'courses' | 'reviews'>('courses')
const groupedProfessorReviews = computed(() => {
  const groups = new Map<number, { courseId: number; courseTitle: string; reviews: Review[] }>()

  for (const review of reviewStore.professorReviews) {
    const group = groups.get(review.courseId)

    if (group) {
      group.reviews.push(review)
      continue
    }

    groups.set(review.courseId, {
      courseId: review.courseId,
      courseTitle: review.courseTitle,
      reviews: [review],
    })
  }

  return Array.from(groups.values())
})

async function onReviewsTab() {
  activeTab.value = 'reviews'
  if (reviewStore.professorReviews.length === 0 && !reviewStore.loading) {
    await reviewStore.fetchProfessorReviews()
  }
}

const showCourseCreatedNotice = ref(route.query.courseCreated === '1')
const actionError = ref('')
let dismissTimer: ReturnType<typeof setTimeout> | null = null

const onCreateCourse = async () => {
  await router.push('/professor/courses/new')
}

const onEditCourse = async (uuid: Course['uuid']) => {
  actionError.value = ''
  await router.push(`/professor/courses/${encodeURIComponent(uuid)}/edit`)
}

const onDeleteCourse = async (uuid: Course['uuid']) => {
  actionError.value = ''
  const confirmed = window.confirm('Delete this course permanently? This cannot be undone.')
  if (!confirmed) return

  try {
    await courseStore.remove(uuid)
  } catch (e: unknown) {
    actionError.value = extractApiErrorMessage(e) ?? "We couldn't delete this course right now. Please try again."
  }
}

const dismissCourseCreatedNotice = async () => {
  showCourseCreatedNotice.value = false

  if (route.query.courseCreated === '1') {
    const nextQuery = { ...route.query }
    delete nextQuery.courseCreated
    await router.replace({ query: nextQuery })
  }
}

onMounted(async () => {
  if (showCourseCreatedNotice.value) {
    dismissTimer = setTimeout(() => {
      void dismissCourseCreatedNotice()
    }, 4000)
  }

  if (!authStore.session) {
    await authStore.fetchSession(true)
  }

  const professorId = authStore.session?.userId
  if (typeof professorId === 'number') {
    await courseStore.fetchAll({ professorId })
  }
})

onBeforeUnmount(() => {
  if (dismissTimer) {
    clearTimeout(dismissTimer)
  }
})

const formatDate = (isoDate: string) => {
  return new Date(isoDate).toLocaleDateString()
}

const formatDateTime = (isoDate: string) => {
  return new Date(isoDate).toLocaleString()
}

const toCourseHref = (link: string) => {
  return link.startsWith('www.') ? `https://${link}` : link
}
</script>

<style scoped>
.professor-dashboard {
  background: var(--color-bg-page);
}

.notice.success {
  max-width: 76rem;
  margin: 1rem auto 0;
  padding: 0.7rem 0.9rem;
  border-radius: 0.7rem;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  color: #166534;
  font-weight: 600;
}

.notice.error {
  max-width: 76rem;
  margin: 0.65rem auto 0;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #991b1b;
  border-radius: 0.7rem;
  padding: 0.7rem 0.9rem;
  font-weight: 600;
}

.notice-fade-enter-active,
.notice-fade-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.notice-fade-enter-from,
.notice-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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

.create-first {
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
}

.course-btn.edit {
  background: #e0f2fe;
  border-color: #bae6fd;
  color: #075985;
}

.course-btn.delete {
  background: #fee2e2;
  border-color: #fecaca;
  color: #991b1b;
}

.created-at {
  white-space: nowrap;
  color: var(--color-text-secondary);
  font-size: 0.84rem;
}

@media (max-width: 720px) {
  .course-item {
    flex-direction: column;
  }

  .created-at {
    align-self: flex-start;
  }
}

.tab-bar {
  display: flex;
  gap: 0.25rem;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 1rem;
}

.tab-btn {
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 0.5rem 0.9rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  margin-bottom: -1px;
  transition: color 0.15s, border-color 0.15s;
}

.tab-btn:hover {
  color: var(--color-text-primary);
}

.tab-btn.active {
  color: var(--color-brand-600);
  border-bottom-color: var(--color-brand-500);
}

.review-groups {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.review-group {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.review-group-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.75rem;
  padding-bottom: 0.35rem;
  border-bottom: 1px solid var(--color-border);
}

.review-group-title {
  margin: 0;
  font-size: 1rem;
  color: var(--color-text-primary);
}

.review-group-count {
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}

.review-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.review-item {
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  padding: 0.8rem 0.95rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.review-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.review-stars {
  display: flex;
  gap: 1px;
}

.star {
  font-size: 0.85rem;
}

.star-filled {
  color: #f59e0b;
}

.star-empty {
  color: var(--color-border);
}

.review-date {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin-left: auto;
}

.review-author {
  margin: 0;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
}

.review-comment {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text-primary);
  line-height: 1.55;
}

@media (max-width: 720px) {
  .review-group-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
