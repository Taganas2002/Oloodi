import { ApiError, type ProblemDetail } from '../api/types'

const CONCURRENCY_FALLBACK =
  'The assignment was changed by another session. Latest state has been refreshed — try again if needed.'

export function toUserMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.isConflict) {
      const detail = error.problem.detail?.trim()
      if (detail) {
        return detail
      }
      return CONCURRENCY_FALLBACK
    }
    return error.problem.detail ?? error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Something went wrong. Please try again.'
}

export function isConflictError(error: unknown): boolean {
  return error instanceof ApiError && error.isConflict
}

export function problemFromUnknown(error: unknown): ProblemDetail | null {
  if (error instanceof ApiError) {
    return error.problem
  }
  return null
}
