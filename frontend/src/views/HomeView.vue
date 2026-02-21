<template>
  <div class="container py-5 text-center">
    <h1 class="display-5 fw-bold mb-3">Welcome to CourseHub</h1>
    <p class="lead text-muted mb-4">Manage professors, students, and courses all in one place.</p>
    <div class="d-flex justify-content-center gap-3">
      <RouterLink to="/users" class="btn btn-danger btn-lg">
        <i class="bi bi-people me-2"></i>Manage Users
      </RouterLink>
      <RouterLink to="/courses" class="btn btn-outline-dark btn-lg">
        <i class="bi bi-book me-2"></i>Manage Courses
      </RouterLink>
    </div>
  </div>
</template>