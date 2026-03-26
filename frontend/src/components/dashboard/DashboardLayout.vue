<template>
  <div class="dashboard-page">
    <header class="topbar">
      <div class="brand">
        <span class="brand-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <path
              d="M4 6.75a1.75 1.75 0 0 1 1.75-1.75h4.1c.87 0 1.7.35 2.3.96.6-.61 1.43-.96 2.3-.96h4.1A1.75 1.75 0 0 1 20.3 6.75V17.5a.5.5 0 0 1-.76.43l-2.83-1.71a2.5 2.5 0 0 0-1.29-.36h-.92c-.66 0-1.28.26-1.75.73a.5.5 0 0 1-.7 0 2.46 2.46 0 0 0-1.75-.73h-.92a2.5 2.5 0 0 0-1.29.36L4.76 17.93A.5.5 0 0 1 4 17.5V6.75Z"
              fill="currentColor"
            />
          </svg>
        </span>
        <span class="brand-text">CourseHub</span>
      </div>

      <div class="top-right">
        <div class="profile" aria-label="Profile">
          <div class="avatar">{{ initials }}</div>
          <div class="meta">
            <strong>{{ displayName }}</strong>
            <span>{{ roleLabel }}</span>
          </div>
        </div>
        <button class="logout" type="button" @click="onLogout">Logout</button>
      </div>
    </header>

    <main class="content">
      <section class="courses-card" aria-label="My Courses">
        <div class="courses-head">
          <h1>{{ title }}</h1>
          <button class="action-btn" type="button" @click="$emit('primaryAction')">
            {{ primaryActionText }}
          </button>
        </div>

        <slot name="content">
          <div class="empty-state">
            <p>{{ emptyMessage }}</p>
            <button class="action-btn secondary" type="button" @click="$emit('primaryAction')">
              {{ emptyActionText }}
            </button>
          </div>
        </slot>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/authStore";

const props = defineProps<{
  roleLabel: string;
  title: string;
  primaryActionText: string;
  emptyMessage: string;
  emptyActionText: string;
  defaultName: string;
}>();

defineEmits<{
  primaryAction: [];
}>();

const router = useRouter();
const authStore = useAuthStore();

const displayName = computed(() => {
  if (authStore.session) {
    const first = authStore.session.firstName?.trim() ?? "";
    const last = authStore.session.lastName?.trim() ?? "";
    const full = `${first} ${last}`.trim();
    if (full) return full;
  }

  return props.defaultName;
});

const initials = computed(() => {
  const parts = displayName.value.split(/\s+/).filter(Boolean);
  if (parts.length === 0) return props.defaultName.charAt(0).toUpperCase();
  return parts
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
});

const onLogout = async () => {
  await authStore.logout();
  await router.push("/login");
};
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
}

.topbar {
  height: 4rem;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.55rem;
}

.brand-icon {
  width: 1.6rem;
  height: 1.6rem;
  color: var(--color-brand-500);
}

.brand-icon svg {
  width: 100%;
  height: 100%;
}

.brand-text {
  font-size: 1.5rem;
  font-weight: 700;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 0.9rem;
}

.profile {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 999px;
  background: var(--color-bg-soft);
  color: var(--color-brand-600);
  font-weight: 700;
  display: grid;
  place-items: center;
  font-size: 0.84rem;
}

.meta {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.meta strong {
  font-size: 0.82rem;
}

.meta span {
  font-size: 0.76rem;
  color: var(--color-text-secondary);
}

.logout {
  border: 1px solid var(--color-border-muted);
  background: var(--color-bg-surface);
  color: #374151;
  border-radius: var(--radius-md);
  height: 2.15rem;
  padding: 0 0.9rem;
  font-weight: 600;
  cursor: pointer;
}

.logout:hover {
  background: #f9fafb;
}

.content {
  max-width: 76rem;
  margin: 0 auto;
  padding: 1.7rem 1rem 2rem;
}

.courses-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.85rem;
  min-height: 26rem;
  padding: 1.4rem;
}

.courses-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

h1 {
  margin: 0;
  font-size: 1.75rem;
}

.action-btn {
  border: 0;
  border-radius: 0.65rem;
  background: var(--color-brand-500);
  color: #ffffff;
  height: 2.6rem;
  padding: 0 1.1rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}

.action-btn:hover {
  background: var(--color-brand-600);
}

.empty-state {
  min-height: 18rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 1rem;
  color: var(--color-text-secondary);
  text-align: center;
}

.secondary {
  height: 2.8rem;
}

@media (max-width: 760px) {
  .topbar {
    height: auto;
    padding: 0.8rem 1rem;
    flex-direction: column;
    align-items: flex-start;
    gap: 0.7rem;
  }

  .top-right {
    width: 100%;
    justify-content: space-between;
  }

  .courses-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-btn {
    width: 100%;
  }
}
</style>
