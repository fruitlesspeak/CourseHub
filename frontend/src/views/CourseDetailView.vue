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

          <!-- Reviews section -->
          <section class="detail-section reviews-section">
            <h3>Reviews</h3>

            <!-- Submit form — enrolled students who haven't reviewed yet -->
            <form v-if="enrolled && !hasReviewed" class="review-form" @submit.prevent="handleSubmitReview">
              <p class="review-form-label">Leave a rating</p>
              <div class="star-input">
                <button
                  v-for="n in 5"
                  :key="n"
                  type="button"
                  class="star-btn"
                  :class="n <= (hoverRating || draftRating) ? 'star-filled' : 'star-empty'"
                  @mouseenter="hoverRating = n"
                  @mouseleave="hoverRating = 0"
                  @click="draftRating = n"
                  :aria-label="`Rate ${n} out of 5`"
                >★</button>
              </div>
              <textarea
                v-model="draftComment"
                class="review-textarea"
                placeholder="Share your experience (optional)"
                rows="3"
              ></textarea>
              <p v-if="reviewError" class="review-error">{{ reviewError }}</p>
              <button
                type="submit"
                class="review-submit-btn"
                :disabled="draftRating === 0 || submittingReview"
              >
                {{ submittingReview ? 'Submitting…' : 'Submit Review' }}
              </button>
            </form>

            <p v-else-if="enrolled && hasReviewed" class="review-already">
              You have already reviewed this course.
            </p>

            <!-- Reviews list -->
            <div v-if="reviewsLoading" class="review-loading">Loading reviews…</div>
            <div v-else-if="reviews.length === 0 && !enrolled" class="review-empty">
              No reviews yet.
            </div>
            <ul v-else-if="reviews.length > 0" class="review-list">
              <li v-for="review in reviews" :key="review.id" class="review-item">
                <div class="review-header">
                  <span class="review-author">{{ review.reviewerFirstName }} {{ review.reviewerLastName }}</span>
                  <span class="review-stars">
                    <span
                      v-for="n in 5"
                      :key="n"
                      class="star"
                      :class="n <= review.rating ? 'star-filled' : 'star-empty'"
                    >★</span>
                  </span>
                  <span class="review-date">{{ formatDate(review.createdAt) }}</span>
                </div>
                <p v-if="review.comment" class="review-comment">{{ review.comment }}</p>
              </li>
            </ul>
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
import { courseApi, enrollmentApi, reviewApi } from '@/api'
import type { Course, Review } from '@/api'
import { parseTags } from '@/stores/enrollmentStore'
import { extractApiErrorStatus } from '@/utils/apiErrors'

const route = useRoute()
const router = useRouter()

const course = ref<Course | null>(null)
const loading = ref(false)
const error = ref('')
const enrolled = ref(false)
const enrolling = ref(false)

const reviews         = ref<Review[]>([])
const reviewsLoading  = ref(false)
const hasReviewed     = ref(false)
const draftRating     = ref(0)
const hoverRating     = ref(0)
const draftComment    = ref('')
const submittingReview = ref(false)
const reviewError     = ref('')

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

  // Load reviews independently so a failure here doesn't block the page
  reviewsLoading.value = true
  try {
    const { data } = await reviewApi.getByCourse(uuid)
    reviews.value = data
    // Check if the current user already submitted a review
    // We rely on the server to reject duplicate submissions; hasReviewed is
    // a UX hint derived from the 409 response when submitting.
  } catch {
    // Non-fatal: reviews failing to load shouldn't block the course page
  } finally {
    reviewsLoading.value = false
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

async function handleSubmitReview() {
  if (!course.value || draftRating.value === 0) return
  submittingReview.value = true
  reviewError.value = ''
  try {
    const review = await reviewApi.submit(course.value.uuid, {
      rating: draftRating.value,
      comment: draftComment.value.trim() || undefined,
    })
    reviews.value.unshift(review.data)
    hasReviewed.value = true
    draftRating.value = 0
    draftComment.value = ''
  } catch (err: unknown) {
    const status = extractApiErrorStatus(err)
    if (status === 409) {
      hasReviewed.value = true
      reviewError.value = 'You have already reviewed this course.'
    } else if (status === 403) {
      reviewError.value = 'You must be enrolled in this course to leave a review.'
    } else {
      reviewError.value = "We couldn't submit your review. Please try again."
    }
  } finally {
    submittingReview.value = false
  }
}

const formatDateTime = (isoDate: string) => new Date(isoDate).toLocaleString()
const formatDate = (isoDate: string) => new Date(isoDate).toLocaleDateString()

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

/* ── Reviews ── */
.reviews-section { gap: 0.75rem; }

.review-form {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  padding: 0.85rem 1rem;
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border);
  border-radius: 0.65rem;
}
.review-form-label {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-primary);
}
.star-input { display: flex; gap: 4px; }
.star-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  transition: transform 0.1s;
}
.star-btn:hover { transform: scale(1.15); }
.star-btn.star-filled { color: #f59e0b; }
.star-btn.star-empty  { color: var(--color-border); }

.review-textarea {
  resize: vertical;
  border: 1px solid var(--color-border);
  border-radius: 0.5rem;
  padding: 0.6rem 0.75rem;
  font-size: 0.88rem;
  color: var(--color-text-primary);
  background: var(--color-bg-page);
  font-family: inherit;
  outline: none;
}
.review-textarea:focus {
  border-color: var(--color-brand-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-brand-500) 15%, transparent);
}
.review-error {
  margin: 0;
  font-size: 0.82rem;
  color: #b91c1c;
}
.review-submit-btn {
  align-self: flex-start;
  border: 0;
  border-radius: 0.55rem;
  padding: 0.45rem 1rem;
  background: var(--color-brand-500);
  color: #fff;
  font-weight: 600;
  font-size: 0.88rem;
  cursor: pointer;
}
.review-submit-btn:hover:not(:disabled) { background: var(--color-brand-600); }
.review-submit-btn:disabled { opacity: 0.65; cursor: not-allowed; }

.review-already {
  margin: 0;
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}
.review-loading {
  font-size: 0.85rem;
  color: var(--color-text-secondary);
}
.review-empty {
  font-size: 0.85rem;
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
  border-radius: 0.65rem;
  padding: 0.75rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.review-header {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}
.review-author {
  font-weight: 600;
  font-size: 0.88rem;
  color: var(--color-text-primary);
}
.review-stars { display: flex; gap: 1px; }
.star { font-size: 0.85rem; }
.star-filled { color: #f59e0b; }
.star-empty  { color: var(--color-border); }
.review-date {
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  margin-left: auto;
}
.review-comment {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text-primary);
  line-height: 1.55;
}
</style>
