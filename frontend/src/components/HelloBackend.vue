<template>
  <div class="hello-backend">
    <h2>Backend Connection Test</h2>
    <button @click="handleFetch" :disabled="appStore.isLoading">
      {{ appStore.isLoading ? 'Loading...' : 'Fetch from Backend' }}
    </button>

    <div v-if="appStore.error" class="error">
      ❌ Error: {{ appStore.error }}
    </div>

    <div v-else-if="appStore.backendMessage" class="message">
      ✅ {{ appStore.backendMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '../stores/appStore.ts'

const appStore = useAppStore()

const apiUrl = computed(() => {
  return import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
})

const handleFetch = async () => {
  await appStore.fetchBackendMessage(apiUrl.value)
}
</script>

<style scoped>
.hello-backend {
  padding: 1rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  margin: 1rem 0;
}

button {
  padding: 0.5rem 1rem;
  background-color: #0066cc;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

button:hover:not(:disabled) {
  background-color: #0052a3;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #cc0000;
  margin-top: 1rem;
  padding: 0.5rem;
  background-color: #ffe6e6;
  border-radius: 4px;
}

.message {
  color: #006600;
  margin-top: 1rem;
  padding: 0.5rem;
  background-color: #e6ffe6;
  border-radius: 4px;
}
</style>
