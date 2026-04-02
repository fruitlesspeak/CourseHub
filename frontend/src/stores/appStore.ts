import { computed, ref } from "vue";
import { defineStore } from "pinia";

export const useAppStore = defineStore("app", () => {
  const backendMessage = ref<string>("Loading...");
  const isLoading = ref<boolean>(false);
  const error = ref<string | null>(null);

  const setMessage = (msg: string) => {
    backendMessage.value = msg;
  };

  const setLoading = (loading: boolean) => {
    isLoading.value = loading;
  };

  const setError = (err: string | null) => {
    error.value = err;
  };

  const fetchBackendMessage = async (apiUrl: string) => {
    setLoading(true);
    setError(null);
    try {
      const normalizedApiBase = apiUrl.endsWith("/api") ? apiUrl : `${apiUrl}/api`;
      const res = await fetch(`${normalizedApiBase}/hello`, {
        credentials: "include",
      });
      if (!res.ok) throw new Error("server_unavailable");
      const data = await res.text();
      setMessage(data);
    } catch (e) {
      const errorMsg =
        e instanceof Error && e.message === "server_unavailable"
          ? "We couldn't reach the server. Please try again."
          : "Something went wrong while contacting the server. Please try again.";
      setError(errorMsg);
      setMessage("Unable to load data right now.");
    } finally {
      setLoading(false);
    }
  };

  const messageStatus = computed(() => ({
    message: backendMessage,
    isLoading,
    error,
  }));

  return {
    backendMessage,
    isLoading,
    error,
    setMessage,
    setLoading,
    setError,
    fetchBackendMessage,
    messageStatus,
  };
});
