<template>
  <section class="auth-layout" aria-label="Login">
    <div class="auth-card">
      <h1>Sign in to CourseHub</h1>
      <p class="subtitle">Use your registered email and password.</p>

      <p v-if="showRegisteredNotice" class="notice success" role="status">
        Registration complete. You can sign in now.
      </p>

      <form class="form" @submit.prevent="onSubmit">
        <label class="field">
          <span>Email</span>
          <input
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            required
            placeholder="you@example.com"
          />
        </label>

        <label class="field">
          <span>Password</span>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            required
            placeholder="••••••••"
          />
        </label>

        <p v-if="errorMessage" class="notice error" role="alert">{{ errorMessage }}</p>

        <button type="submit" :disabled="isLoading">
          {{ isLoading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

type LoginResponse = {
  dashboardPath?: string
  userId?: string | number
  role?: 'STUDENT' | 'PROFESSOR'
}

const router = useRouter()
const route = useRoute()

const form = reactive({
  email: '',
  password: '',
})

const isLoading = ref(false)
const errorMessage = ref('')

const showRegisteredNotice = computed(() => route.query.registered === '1')

const resolveDashboardPath = (response: LoginResponse): string => {
  if (typeof response.dashboardPath === 'string' && response.dashboardPath.startsWith('/')) {
    return response.dashboardPath
  }

  if (response.userId !== undefined && response.userId !== null) {
    return `/dashboard/${encodeURIComponent(String(response.userId))}`
  }

  return '/dashboard'
}

const onSubmit = async () => {
  errorMessage.value = ''

  if (!form.email || !form.password) {
    errorMessage.value = 'Email and password are required.'
    return
  }

  isLoading.value = true

  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: form.email,
        password: form.password,
      }),
    })

    if (!response.ok) {
      if (response.status === 401) {
        errorMessage.value = 'Invalid email or password.'
      } else if (response.status === 400) {
        errorMessage.value = 'Please verify your input and try again.'
      } else {
        errorMessage.value = 'Unable to sign in right now. Please try again.'
      }
      return
    }

    let data: LoginResponse = {}
    try {
      data = (await response.json()) as LoginResponse
    } catch {
      data = {}
    }

    await router.push(resolveDashboardPath(data))
  } catch {
    errorMessage.value = 'Unable to sign in right now. Please try again.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 2rem 1rem;
  background: linear-gradient(180deg, #f8f9fc 0%, #edf2ff 100%);
}

.auth-card {
  width: min(30rem, 100%);
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.08);
  padding: 1.5rem;
}

h1 {
  margin: 0;
  font-size: 1.5rem;
  color: #0f172a;
}

.subtitle {
  margin: 0.5rem 0 1.25rem;
  color: #475569;
}

.form {
  display: grid;
  gap: 0.9rem;
}

.field {
  display: grid;
  gap: 0.4rem;
}

.field span {
  font-size: 0.9rem;
  font-weight: 600;
  color: #1f2937;
}

input {
  height: 2.6rem;
  border-radius: 0.6rem;
  border: 1px solid #cbd5e1;
  padding: 0 0.75rem;
  font-size: 0.95rem;
}

input:focus {
  outline: 2px solid rgba(79, 70, 229, 0.2);
  border-color: #4f46e5;
}

button {
  margin-top: 0.35rem;
  height: 2.6rem;
  border: 0;
  border-radius: 0.6rem;
  background: #4f46e5;
  color: #ffffff;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.notice {
  margin: 0;
  border-radius: 0.5rem;
  padding: 0.6rem 0.7rem;
  font-size: 0.86rem;
}

.success {
  background: #ecfdf3;
  border: 1px solid #9be8bf;
  color: #0f5132;
}

.error {
  background: #fff1f2;
  border: 1px solid #ffc7cd;
  color: #9f1239;
}
</style>
