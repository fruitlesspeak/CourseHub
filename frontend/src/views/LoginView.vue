<template>
  <section class="auth-page" aria-label="Login">
    <div class="auth-card">
      <div class="auth-brand" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none">
          <path
            d="M4 6.75a1.75 1.75 0 0 1 1.75-1.75h4.1c.87 0 1.7.35 2.3.96.6-.61 1.43-.96 2.3-.96h4.1A1.75 1.75 0 0 1 20.3 6.75V17.5a.5.5 0 0 1-.76.43l-2.83-1.71a2.5 2.5 0 0 0-1.29-.36h-.92c-.66 0-1.28.26-1.75.73a.5.5 0 0 1-.7 0 2.46 2.46 0 0 0-1.75-.73h-.92a2.5 2.5 0 0 0-1.29.36L4.76 17.93A.5.5 0 0 1 4 17.5V6.75Zm8.5 7.37c.62-.33 1.32-.5 2.03-.5h.92c.64 0 1.26.13 1.85.38V6.75a.5.5 0 0 0-.5-.5h-4.1c-.66 0-1.25.34-1.6.86v7.01Zm-1 0V7.1a1.8 1.8 0 0 0-1.6-.85h-4.1a.5.5 0 0 0-.5.5V14c.58-.25 1.2-.38 1.85-.38h.92c.72 0 1.41.17 2.03.5Z"
            fill="currentColor"
          />
        </svg>
      </div>
      <h1 class="auth-title">Welcome to CourseHub</h1>
      <p class="auth-subtitle">Sign in to continue your learning journey</p>

      <p v-if="showRegisteredNotice" class="auth-notice success" role="status">
        Registration complete. You can sign in now.
      </p>

      <form class="auth-form" @submit.prevent="onSubmit">
        <label class="auth-field">
          <span>Email</span>
          <input
            v-model.trim="form.email"
            class="auth-input"
            type="email"
            autocomplete="email"
            required
            placeholder="you@example.com"
          />
        </label>

        <label class="auth-field">
          <span>Password</span>
          <input
            v-model="form.password"
            class="auth-input"
            type="password"
            autocomplete="current-password"
            required
            placeholder="********"
          />
        </label>

        <p v-if="errorMessage" class="auth-notice error" role="alert">{{ errorMessage }}</p>

        <button class="auth-button" type="submit" :disabled="isLoading">
          <span class="button-icon" aria-hidden="true">&rarr;</span>
          {{ isLoading ? 'Signing in...' : 'Sign In' }}
        </button>
      </form>

      <p class="auth-footer">
        Don't have an account?
        <RouterLink to="/register">Sign up</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  email: '',
  password: '',
})

const isLoading = ref(false)
const errorMessage = ref('')

const showRegisteredNotice = computed(() => route.query.registered === '1')

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
      credentials: 'include',
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

    // Body is not needed for routing; session is the source of truth.
    await response.json().catch(() => null)

    await authStore.fetchSession(true)

    const redirectTarget = typeof route.query.redirect === 'string' ? route.query.redirect : null
    if (redirectTarget && redirectTarget.startsWith('/')) {
      await router.push(redirectTarget)
      return
    }

    await router.push(authStore.defaultDashboardPath)
  } catch {
    errorMessage.value = 'Unable to sign in right now. Please try again.'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.button-icon {
  font-size: 1.1rem;
}
</style>
