<template>
  <div class="app">
    <header class="app-header">
      <RouterLink to="/" class="app-logo">🎓 CourseHub</RouterLink>
      <nav>
        <RouterLink to="/users"   class="nav-link">👥 Users</RouterLink>
        <RouterLink to="/courses" class="nav-link">📚 Courses</RouterLink>
      </nav>
    </header>

    <main class="app-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup lang="ts">
// RouterView and RouterLink are globally available when using vue-router
</script>

<style>
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: 'Inter', system-ui, sans-serif;
  background: #f0f2f5;
  color: #1a1a2e;
  min-height: 100vh;
}

.app-header {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  color: white;
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  gap: 2rem;
  box-shadow: 0 2px 12px rgba(0,0,0,.3);
  position: sticky;
  top: 0;
  z-index: 100;
}

.app-logo {
  font-size: 1.4rem;
  font-weight: 700;
  color: white;
  text-decoration: none;
  white-space: nowrap;
}

nav { display: flex; gap: .5rem; }

.nav-link {
  padding: .45rem 1.2rem;
  border: 2px solid rgba(255,255,255,.25);
  background: transparent;
  color: rgba(255,255,255,.75);
  border-radius: 8px;
  font-size: .9rem;
  font-weight: 500;
  text-decoration: none;
  transition: all .2s;
}
.nav-link:hover           { border-color: rgba(255,255,255,.6); color: white; }
.nav-link.router-link-active {
  background: #e94560;
  border-color: #e94560;
  color: white;
}

.app-main {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}
</style>