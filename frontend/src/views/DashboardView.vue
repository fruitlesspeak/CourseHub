<template>
  <div class="professor-dashboard">
    <p v-if="showCourseCreatedNotice" class="notice success" role="status">Course Created</p>

    <DashboardLayout
      role-label="Professor"
      default-name="Professor"
      title="My Courses"
      primary-action-text="Create New Course"
      empty-message="You haven't created any courses yet."
      empty-action-text="Create Your First Course"
      @primary-action="onCreateCourse"
    >
      <template #content>
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
            </div>
            <time class="created-at">{{ formatDate(course.createdAt) }}</time>
          </article>
        </div>

        <div v-else class="state empty">
          <p>You haven't created any courses yet.</p>
          <button class="create-first" type="button" @click="onCreateCourse">Create Your First Course</button>
        </div>
      </template>
    </DashboardLayout>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DashboardLayout from '@/components/dashboard/DashboardLayout.vue'
import { useAuthStore } from '@/stores/authStore'
import { useCourseStore } from '@/stores/courseStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const courseStore = useCourseStore()

const showCourseCreatedNotice = computed(() => route.query.courseCreated === '1')

const onCreateCourse = async () => {
  await router.push('/professor/courses/new')
}

onMounted(async () => {
  if (!authStore.session) {
    await authStore.fetchSession(true)
  }

  const professorId = authStore.session?.userId
  if (typeof professorId === 'number') {
    await courseStore.fetchAll({ professorId })
  }
})

const formatDate = (isoDate: string) => {
  return new Date(isoDate).toLocaleDateString()
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
</style>
