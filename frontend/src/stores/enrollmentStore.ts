// src/stores/enrollmentStore.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { courseApi, enrollmentApi } from '../api/index.ts'
import type { Course } from '@/api'


/** Tags come back as a comma-separated string — split for display */
export function parseTags(raw: string | null): string[] {
    if (!raw) return []
    return raw.split(',').map((t) => t.trim()).filter(Boolean)
}

export const useEnrollmentStore = defineStore('enrollment', () => {
    const courses = ref<Course[]>([])
    const enrolledUuids = ref<Set<string>>(new Set())
    const loading = ref(false)
    const enrollingUuids = ref<Set<string>>(new Set())

    const enrolledCount = computed(() => enrolledUuids.value.size)

    // ── Actions ────────────────────────────────────────────────────────


    async function fetchCourses(params: { tag?: string; title?: string } = {}): Promise<void> {
        loading.value = true
        try {
            const { data } = await courseApi.getAll(params)
            courses.value = data
        } finally {
            loading.value = false
        }
    }

    async function fetchMyEnrollments(): Promise<void> {
        try {
            const { data } = await enrollmentApi.getMyCourses()
            enrolledUuids.value = new Set(data.map((course: Course) => course.uuid))
        } catch {
            // Non-fatal: if this fails the worst case is stale enrolled state
        }
    }

    async function enroll(courseUuid: string): Promise<void> {
        if (enrollingUuids.value.has(courseUuid)) return
        enrollingUuids.value.add(courseUuid)
        try {
            await enrollmentApi.enroll(courseUuid)
            enrolledUuids.value.add(courseUuid)
        } finally {
            enrollingUuids.value.delete(courseUuid)
        }
    }

    function isEnrolled(courseUuid: string): boolean {
        return enrolledUuids.value.has(courseUuid)
    }

    function isEnrolling(courseUuid: string): boolean {
        return enrollingUuids.value.has(courseUuid)
    }

    return {
        courses,
        enrolledUuids,
        enrolledCount,
        loading,
        fetchCourses,
        fetchMyEnrollments,
        enroll,
        isEnrolled,
        isEnrolling,
    }
})