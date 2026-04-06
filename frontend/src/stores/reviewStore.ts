import { defineStore } from 'pinia'
import { ref } from 'vue'
import { reviewApi } from '@/api'
import type { Review, CreateReviewPayload } from '@/api'

export const useReviewStore = defineStore('review', () => {
  const professorReviews = ref<Review[]>([])
  const loading          = ref(false)
  const error            = ref<string | null>(null)

  // ── Actions ──────────────────────────────────────────────────────────────

  async function fetchProfessorReviews(): Promise<void> {
    loading.value = true
    error.value   = null
    try {
      const { data } = await reviewApi.getMyProfessorReviews()
      professorReviews.value = data
    } catch {
      error.value = "We couldn't load your reviews right now. Please try again."
    } finally {
      loading.value = false
    }
  }

  async function submitReview(courseUuid: string, payload: CreateReviewPayload): Promise<Review> {
    const { data } = await reviewApi.submit(courseUuid, payload)
    return data
  }

  return {
    professorReviews,
    loading,
    error,
    fetchProfessorReviews,
    submitReview,
  }
})
