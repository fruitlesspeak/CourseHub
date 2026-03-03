<template>
  <div class="auth-page">
    <div class="auth-card register-card" role="main" aria-label="Register">
      <div class="auth-brand" aria-hidden="true">
        <svg width="44" height="44" viewBox="0 0 24 24" fill="none">
          <path
            d="M4 19.5V6.6c0-.9.7-1.6 1.6-1.6H11c1 0 1.9.4 2.5 1.1.6-.7 1.5-1.1 2.5-1.1h5.4c.9 0 1.6.7 1.6 1.6V19.5c0 .6-.5 1-1.1.9-1.8-.4-4.2-.9-5.9-.9-1.9 0-3.2.4-4.5 1.1-1.3-.7-2.6-1.1-4.5-1.1-1.7 0-4.1.5-5.9.9-.6.1-1.1-.3-1.1-.9Z"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linejoin="round"
          />
          <path d="M12 6.4V20.6" stroke="currentColor" stroke-width="1.6" />
        </svg>
      </div>

      <h1 class="auth-title">Join CourseHub</h1>
      <p class="auth-subtitle">Create your account to get started</p>

      <form class="auth-form register-form" @submit.prevent="onSubmit">
        <div class="auth-field">
          <label class="auth-label" for="firstName">First Name</label>
          <input
            id="firstName"
            class="auth-input"
            v-model.trim="form.firstName"
            type="text"
            autocomplete="given-name"
            placeholder="John"
            required
          />
        </div>

        <div class="auth-field">
          <label class="auth-label" for="lastName">Last Name</label>
          <input
            id="lastName"
            class="auth-input"
            v-model.trim="form.lastName"
            type="text"
            autocomplete="family-name"
            placeholder="Doe"
            required
          />
        </div>

        <div class="auth-field">
          <label class="auth-label" for="email">Email</label>
          <input
            id="email"
            class="auth-input"
            v-model.trim="form.email"
            type="email"
            autocomplete="email"
            placeholder="you@example.com"
            required
          />
        </div>

        <div class="auth-field">
          <label class="auth-label" for="password">Password</label>
          <input
            id="password"
            class="auth-input"
            v-model="form.password"
            type="password"
            autocomplete="new-password"
            placeholder="********"
            minlength="8"
            required
          />
          <p class="register-hint">Minimum 8 characters.</p>
        </div>

        <div class="auth-field">
          <label class="auth-label" for="confirmPassword">Confirm Password</label>
          <input
            id="confirmPassword"
            class="auth-input"
            v-model="form.confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="********"
            minlength="8"
            required
          />
          <p class="register-hint">Re-enter your password.</p>
        </div>

        <div class="auth-field">
          <div class="auth-label">I am a</div>
          <div class="roleRow" role="radiogroup" aria-label="Role">
            <button
              type="button"
              class="roleBtn"
              :class="{ active: form.role === 'STUDENT' }"
              @click="form.role = 'STUDENT'"
              role="radio"
              :aria-checked="form.role === 'STUDENT'"
            >
              Student
            </button>
            <button
              type="button"
              class="roleBtn"
              :class="{ active: form.role === 'PROFESSOR' }"
              @click="form.role = 'PROFESSOR'"
              role="radio"
              :aria-checked="form.role === 'PROFESSOR'"
            >
              Professor
            </button>
          </div>
        </div>

        <p v-if="error" class="auth-notice error" role="alert">{{ error }}</p>

        <button class="auth-button" type="submit" :disabled="loading">
          <span class="btnIcon" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M15 21a6 6 0 0 0-12 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" stroke="currentColor" stroke-width="1.8"/>
              <path d="M19 8v6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              <path d="M16 11h6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
          </span>
          {{ loading ? "Creating..." : "Create Account" }}
        </button>

        <div class="auth-footer">
          <span>Already have an account?</span>
          <a class="link" href="/login">Sign in</a>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const router = useRouter();
const authStore = useAuthStore();
const loading = ref(false);
const error = ref("");

const form = reactive({
  firstName: "",
  lastName: "",
  email: "",
  password: "",
  confirmPassword: "",
  role: "STUDENT" as "STUDENT" | "PROFESSOR",
});

function validate() {
  if (!form.firstName) return "First name is required.";
  if (!form.lastName) return "Last name is required.";
  if (!form.email) return "Email is required.";
  if (!form.password || form.password.length < 8) return "Password must be at least 8 characters.";
  if (form.password !== form.confirmPassword) return "Passwords do not match.";
  if (!form.role) return "Role is required.";
  return "";
}

async function onSubmit() {
  error.value = validate();
  if (error.value) return;

  loading.value = true;
  try {
    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        firstName: form.firstName,
        lastName: form.lastName,
        email: form.email,
        password: form.password,
        role: form.role,
      }),
    });

    if (!response.ok) {
      if (response.status === 409) error.value = "Email already in use.";
      else if (response.status === 400) error.value = "Invalid input. Please check your fields.";
      else error.value = "Registration failed. Try again.";
      return;
    }

    const loginResponse = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({
        email: form.email,
        password: form.password,
      }),
    });

    if (!loginResponse.ok) {
      error.value = "Account created, but automatic sign-in failed. Please sign in.";
      await router.push({ path: "/login", query: { registered: "1" } });
      return;
    }

    // Body is not needed for routing; session is the source of truth.
    await loginResponse.json().catch(() => null);

    await authStore.fetchSession(true);
    await router.push(authStore.defaultDashboardPath);
  } catch {
    error.value = "Registration failed. Try again.";
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.register-card {
  width: min(32rem, 92vw);
}

.register-form {
  gap: 1rem;
}

.register-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}

.roleRow {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.875rem;
}

.roleBtn {
  height: 2.875rem;
  border-radius: 0.75rem;
  border: 2px solid var(--color-border);
  background: var(--color-bg-surface);
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.05s, border-color 0.15s, background 0.15s, box-shadow 0.15s, color 0.15s;
  color: var(--color-text-primary);
}

.roleBtn:active {
  transform: scale(0.99);
}

.roleBtn.active {
  border-color: var(--color-brand-500);
  background: rgba(79, 70, 229, 0.08);
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.1);
  color: var(--color-brand-500);
}

.btnIcon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.link {
  color: var(--color-brand-500);
  font-weight: 700;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>
