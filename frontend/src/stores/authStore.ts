import { computed, ref } from "vue";
import { defineStore } from "pinia";

export type AuthRole = "STUDENT" | "PROFESSOR";

export type AuthSession = {
  userId: number;
  name: string;
  email: string;
  role: AuthRole;
  dashboardPath: string;
};

export const useAuthStore = defineStore("auth", () => {
  const session = ref<AuthSession | null>(null);
  const initialized = ref(false);
  const isLoading = ref(false);

  const isAuthenticated = computed(() => session.value !== null);

  const defaultDashboardPath = computed(() => {
    if (!session.value) return "/login";

    if (typeof session.value.dashboardPath === "string" && session.value.dashboardPath.startsWith("/")) {
      return session.value.dashboardPath;
    }

    const basePath = session.value.role === "PROFESSOR" ? "/professor/dashboard" : "/student/dashboard";
    return `${basePath}/${encodeURIComponent(String(session.value.userId))}`;
  });

  const fetchSession = async (force = false) => {
    if (initialized.value && !force) {
      return session.value;
    }

    isLoading.value = true;
    try {
      const response = await fetch("/api/auth/session", {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        session.value = null;
        return null;
      }

      const data = (await response.json()) as AuthSession;
      session.value = data;
      return data;
    } catch {
      session.value = null;
      return null;
    } finally {
      initialized.value = true;
      isLoading.value = false;
    }
  };

  const clearSession = () => {
    session.value = null;
    initialized.value = true;
  };

  const logout = async () => {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        credentials: "include",
      });
    } finally {
      clearSession();
    }
  };

  return {
    session,
    initialized,
    isLoading,
    isAuthenticated,
    defaultDashboardPath,
    fetchSession,
    clearSession,
    logout,
  };
});
