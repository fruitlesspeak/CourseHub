<!--
  Created by AI.
  Prompt used: "can you implement this calendar or frontend in the current design? can you also keep frontend views and patterns similar to existing ones for courses?"
-->
<template>
  <div class="student-dashboard">
    <DashboardLayout
      role-label="Student"
      default-name="Student"
      title="My Dashboard"
      primary-action-text="Browse Courses"
      empty-message="No courses or due dates yet."
      empty-action-text="Browse Courses"
      @primary-action="onBrowseCourses"
    >
      <template #content>
        <div v-if="loadError" class="state error">{{ loadError }}</div>

        <div class="summary-grid">
          <article class="summary-card">
            <p class="summary-label">Courses</p>
            <strong class="summary-value">{{ courseStore.courses.length }}</strong>
          </article>

          <article class="summary-card">
            <p class="summary-label">Upcoming Due Dates</p>
            <strong class="summary-value">{{ upcomingItems.length }}</strong>
          </article>

          <article class="summary-card">
            <p class="summary-label">Important Dates</p>
            <strong class="summary-value">{{ importantDateStore.importantDates.length }}</strong>
          </article>
        </div>

        <div v-if="isLoading" class="state loading">Loading dashboard...</div>

        <div v-else class="dashboard-grid">
          <section class="panel">
            <div class="panel-head">
              <div>
                <h2>Upcoming Dates</h2>
                <p v-if="selectedCourse" class="panel-subtitle">
                  Showing dates for {{ selectedCourse.title }}. Click the course again to clear.
                </p>
              </div>
            </div>

            <div v-if="upcomingItems.length" class="item-list">
              <article v-for="item in upcomingItems" :key="item.id" class="item-card">
                <div class="item-main">
                  <h3>{{ item.title }}</h3>
                  <p class="item-code">{{ item.courseCode }}</p>
                  <p class="item-meta">{{ item.typeLabel }}</p>
                  <p class="item-meta">{{ formatDateTime(item.date) }}</p>
                  <p v-if="item.description" class="item-desc">{{ item.description }}</p>
                </div>
              </article>
            </div>

            <div v-else class="state empty">
              <p>No upcoming dates.</p>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>My Courses</h2>
            </div>

            <div v-if="courseStore.courses.length" class="item-list">
              <article
                v-for="course in courseStore.courses"
                :key="course.uuid"
                class="item-card course-card"
                :class="{ selected: selectedCourseId === course.id }"
              >
                <div class="item-main" @click="toggleCourseSelection(course.id)">
                  <h3>{{ course.title }}</h3>
                  <p class="item-code">{{ course.code }}</p>
                  <p v-if="course.description" class="item-desc">{{ course.description }}</p>
                </div>
                <div class="course-actions" @click.stop>
                  <button
                    type="button"
                    class="course-btn manage"
                    :aria-expanded="openMenuUuid === course.uuid"
                    @click="toggleManageMenu(course.uuid)"
                  >
                    Manage
                  </button>

                  <div v-if="openMenuUuid === course.uuid" class="course-menu">
                    <button
                      type="button"
                      class="course-menu-item"
                      @click="onViewCourse(course.uuid)"
                    >
                      View Course
                    </button>
                    <button
                      type="button"
                      class="course-menu-item drop"
                      :disabled="droppingUuids.has(course.uuid)"
                      @click="onDrop(course.uuid)"
                    >
                      {{ droppingUuids.has(course.uuid) ? 'Dropping...' : 'Drop Course' }}
                    </button>
                    <button
                      type="button"
                      class="course-menu-item placeholder"
                      disabled
                      title="Rating will be implemented later."
                    >
                      Post Rating (Coming Soon)
                    </button>
                  </div>
                </div>
              </article>
            </div>

            <div v-else class="state empty">
              <p>No enrolled courses yet.</p>
            </div>
          </section>
        </div>
      </template>
    </DashboardLayout>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { enrollmentApi, type Course, type ImportantDate } from '@/api'
import DashboardLayout from '@/components/dashboard/DashboardLayout.vue'
import { useCourseStore } from '@/stores/courseStore'
import { useImportantDateStore } from '@/stores/importantDateStore'

type DashboardItem = {
  id: string
  title: string
  courseId: number
  courseCode: string
  date: string
  description: string | null
  typeLabel: string
}

const router = useRouter()
const courseStore = useCourseStore()
const importantDateStore = useImportantDateStore()
const selectedCourseId = ref<number | null>(null)
const droppingUuids = ref<Set<string>>(new Set())
const openMenuUuid = ref<string | null>(null)

const isLoading = computed(() => courseStore.loading || importantDateStore.loading)
const loadError = computed(() => courseStore.error ?? importantDateStore.error)
const selectedCourse = computed(() =>
  courseStore.courses.find((course) => course.id === selectedCourseId.value) ?? null,
)

const importantDateItems = computed<DashboardItem[]>(() => {
  return importantDateStore.importantDates.map((importantDate) => {
    const course = courseStore.courses.find((item) => item.id === importantDate.courseId)
    return toImportantDateItem(importantDate, course)
  })
})

const upcomingItems = computed<DashboardItem[]>(() => {
  const now = Date.now()
  const selectedId = selectedCourseId.value

  return importantDateItems.value
    .filter((item) => selectedId === null || item.courseId === selectedId)
    .filter((item) => new Date(item.date).getTime() >= now)
    .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime())
    .slice(0, 8)
})

const onBrowseCourses = async () => {
  await router.push({ name: 'student-catalog' })
}

const onViewCourse = async (uuid: string) => {
  openMenuUuid.value = null
  await router.push({ name: 'CourseDetail', params: { uuid }, query: { from: 'dashboard' } })
}

const toggleManageMenu = (uuid: string) => {
  openMenuUuid.value = openMenuUuid.value === uuid ? null : uuid
}

const onDrop = async (uuid: string) => {
  if (droppingUuids.value.has(uuid)) return

  const confirmed = window.confirm('Drop this course?')
  if (!confirmed) return

  openMenuUuid.value = null

  const nextDropping = new Set(droppingUuids.value)
  nextDropping.add(uuid)
  droppingUuids.value = nextDropping

  try {
    await enrollmentApi.drop(uuid)
    courseStore.courses = courseStore.courses.filter((course) => course.uuid !== uuid)
    selectedCourseId.value =
      courseStore.courses.some((course) => course.id === selectedCourseId.value)
        ? selectedCourseId.value
        : null
    await importantDateStore.fetchByCourses(courseStore.courses.map((course: Course) => course.id))
  } catch (e: unknown) {
    courseStore.error = extractError(e) ?? 'Failed to drop course.'
  } finally {
    const updatedDropping = new Set(droppingUuids.value)
    updatedDropping.delete(uuid)
    droppingUuids.value = updatedDropping
  }
}

onMounted(async () => {
  courseStore.loading = true
  courseStore.error = null
  try {
    const { data } = await enrollmentApi.getMyCourses()
    courseStore.courses = data
    await importantDateStore.fetchByCourses(data.map((course: Course) => course.id))
  } catch (e: unknown) {
    courseStore.courses = []
    importantDateStore.importantDates = []
    courseStore.error = extractError(e) ?? 'Failed to load courses.'
  } finally {
    courseStore.loading = false
  }
})

function toggleCourseSelection(courseId: number): void {
  selectedCourseId.value = selectedCourseId.value === courseId ? null : courseId
}

function toImportantDateItem(importantDate: ImportantDate, course?: Course): DashboardItem {
  return {
    id: `important-${importantDate.id}`,
    title: importantDate.title,
    courseId: importantDate.courseId,
    courseCode: course?.code ?? `Course #${importantDate.courseId}`,
    date: importantDate.dueAt,
    description: importantDate.description,
    typeLabel: 'Important Date',
  }
}

function formatDateTime(isoDate: string): string {
  return new Date(isoDate).toLocaleString()
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
.student-dashboard {
  background: var(--color-bg-page);
}

.summary-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.summary-card,
.panel {
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  background: var(--color-bg-surface);
}

.summary-card {
  padding: 0.9rem 1rem;
}

.summary-label {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 0.85rem;
}

.summary-value {
  display: block;
  margin-top: 0.35rem;
  font-size: 1.65rem;
}

.dashboard-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 1rem;
}

.panel-head {
  padding: 0.9rem 1rem;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.panel-head h2 {
  margin: 0;
  font-size: 1.2rem;
}

.panel-subtitle {
  margin: 0.3rem 0 0;
  color: var(--color-text-secondary);
  font-size: 0.88rem;
}

.item-list {
  display: grid;
  gap: 0.75rem;
  padding: 1rem;
}

.item-card {
  border: 1px solid var(--color-border);
  border-radius: 0.7rem;
  padding: 0.85rem 0.95rem;
  background: var(--color-bg-surface);
}

.course-card {
  text-align: left;
  display: flex;
  justify-content: space-between;
  gap: 0.75rem;
  align-items: flex-start;
}

.course-card.selected {
  border-color: var(--color-brand-500);
  background: #eef2ff;
}

.item-main h3 {
  margin: 0;
  font-size: 1rem;
}

.item-code {
  margin: 0.2rem 0 0;
  color: var(--color-brand-600);
  font-weight: 700;
  font-size: 0.88rem;
}

.item-meta {
  margin: 0.35rem 0 0;
  color: var(--color-text-secondary);
  font-size: 0.9rem;
}

.item-desc {
  margin: 0.45rem 0 0;
  color: var(--color-text-secondary);
}

.course-actions {
  position: relative;
  flex-shrink: 0;
}

.course-btn {
  border-radius: 0.55rem;
  padding: 0.35rem 0.7rem;
  border: 1px solid transparent;
  font-weight: 600;
  cursor: pointer;
}

.course-btn.manage {
  background: var(--color-bg-soft);
  border-color: var(--color-border);
  color: var(--color-text-primary);
}

.course-menu {
  position: absolute;
  top: calc(100% + 0.35rem);
  right: 0;
  min-width: 12rem;
  border: 1px solid var(--color-border);
  border-radius: 0.7rem;
  background: var(--color-bg-surface);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  padding: 0.35rem;
  display: grid;
  gap: 0.2rem;
  z-index: 5;
}

.course-menu-item {
  width: 100%;
  border: 0;
  background: transparent;
  border-radius: 0.55rem;
  padding: 0.55rem 0.7rem;
  text-align: left;
  font-size: 0.9rem;
  color: var(--color-text-primary);
  cursor: pointer;
}

.course-menu-item:hover:not(:disabled) {
  background: var(--color-bg-soft);
}

.course-menu-item.drop {
  color: #991b1b;
}

.course-menu-item.drop:hover:not(:disabled) {
  background: #fef2f2;
}

.course-menu-item.placeholder {
  color: var(--color-text-secondary);
}

.course-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.course-menu-item:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.state {
  margin-top: 1rem;
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
  margin: 1rem;
  border: 1px dashed var(--color-border);
  color: var(--color-text-secondary);
}

@media (max-width: 860px) {
  .summary-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .course-card {
    flex-direction: column;
  }
}
</style>
