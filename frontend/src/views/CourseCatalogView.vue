<template>
  <DashboardLayout
    role-label="Student"
    title="Course Catalog"
    primary-action-text="Refresh"
    empty-message="No courses available yet."
    empty-action-text="Clear search"
    default-name="Student"
    @primary-action="onRefresh"
  >
    <template #content>
      <!-- ── Back link ── -->
      <router-link :to="{ name: 'student-dashboard' }" class="back-link">
        ← Back to Dashboard
      </router-link>

      <!-- ── Search row ── -->
      <div class="search-row">
        <div class="search-field">
          <svg class="search-icon" viewBox="0 0 16 16" fill="none" width="14" height="14" aria-hidden="true">
            <circle cx="6.5" cy="6.5" r="4" stroke="currentColor" stroke-width="1.5"/>
            <path d="M10.5 10.5L14 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <input
            v-model="tagInput"
            type="text"
            class="search-input"
            placeholder="Filter by tag — e.g. Java, Spring…"
            :disabled="store.loading"
            @keyup.enter="submitSearch"
            aria-label="Filter courses by tag"
          />
          <button
            v-if="activeTag"
            class="clear-btn"
            type="button"
            @click="clearSearch"
            aria-label="Clear filter"
          >
            <svg viewBox="0 0 12 12" fill="none" width="10" height="10">
              <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <button
          class="action-btn"
          type="button"
          :disabled="store.loading"
          @click="submitSearch"
        >
          <span v-if="store.loading" class="spinner" aria-hidden="true"></span>
          <svg v-else viewBox="0 0 16 16" fill="none" width="14" height="14" aria-hidden="true">
            <circle cx="6.5" cy="6.5" r="4" stroke="currentColor" stroke-width="1.5"/>
            <path d="M10.5 10.5L14 14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          Search
        </button>
      </div>

      <!-- Active filter pill -->
      <div v-if="activeTag" class="filter-pill" role="status" aria-live="polite">
        <svg viewBox="0 0 16 16" fill="none" width="12" height="12" aria-hidden="true">
          <path d="M2 4h12M4 8h8M6 12h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        Showing results for <strong>{{ activeTag }}</strong>
        <button type="button" @click="clearSearch" aria-label="Remove filter">
          <svg viewBox="0 0 12 12" fill="none" width="9" height="9">
            <path d="M2 2l8 8M10 2l-8 8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <!-- Error banner -->
      <div v-if="error" class="inline-error" role="alert">
        <svg viewBox="0 0 16 16" fill="none" width="14" height="14" aria-hidden="true">
          <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.4"/>
          <path d="M8 5v3.5M8 10.5v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        {{ error }}
        <button type="button" @click="error = null" aria-label="Dismiss">×</button>
      </div>

      <!-- Skeleton loading -->
      <div v-if="store.loading" class="course-grid" aria-busy="true">
        <div v-for="n in 6" :key="n" class="skeleton-card">
          <div class="sk sk-band"></div>
          <div class="sk-body">
            <div class="sk sk-title"></div>
            <div class="sk sk-line"></div>
            <div class="sk sk-line short"></div>
            <div class="sk-tags">
              <div class="sk sk-tag"></div>
              <div class="sk sk-tag"></div>
            </div>
            <div class="sk sk-btn"></div>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div
        v-else-if="store.courses.length === 0"
        class="empty-state"
        role="status"
      >
        <svg viewBox="0 0 48 48" fill="none" width="42" height="42" aria-hidden="true">
          <rect x="6" y="10" width="36" height="28" rx="4" stroke="currentColor" stroke-width="1.8"/>
          <path d="M15 22h18M15 29h10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
        </svg>
        <p v-if="activeTag">No courses match <strong>"{{ activeTag }}"</strong></p>
        <p v-else>No courses are available right now.</p>
        <button v-if="activeTag" class="action-btn" type="button" @click="clearSearch">
          View all courses
        </button>
      </div>

      <!-- Course grid -->
      <div v-else class="course-grid">
        <CourseCard
          v-for="course in store.courses"
          :key="course.uuid"
          :course="course"
          :enrolled="store.isEnrolled(course.uuid)"
          :enrolling="store.isEnrolling(course.uuid)"
          @enroll="handleEnroll"
        />
      </div>
    </template>
  </DashboardLayout>

  <!-- Toast — teleported outside layout so it's never clipped -->
  <Teleport to="body">
    <Transition name="toast">
      <div
        v-if="toast.visible"
        class="toast"
        :class="toast.variant"
        role="status"
        aria-live="polite"
      >
        <svg v-if="toast.variant === 'success'" viewBox="0 0 16 16" fill="none" width="14" height="14">
          <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.4"/>
          <path d="M5 8l2.5 2.5 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <svg v-else viewBox="0 0 16 16" fill="none" width="14" height="14">
          <circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.4"/>
          <path d="M8 5v3.5M8 10.5v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        {{ toast.message }}
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useEnrollmentStore } from '@/stores/enrollmentStore'
import DashboardLayout from '@/components/dashboard/DashboardLayout.vue'
import CourseCard from '@/views/CourseCardView.vue'

const store = useEnrollmentStore()

const tagInput  = ref('')
const activeTag = ref('')
const error     = ref<string | null>(null)
const toast     = ref({ visible: false, message: '', variant: 'success', _t: 0 as ReturnType<typeof setTimeout> })

onMounted(async () => {
  // Load enrolled state first so cards render correctly on first paint
  await Promise.all([
    loadCourses(),
    store.fetchMyEnrollments(),
  ])
})

async function loadCourses(tag?: string) {
  error.value = null
  try {
    await store.fetchCourses(tag ? { tag } : {})
  } catch (err: any) {
    const status = err?.response?.status
    if (status === 401) {
      error.value = 'You must be logged in to browse courses.'
    } else {
      error.value = 'Failed to load courses. Please try again.'
    }
  }
}

function submitSearch() {
  const tag = tagInput.value.trim()
  activeTag.value = tag
  loadCourses(tag || undefined)
}

function clearSearch() {
  tagInput.value  = ''
  activeTag.value = ''
  loadCourses()
}

function onRefresh() {
  loadCourses(activeTag.value || undefined)
}

async function handleEnroll(courseUuid: string) {
  error.value = null
  try {
    await store.enroll(courseUuid)
    showToast('Enrolled! You now have access to course materials.', 'success')
  } catch (err: any) {
    const status = err?.response?.status
    if (status === 409) {
      // Already enrolled — just sync local state silently
      store.enrolledUuids.add(courseUuid)
      showToast('You are already enrolled in this course.', 'info')
    } else if (status === 401) {
      error.value = 'Please log in to enroll in courses.'
    } else if (status === 403) {
      showToast('You are not authorized to enroll in this course.', 'error')
    } else if (status === 404) {
      showToast('Course not found — it may have been removed.', 'error')
    } else {
      showToast('Enrollment failed. Please try again.', 'error')
    }
  }
}

function showToast(message: string, variant: 'success' | 'error' | 'info') {
  clearTimeout(toast.value._t)
  toast.value = {
    visible: true,
    message,
    variant,
    _t: setTimeout(() => { toast.value.visible = false }, 4000),
  }
}
</script>

<style scoped>
/* ── Back link ── */
.back-link {
  display: inline-block;
  margin-bottom: 0.85rem;
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 0.88rem;
}
.back-link:hover { color: var(--color-text-primary); }

/* ── Search row ── */
.search-row {
  display: flex;
  gap: 0.6rem;
  margin-bottom: 0.75rem;
}

.search-field {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-secondary);
  pointer-events: none;
}

.search-input {
  width: 100%;
  height: 2.6rem;
  padding: 0 2.2rem 0 2.2rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.65rem);
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.search-input:focus {
  border-color: var(--color-brand-500);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-brand-500) 15%, transparent);
}
.search-input::placeholder { color: var(--color-text-secondary); }
.search-input:disabled      { opacity: 0.55; }

.clear-btn {
  position: absolute;
  right: 0.65rem;
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  padding: 2px;
  border-radius: 4px;
}
.clear-btn:hover { color: var(--color-text-primary); }

/* Mirrors .action-btn from DashboardLayout */
.action-btn {
  border: 0;
  border-radius: 0.65rem;
  background: var(--color-brand-500);
  color: #fff;
  height: 2.6rem;
  padding: 0 1.1rem;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  white-space: nowrap;
  flex-shrink: 0;
}
.action-btn:hover:not(:disabled) { background: var(--color-brand-600); }
.action-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ── Filter pill ── */
.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.8rem;
  color: var(--color-text-secondary);
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border-muted);
  border-radius: 999px;
  padding: 0.25rem 0.65rem;
  margin-bottom: 0.9rem;
}
.filter-pill strong { color: var(--color-text-primary); }
.filter-pill button {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-secondary);
  display: flex;
  padding: 0;
  margin-left: 0.1rem;
}
.filter-pill button:hover { color: var(--color-text-primary); }

/* ── Inline error ── */
.inline-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.84rem;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: var(--radius-md, 0.65rem);
  padding: 0.6rem 0.85rem;
  margin-bottom: 1rem;
}
.inline-error button {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: #b91c1c;
  font-size: 1.1rem;
  line-height: 1;
  padding: 0;
}

/* ── Course grid ── */
.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(17rem, 1fr));
  gap: 1rem;
  margin-top: 0.5rem;
}

/* ── Empty state ── */
.empty-state {
  min-height: 14rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  color: var(--color-text-secondary);
  text-align: center;
}
.empty-state p { margin: 0; font-size: 0.9rem; }
.empty-state strong { color: var(--color-text-primary); }

/* ── Skeleton ── */
.skeleton-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.85rem;
  overflow: hidden;
}
.sk-body { padding: 1rem; display: flex; flex-direction: column; gap: 0.5rem; }
.sk {
  background: linear-gradient(
    90deg,
    var(--color-bg-soft) 25%,
    var(--color-border)   50%,
    var(--color-bg-soft) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: 4px;
}
.sk-band         { height: 5rem; border-radius: 0; }
.sk-title        { height: 15px; width: 70%; }
.sk-line         { height: 11px; width: 88%; }
.sk-line.short   { width: 52%; }
.sk-tags         { display: flex; gap: 0.3rem; }
.sk-tag          { height: 17px; width: 48px; border-radius: 999px; }
.sk-btn          { height: 32px; border-radius: 0.6rem; margin-top: 0.35rem; }
@keyframes shimmer {
  0%   { background-position:  200% 0; }
  100% { background-position: -200% 0; }
}

/* ── Button spinner ── */
.spinner {
  width: 0.8rem;
  height: 0.8rem;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Toast ── */
.toast {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.7rem 1.1rem;
  border-radius: 0.7rem;
  font-size: 0.84rem;
  font-weight: 500;
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  z-index: 9999;
  max-width: 22rem;
}
.toast.success { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.toast.error   { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.toast.info    { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; }

.toast-enter-active, .toast-leave-active { transition: opacity 0.2s, transform 0.2s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(6px); }

/* ── Responsive ── */
@media (max-width: 640px) {
  .search-row    { flex-direction: column; }
  .action-btn    { width: 100%; justify-content: center; }
  .course-grid   { grid-template-columns: 1fr; }
}
</style>