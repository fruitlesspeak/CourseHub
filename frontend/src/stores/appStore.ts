import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAppStore = defineStore('app', () => {
  const backendMessage = ref<string>('Loading...')
  const isLoading = ref<boolean>(false)
  const error = ref<string | null>(null)

  const setMessage = (msg: string) => {
    backendMessage.value = msg
  }

  const setLoading = (loading: boolean) => {
    isLoading.value = loading
  }

  const setError = (err: string | null) => {
    error.value = err
  }

  const fetchBackendMessage = async (apiUrl: string) => {
    setLoading(true)
    setError(null)
    try {
      const res = await fetch(`${apiUrl}/api/hello`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.text()
      setMessage(data)
    } catch (e) {
      const errorMsg = e instanceof Error ? e.message : 'Unknown error'
      setError(errorMsg)
      setMessage('Failed to load')
    } finally {
      setLoading(false)
    }
  }

  const messageStatus = computed(() => ({
    message: backendMessage,
    isLoading,
    error
  }))

  return {
    backendMessage,
    isLoading,
    error,
    setMessage,
    setLoading,
    setError,
    fetchBackendMessage,
    messageStatus
  }
})
