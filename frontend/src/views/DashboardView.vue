<template>
  <section class="dashboard">
    <h1>Dashboard</h1>
    <p v-if="userId">Signed in as user: {{ userId }}</p>
    <p v-else>Signed in successfully. This is your dashboard landing page.</p>
    <router-link to="/">Back to home</router-link>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const userId = computed(() => {
  const id = route.params.userId
  return typeof id === 'string' ? id : ''
})
</script>

<style scoped>
.dashboard {
  max-width: 48rem;
  margin: 2.5rem auto;
  padding: 0 1rem;
  color: #0f172a;
}

h1 {
  margin-bottom: 0.75rem;
}

a {
  color: #4f46e5;
}
</style>
