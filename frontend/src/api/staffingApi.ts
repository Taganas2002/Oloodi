import {
  ApiError,
  type Assignment,
  type AssignmentAction,
  type DashboardResponse,
  type ProblemDetail,
} from './types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') ?? ''

async function parseProblem(response: Response): Promise<ProblemDetail> {
  try {
    const body = (await response.json()) as ProblemDetail
    if (body && typeof body.status === 'number') {
      return body
    }
  } catch {
    // fall through
  }
  return {
    status: response.status,
    title: response.statusText || 'Error',
    detail: `Request failed with status ${response.status}`,
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      Accept: 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (!response.ok) {
    throw new ApiError(await parseProblem(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const staffingApi = {
  getDashboard(shiftId: string): Promise<DashboardResponse> {
    return request(`/api/shifts/${shiftId}/dashboard`)
  },

  getAssignments(shiftId: string): Promise<Assignment[]> {
    return request(`/api/shifts/${shiftId}/assignments`)
  },

  getAssignment(shiftId: string, assignmentId: string): Promise<Assignment> {
    return request(`/api/shifts/${shiftId}/assignments/${assignmentId}`)
  },

  postAction(
    shiftId: string,
    assignmentId: string,
    action: AssignmentAction,
  ): Promise<Assignment> {
    return request(`/api/shifts/${shiftId}/assignments/${assignmentId}/${action}`, {
      method: 'POST',
    })
  },
}
