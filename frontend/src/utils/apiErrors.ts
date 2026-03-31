type ErrorResponseData = {
  error?: string
  message?: string
  [key: string]: unknown
}

type ErrorResponse = {
  status?: number
  data?: unknown
}

type ErrorWithResponse = {
  response?: ErrorResponse
}

const STATUS_FALLBACKS: Record<number, string> = {
  400: 'Please review your details and try again.',
  401: 'Please sign in to continue.',
  403: 'You do not have permission to do that.',
  404: 'The requested item could not be found.',
  409: 'This action could not be completed because of a conflict. Please try again.',
  422: 'Please review the highlighted fields and try again.',
}

const DEFAULT_MESSAGE_MAP: Record<string, string> = {
  Unauthorized: 'Please sign in to continue.',
  Forbidden: 'You do not have permission to do that.',
  'Bad Request': 'Please review your details and try again.',
  'Not Found': 'The requested item could not be found.',
  Conflict: 'This action could not be completed because of a conflict. Please try again.',
}

function normalizeMessage(message: string): string {
  return DEFAULT_MESSAGE_MAP[message] ?? message
}

function extractFieldMessage(data: ErrorResponseData): string | null {
  for (const value of Object.values(data)) {
    if (typeof value === 'string' && value.trim()) {
      return normalizeMessage(value)
    }
  }

  return null
}

export function extractApiErrorMessage(error: unknown): string | null {
  if (!error || typeof error !== 'object' || !('response' in error)) {
    return null
  }

  const response = (error as ErrorWithResponse).response
  if (!response || typeof response !== 'object') {
    return null
  }

  const { status, data } = response
  if (data && typeof data === 'object') {
    const payload = data as ErrorResponseData
    if (typeof payload.error === 'string' && payload.error.trim()) {
      return normalizeMessage(payload.error)
    }

    if (typeof payload.message === 'string' && payload.message.trim()) {
      return normalizeMessage(payload.message)
    }

    const fieldMessage = extractFieldMessage(payload)
    if (fieldMessage) {
      return fieldMessage
    }
  }

  if (typeof status === 'number') {
    return STATUS_FALLBACKS[status] ?? null
  }

  return null
}

export function extractApiErrorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== 'object' || !('response' in error)) {
    return undefined
  }

  const response = (error as ErrorWithResponse).response
  return typeof response?.status === 'number' ? response.status : undefined
}
