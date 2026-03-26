<template>
  <article class="course-card" :class="{ 'is-enrolled': enrolled }">
    <!-- Color band -->
    <div class="card-band" :style="{ background: bandColor }" aria-hidden="true">
      <span class="card-initial">{{ initial }}</span>
      <span v-if="enrolled" class="enrolled-pip">
        <svg viewBox="0 0 12 12" fill="none" width="9" height="9">
          <path d="M2 6.5l3 3 5-5" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        Enrolled
      </span>
    </div>

    <div class="card-body">
      <!-- Title + code -->
      <div class="card-header-row">
        <h3 class="card-title">{{ course.title }}</h3>
        <span v-if="course.code" class="course-code">{{ course.code }}</span>
      </div>

      <!-- Description -->
      <p class="card-desc">{{ truncated }}</p>

      <!-- Tags (parsed from comma-separated string) -->
      <div v-if="tags.length" class="card-tags">
        <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
      </div>

      <!-- Due date -->
      <p v-if="course.dueDate" class="card-due">
        <svg viewBox="0 0 16 16" fill="none" width="12" height="12" aria-hidden="true">
          <rect x="2" y="3" width="12" height="11" rx="2" stroke="currentColor" stroke-width="1.4"/>
          <path d="M5 2v2M11 2v2M2 7h12" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/>
        </svg>
        Due {{ formattedDue }}
      </p>

      <!-- Action buttons -->
      <div class="card-footer">
        <template v-if="enrolled">
          <button class="btn btn-enrolled" disabled>
            <svg viewBox="0 0 16 16" fill="none" width="13" height="13" aria-hidden="true">
              <path d="M3 8.5l3.5 3.5 6.5-7" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            Enrolled
          </button>
          <router-link
            :to="{ name: 'CourseDetail', params: { uuid: course.uuid }, query: { from: 'catalog' } }"
            class="btn btn-secondary"
          >
            View course
          </router-link>
        </template>

        <button
          v-else
          class="btn btn-primary"
          :disabled="enrolling"
          @click.stop="$emit('enroll', course.uuid)"
        >
          <span v-if="enrolling" class="spinner" aria-hidden="true"></span>
          <svg v-else viewBox="0 0 16 16" fill="none" width="13" height="13" aria-hidden="true">
            <path d="M8 3v10M3 8h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
          {{ enrolling ? 'Enrolling…' : 'Enroll' }}
        </button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Course } from '@/api'
import { parseTags } from '@/stores/enrollmentStore'

const props = defineProps<{
  course:   Course
  enrolled: boolean
  enrolling: boolean
}>()

defineEmits<{ enroll: [uuid: string] }>()

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

const bandColor = computed(() => BANDS[(props.course.id ?? 0) % BANDS.length])
const initial   = computed(() => (props.course.title ?? '?')[0]?.toUpperCase() ?? '?')
const tags      = computed(() => parseTags(props.course.tags))

const truncated = computed(() => {
  const d = props.course.description ?? ''
  return d.length > 100 ? d.slice(0, 97) + '…' : d
})

const formattedDue = computed(() => {
  if (!props.course.dueDate) return ''
  return new Date(props.course.dueDate).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', year: 'numeric',
  })
})
</script>

<style scoped>
.course-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.85rem;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.18s, transform 0.18s;
}
.course-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.course-card.is-enrolled {
  border-color: color-mix(in srgb, var(--color-brand-500) 35%, transparent);
}

/* ── Band ── */
.card-band {
  height: 5rem;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.card-initial {
  font-size: 2.8rem;
  font-weight: 800;
  color: rgba(255,255,255,0.18);
  user-select: none;
  line-height: 1;
}
.enrolled-pip {
  position: absolute;
  top: 0.5rem;
  right: 0.6rem;
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.18rem 0.5rem;
  border-radius: 999px;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

/* ── Body ── */
.card-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 0.3rem;
}
.card-header-row {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
  flex-wrap: wrap;
}
.card-title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.3;
}
.course-code {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-bg-soft);
  border: 1px solid var(--color-border-muted);
  border-radius: 4px;
  padding: 0.1rem 0.4rem;
  white-space: nowrap;
}
.card-desc {
  margin: 0.1rem 0 0;
  font-size: 0.81rem;
  color: var(--color-text-secondary);
  line-height: 1.5;
  flex: 1;
}

/* ── Tags ── */
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.28rem;
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

/* ── Due date ── */
.card-due {
  margin: 0.1rem 0 0;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

/* ── Footer / buttons ── */
.card-footer {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.8rem;
}
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  height: 2.15rem;
  padding: 0 0.9rem;
  border-radius: var(--radius-md, 0.65rem);
  font-size: 0.83rem;
  font-weight: 600;
  cursor: pointer;
  border: 0;
  flex: 1;
  transition: background 0.14s;
}
.btn-primary {
  background: var(--color-brand-500);
  color: #fff;
}
.btn-primary:hover:not(:disabled) { background: var(--color-brand-600); }
.btn-primary:disabled { opacity: 0.65; cursor: not-allowed; }

.btn-enrolled {
  background: var(--color-bg-soft);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border-muted);
  cursor: default;
}
.btn-secondary {
  background: var(--color-bg-soft);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  text-decoration: none;
}
.btn-secondary:hover { background: var(--color-border); }

.spinner {
  width: 0.8rem;
  height: 0.8rem;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>