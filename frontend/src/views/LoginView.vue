<template>
  <section class="auth-layout" aria-label="Login">
    <div class="auth-card">
      <div class="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M4 6.75a1.75 1.75 0 0 1 1.75-1.75h4.1c.87 0 1.7.35 2.3.96.6-.61 1.43-.96 2.3-.96h4.1A1.75 1.75 0 0 1 20.3 6.75V17.5a.5.5 0 0 1-.76.43l-2.83-1.71a2.5 2.5 0 0 0-1.29-.36h-.92c-.66 0-1.28.26-1.75.73a.5.5 0 0 1-.7 0 2.46 2.46 0 0 0-1.75-.73h-.92a2.5 2.5 0 0 0-1.29.36L4.76 17.93A.5.5 0 0 1 4 17.5V6.75Zm8.5 7.37c.62-.33 1.32-.5 2.03-.5h.92c.64 0 1.26.13 1.85.38V6.75a.5.5 0 0 0-.5-.5h-4.1c-.66 0-1.25.34-1.6.86v7.01Zm-1 0V7.1a1.8 1.8 0 0 0-1.6-.85h-4.1a.5.5 0 0 0-.5.5V14c.58-.25 1.2-.38 1.85-.38h.92c.72 0 1.41.17 2.03.5Z"
            fill="currentColor"
          />
        </svg>
      </div>
      <h1>Welcome to CourseHub</h1>
      <p class="subtitle">Sign in to continue your learning journey</p>

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
            placeholder="********"
          />
        </label>

        <p v-if="errorMessage" class="notice error" role="alert">{{ errorMessage }}</p>

        <button type="submit" :disabled="isLoading">
          <span class="button-icon" aria-hidden="true">&rarr;</span>
          {{ isLoading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>

      <p class="signup">
        Don't have an account?
        <RouterLink to="/register">Sign up</RouterLink>
      </p>
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
  padding: 2rem 1rem 3rem;
  background: #dde3f1;
}

.auth-card {
  width: min(25.5rem, 100%);
  background: #ffffff;
  border: 1px solid #e7ebf3;
  border-radius: 1rem;
  box-shadow: 0 14px 30px rgba(35, 46, 71, 0.12);
  padding: 1.9rem 1.7rem 1.6rem;
}

.brand-mark {
  display: grid;
  place-items: center;
  margin-bottom: 1.1rem;
}

.brand-mark svg {
  width: 2.15rem;
  height: 2.15rem;
  color: #4b45ea;
}

h1 {
  margin: 0;
  text-align: center;
  font-size: 1.65rem;
  font-weight: 700;
  color: #222a3a;
}

.subtitle {
  margin: 0.45rem 0 1.5rem;
  text-align: center;
  color: #5d6880;
  font-size: 1rem;
}

.form {
  display: grid;
  gap: 0.95rem;
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field span {
  font-size: 0.95rem;
  font-weight: 600;
  color: #37445b;
}

input {
  height: 3rem;
  border-radius: 0.7rem;
  border: 1px solid #d2d9e5;
  padding: 0 0.9rem;
  font-size: 1rem;
  color: #1f2937;
  background: #fff;
}

input::placeholder {
  color: #9aa6bb;
}

input:focus {
  outline: 2px solid rgba(85, 77, 241, 0.18);
  border-color: #574bf1;
}

button {
  margin-top: 0.4rem;
  height: 3rem;
  border: 0;
  border-radius: 0.7rem;
  background: linear-gradient(90deg, #4c40ea 0%, #4f45ee 50%, #4a3be7 100%);
  color: #ffffff;
  font-weight: 600;
  font-size: 1.25rem;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.button-icon {
  font-size: 1.1rem;
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

.signup {
  margin: 1.35rem 0 0;
  text-align: center;
  color: #5f6d84;
}

.signup a {
  color: #4b45ea;
  font-weight: 700;
  text-decoration: none;
}

.signup a:hover {
  text-decoration: underline;
}
</style>

