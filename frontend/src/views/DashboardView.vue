<template>
  <DashboardLayout
    role-label="Professor"
    default-name="Professor"
    title="My Courses"
    primary-action-text="Create New Course"
    empty-message="You haven't created any courses yet."
    empty-action-text="Create Your First Course"
    @primary-action="onCreateCourse"
  />
</template>

<script setup lang="ts">
import DashboardLayout from "@/components/dashboard/DashboardLayout.vue";

const onCreateCourse = () => {
  window.alert("Create course flow coming next.");
};
</script>
