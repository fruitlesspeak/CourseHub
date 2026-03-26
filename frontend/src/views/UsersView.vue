<template>
  <div class="container-fluid py-4">

    <!-- ── Toolbar ── -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-3 mb-4">
      <div class="d-flex align-items-center gap-3 flex-wrap">
        <h2 class="mb-0 fw-bold">Users</h2>
        <div class="btn-group" role="group">
          <button
            v-for="f in filters" :key="f.value"
            type="button"
            :class="['btn btn-sm', roleFilter === f.value ? 'btn-dark' : 'btn-outline-dark']"
            @click="setFilter(f.value)"
          >{{ f.label }}</button>
        </div>
      </div>
      <button class="btn btn-danger" @click="openCreate">
        <i class="bi bi-plus-lg me-1"></i> Add User
      </button>
    </div>

    <!-- ── Error ── -->
    <div v-if="store.error" class="alert alert-danger alert-dismissible" role="alert">
      {{ store.error }}
      <button type="button" class="btn-close" @click="store.error = null"></button>
    </div>

    <!-- ── Table ── -->
    <div class="card shadow-sm">
      <div v-if="store.loading" class="text-center py-5 text-muted">
        <div class="spinner-border spinner-border-sm me-2"></div> Loading…
      </div>
      <div v-else-if="store.users.length" class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Student ID</th>
              <th>Created</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in store.users" :key="u.uuid">
              <td>
                <div class="d-flex align-items-center gap-2">
                  <div class="avatar-circle">{{ initials(u) }}</div>
                  <span class="fw-medium">{{ u.firstName }} {{ u.lastName }}</span>
                </div>
              </td>
              <td class="text-muted">{{ u.email }}</td>
              <td>
                <span :class="['badge rounded-pill', u.isProfessor ? 'bg-primary' : 'bg-success']">
                  {{ u.isProfessor ? 'Professor' : 'Student' }}
                </span>
              </td>
              <td class="text-muted">{{ u.studentId ?? '—' }}</td>
              <td class="text-muted">{{ formatDate(u.createdAt) }}</td>
              <td class="text-end">
                <button class="btn btn-sm btn-outline-secondary me-1" @click="openEdit(u)" title="Edit">
                  <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" @click="confirmDelete(u)" title="Delete">
                  <i class="bi bi-trash"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="text-center py-5 text-muted">No users found.</div>
    </div>

    <!-- ── Create / Edit Modal ── -->
    <template v-if="showModal">
      <div class="modal fade show d-block" tabindex="-1">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content">

            <div class="modal-header">
              <h5 class="modal-title">{{ editTarget ? 'Edit User' : 'Add User' }}</h5>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>

            <div class="modal-body">
              <form id="userForm" @submit.prevent="submitForm">

                <div class="row g-3">
                  <div class="col-6">
                    <label class="form-label">First Name <span class="text-danger">*</span></label>
                    <input class="form-control" v-model="form.firstName" required />
                  </div>
                  <div class="col-6">
                    <label class="form-label">Last Name <span class="text-danger">*</span></label>
                    <input class="form-control" v-model="form.lastName" required />
                  </div>
                </div>

                <div class="mt-3">
                  <label class="form-label">Email <span class="text-danger">*</span></label>
                  <input type="email" class="form-control" v-model="form.email" required />
                </div>

                <div class="mt-3" v-if="!editTarget">
                  <label class="form-label">Password <span class="text-danger">*</span></label>
                  <input type="password" class="form-control" v-model="form.password"
                         placeholder="Min 8 characters" minlength="8" required />
                </div>

                <div class="mt-3">
                  <label class="form-label d-block">Role</label>
                  <div class="btn-group w-100" role="group">
                    <button type="button"
                      :class="['btn', !form.isProfessor ? 'btn-dark' : 'btn-outline-dark']"
                      @click="form.isProfessor = false; form.studentId = ''">
                      Student
                    </button>
                    <button type="button"
                      :class="['btn', form.isProfessor ? 'btn-dark' : 'btn-outline-dark']"
                      @click="form.isProfessor = true; form.studentId = null">
                      Professor
                    </button>
                  </div>
                </div>

                <template v-if="!form.isProfessor">
                  <div class="mt-3">
                    <label class="form-label">Student ID</label>
                    <input class="form-control" v-model="(form.studentId as string)"
                           placeholder="e.g. S123456" />
                  </div>
                  <div class="mt-3">
                    <label class="form-label">Supervising Professor</label>
                    <select class="form-select" v-model="form.professorId">
                      <option value="">None</option>
                      <option v-for="p in store.professors" :key="p.id" :value="String(p.id)">
                        {{ p.firstName }} {{ p.lastName }}
                      </option>
                    </select>
                  </div>
                </template>

                <div v-if="formError" class="alert alert-danger mt-3 mb-0 py-2">{{ formError }}</div>
              </form>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" @click="closeModal">Cancel</button>
              <button type="submit" form="userForm" class="btn btn-danger" :disabled="submitting">
                <span v-if="submitting" class="spinner-border spinner-border-sm me-1"></span>
                {{ submitting ? 'Saving…' : (editTarget ? 'Save Changes' : 'Create User') }}
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
              <h5 class="modal-title">Delete User</h5>
              <button type="button" class="btn-close" @click="deleteTarget = null"></button>
            </div>
            <div class="modal-body pt-0">
              <p class="mb-0">
                Delete <strong>{{ deleteTarget.firstName }} {{ deleteTarget.lastName }}</strong>?
                This cannot be undone.
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
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/userStore'
import type { User } from '../api/index'

interface UserForm {
  firstName:   string
  lastName:    string
  email:       string
  password:    string
  isProfessor: boolean
  professorId: string | null
  studentId:   string | null
}

const store = useUserStore()

const roleFilter   = ref<string>('all')
const showModal    = ref(false)
const editTarget   = ref<User | null>(null)
const deleteTarget = ref<User | null>(null)
const submitting   = ref(false)
const formError    = ref<string | null>(null)

const blankForm = (): UserForm => ({
  firstName: '', lastName: '', email: '', password: '',
  isProfessor: false, professorId: null, studentId: '',
})
const form = ref<UserForm>(blankForm())

const filters = [
  { value: 'all',       label: 'All' },
  { value: 'professor', label: 'Professors' },
  { value: 'student',   label: 'Students' },
]

function setFilter(value: string) {
  roleFilter.value = value
  const map: Record<string, boolean | undefined> = { all: undefined, professor: true, student: false }
  store.fetchAll(map[value])
}

onMounted(() => store.fetchAll())

function openCreate() {
  editTarget.value = null; form.value = blankForm()
  formError.value = null;  showModal.value = true
}

function openEdit(u: User) {
  editTarget.value = u
  form.value = {
    firstName:   u.firstName,
    lastName:    u.lastName,
    email:       u.email,
    password:    '',
    isProfessor: u.isProfessor,
    professorId: u.professorId ? String(u.professorId) : null,
    studentId:   u.studentId ?? '',
  }
  formError.value = null; showModal.value = true
}

function closeModal() { showModal.value = false; editTarget.value = null }

async function submitForm() {
  submitting.value = true; formError.value = null
  try {
    const professorId = form.value.professorId ? Number(form.value.professorId) : null
    const studentId   = form.value.studentId || null

    if (editTarget.value) {
      await store.update(editTarget.value.uuid, {
        email: form.value.email, firstName: form.value.firstName,
        lastName: form.value.lastName, isProfessor: form.value.isProfessor,
        professorId, studentId,
      })
    } else {
      await store.create({
        email: form.value.email, password: form.value.password,
        firstName: form.value.firstName, lastName: form.value.lastName,
        isProfessor: form.value.isProfessor, professorId, studentId,
      })
    }
    closeModal()
  } catch (e: unknown) {
    formError.value = extractError(e) ?? 'An error occurred.'
  } finally {
    submitting.value = false
  }
}

function confirmDelete(u: User) { deleteTarget.value = u }

async function doDelete() {
  if (!deleteTarget.value) return
  submitting.value = true
  try {
    await store.remove(deleteTarget.value.uuid)
    deleteTarget.value = null
  } catch (e: unknown) {
    store.error = extractError(e) ?? 'Delete failed.'
    deleteTarget.value = null
  } finally {
    submitting.value = false
  }
}

const initials   = (u: User) => `${u.firstName?.[0] ?? ''}${u.lastName?.[0] ?? ''}`.toUpperCase()
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
.avatar-circle {
  width: 34px; height: 34px; border-radius: 50%;
  background: linear-gradient(135deg, #dc3545, #a71d2a);
  color: white; font-size: .75rem; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
</style>