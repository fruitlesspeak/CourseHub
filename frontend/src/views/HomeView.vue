<template>
  <div class="home">
    <h1>Welcome to Vue + Spring Boot</h1>
    <nav>
      <router-link to="/">Home</router-link>
      <router-link to="/hello">Backend Test</router-link>
    </nav>
    <router-view />
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
nav {
  margin: 1rem 0;
}

nav a {
  margin-right: 1rem;
  text-decoration: none;
  color: #0066cc;
}

nav a:hover {
  text-decoration: underline;
}
</style>
