import { afterEach, describe, expect, it, vi } from 'vitest'
import { staffingApi } from './staffingApi'
import { ApiError } from './types'

describe('staffingApi', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('GETs dashboard', async () => {
    const payload = { shift: { id: '1' }, assignments: [], counts: { filled: 0, remaining: 3, checkedIn: 0, checkedOut: 0 } }
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => payload,
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await staffingApi.getDashboard('11111111-1111-1111-1111-111111111111')
    expect(result).toEqual(payload)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/shifts/11111111-1111-1111-1111-111111111111/dashboard',
      expect.objectContaining({ headers: expect.any(Object) }),
    )
  })

  it('POSTs mutation actions', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ id: 'a1', participationStatus: 'INVITED' }),
    })
    vi.stubGlobal('fetch', fetchMock)

    await staffingApi.postAction('s1', 'a1', 'invite')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/shifts/s1/assignments/a1/invite',
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('throws ApiError with ProblemDetail on failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: false,
        status: 409,
        json: async () => ({
          status: 409,
          title: 'Conflict',
          detail: 'Illegal transition',
        }),
      }),
    )

    await expect(staffingApi.postAction('s1', 'a1', 'accept')).rejects.toBeInstanceOf(ApiError)
  })
})
