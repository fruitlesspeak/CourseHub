<template>
  <div class="container-fluid py-4">

    <!-- ── Toolbar ── -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-3 mb-4">
      <div class="d-flex align-items-center gap-3 flex-wrap">
        <h2 class="mb-0 fw-bold">Courses</h2>
        <div class="input-group input-group-sm" style="width: 220px;">
          <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
          <input
            type="text" class="form-control border-start-0"
            v-model="searchQuery" placeholder="Search by title…"
          />
        </div>
      </div>
      <button class="btn btn-danger" @click="openCreate">
        <i class="bi bi-plus-lg me-1"></i> Add Course
      </button>
    </div>

    <!-- ── Error ── -->
    <div v-if="courseStore.error" class="alert alert-danger alert-dismissible" role="alert">
      {{ courseStore.error }}
      <button type="button" class="btn-close" @click="courseStore.error = null"></button>
    </div>

    <!-- ── Cards ── -->
    <div v-if="courseStore.loading" class="text-center py-5 text-muted">
      <div class="spinner-border spinner-border-sm me-2"></div> Loading…
    </div>
    <div v-else-if="courseStore.courses.length" class="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-4">
      <div v-for="c in courseStore.courses" :key="c.uuid" class="col">
        <div class="card h-100 shadow-sm">
          <div class="card-body d-flex flex-column">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <span class="fs-4">📖</span>
              <div class="d-flex gap-1">
                <button class="btn btn-sm btn-outline-secondary" @click="openEdit(c)" title="Edit">
                  <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" @click="confirmDelete(c)" title="Delete">
                  <i class="bi bi-trash"></i>
                </button>
              </div>
            </div>
            <h5 class="card-title fw-semibold">{{ c.title }}</h5>
            <p class="card-text text-muted small flex-grow-1 course-desc">
              {{ c.description ?? 'No description provided.' }}
            </p>
            <div class="d-flex justify-content-between align-items-center mt-3 pt-2 border-top small text-muted">
              <span>👨‍🏫 {{ professorName(c.professorId) }}</span>
              <span>{{ formatDate(c.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="card shadow-sm">
      <div class="text-center py-5 text-muted">No courses found.</div>
    </div>

    <!-- ── Create / Edit Modal ── -->
    <template v-if="showModal">
      <div class="modal fade show d-block" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content">

            <div class="modal-header">
              <h5 class="modal-title">{{ editTarget ? 'Edit Course' : 'Add Course' }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>

            <div class="modal-body">
              <form id="courseForm" @submit.prevent="submitForm">

                <div class="mb-3">
                  <label class="form-label">Title <span class="text-danger">*</span></label>
                  <input class="form-control" v-model="form.title" maxlength="255" required />
                </div>

                <div class="mb-3">
                  <label class="form-label">Description</label>
                  <textarea class="form-control" v-model="form.description" rows="3"
                            placeholder="Optional course description…"></textarea>
                </div>

                <div class="mb-3">
                  <label class="form-label">Professor <span class="text-danger">*</span></label>
                  <select class="form-select" v-model="form.professorId" required>
                    <option value="">Select a professor…</option>
                    <option v-for="p in userStore.professors" :key="p.id" :value="String(p.id)">
                      {{ p.firstName }} {{ p.lastName }}
                    </option>
                  </select>
                  <div v-if="!userStore.professors.length" class="form-text text-warning">
                    No professors found. Add a professor first.
                  </div>
                </div>

                <div v-if="formError" class="alert alert-danger mb-0 py-2">{{ formError }}</div>
              </form>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" @click="closeModal">Cancel</button>
              <button type="submit" form="courseForm" class="btn btn-danger" :disabled="submitting">
                <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
                {{ submitting ? 'Saving…' : (editTarget ? 'Save Changes' : 'Create Course') }}
              </button>
            </div>

          </div>
        </div>
      </div>
      <div class="modal-backdrop fade show"></div>
    </template>

    <!-- ── Delete Confirm Modal ── -->
    <template v-if="deleteTarget">
      <div class="modal fade show d-block" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered modal-sm">
          <div class="modal-content">
            <div class="modal-header border-0">
              <h5 class="modal-title">Delete Course</h5>
              <button type="button" class="btn-close" @click="deleteTarget = null"></button>
            </div>
            <div class="modal-body pt-0">
              <p class="mb-0">
                Delete <strong>{{ deleteTarget.title }}</strong>? This cannot be undone.
              </p>
            </div>
            <div class="modal-footer border-0">
              <button class="btn btn-secondary" @click="deleteTarget = null">Cancel</button>
              <button class="btn btn-danger" @click="doDelete" :disabled="submitting">
                <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
                {{ submitting ? 'Deleting…' : 'Delete' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-backdrop fade show"></div>
    </template>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useCourseStore } from '../stores/courseStore'
import { useUserStore }   from '../stores/userStore'
import type { Course } from '../api/index'

interface CourseForm {
  title:       string
  description: string
  professorId: string
}

const courseStore = useCourseStore()
const userStore   = useUserStore()

const searchQuery  = ref('')
const showModal    = ref(false)
const editTarget   = ref<Course | null>(null)
const deleteTarget = ref<Course | null>(null)
const submitting   = ref(false)
const formError    = ref<string | null>(null)

const blankForm = (): CourseForm => ({ title: '', description: '', professorId: '' })
const form = ref<CourseForm>(blankForm())

const professorName = (id: number) => {
  const p = userStore.professors.find(p => p.id === id)
  return p ? `${p.firstName} ${p.lastName}` : `Prof. #${id}`
}

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(searchQuery, (val) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => courseStore.fetchAll(val ? { title: val } : {}), 350)
})

onMounted(async () => {
  if (!userStore.professors.length) await userStore.fetchAll(true)
  courseStore.fetchAll()
})

function openCreate() {
  editTarget.value = null; form.value = blankForm()
  formError.value = null;  showModal.value = true
}

function openEdit(c: Course) {
  editTarget.value = c
  form.value = { title: c.title, description: c.description ?? '', professorId: String(c.professorId) }
  formError.value = null; showModal.value = true
}

function closeModal() { showModal.value = false; editTarget.value = null }

async function submitForm() {
  submitting.value = true; formError.value = null
  try {
    const professorId = Number(form.value.professorId)
    if (editTarget.value) {
      await courseStore.update(editTarget.value.uuid, {
        title: form.value.title,
        description: form.value.description || undefined,
        professorId,
      })
    } else {
      await courseStore.create({
        title: form.value.title,
        description: form.value.description || undefined,
        professorId,
      })
    }
    closeModal()
  } catch (e: unknown) {
    formError.value = extractError(e) ?? 'An error occurred.'
  } finally {
    submitting.value = false
  }
}

function confirmDelete(c: Course) { deleteTarget.value = c }

async function doDelete() {
  if (!deleteTarget.value) return
  submitting.value = true
  try {
    await courseStore.remove(deleteTarget.value.uuid)
    deleteTarget.value = null
  } catch (e: unknown) {
    courseStore.error = extractError(e) ?? 'Delete failed.'
    deleteTarget.value = null
  } finally {
    submitting.value = false
  }
}

const formatDate = (d: string) => new Date(d).toLocaleDateString()

function extractError(e: unknown): string | null {
  if (e && typeof e === 'object' && 'response' in e) {
    const resp = (e as { response?: { data?: { error?: string } } }).response
    return resp?.data?.error ?? null
  }
  return null
}
</script>

<style scoped>
.course-desc {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>