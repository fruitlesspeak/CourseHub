<template>
  <DashboardLayout
    role-label="Student"
    default-name="Student"
    title="My Courses"
    primary-action-text="Enroll Courses"
    empty-message="You haven't enrolled in any courses yet."
    empty-action-text="Explore Courses"
    @primary-action="onExploreCourses"
  />
</template>

<script setup lang="ts">
import DashboardLayout from "@/components/dashboard/DashboardLayout.vue";

const onExploreCourses = () => {
  window.alert("Course enrollment flow coming next.");
};
</script>
