import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ApiError } from '../api/types'
import { toUserMessage } from '../lib/errors'
import { ErrorBanner } from './ErrorBanner'

describe('ErrorBanner + ProblemDetail messaging', () => {
  it('renders conflict detail from ProblemDetail', () => {
    const message = toUserMessage(
      new ApiError({
        status: 409,
        title: 'Conflict',
        detail:
          'Illegal transition: cannot check-in from participation=UNINVITED attendance=NOT_STARTED',
      }),
    )
    render(<ErrorBanner message={message} />)
    expect(screen.getByTestId('error-banner')).toHaveTextContent(/Illegal transition/)
  })

  it('uses concurrency fallback for bare 409', () => {
    const message = toUserMessage(new ApiError({ status: 409, title: 'Conflict' }))
    expect(message).toMatch(/changed by another session|refreshed/i)
  })

  it('surfaces optimistic-lock ProblemDetail copy', () => {
    const message = toUserMessage(
      new ApiError({
        status: 409,
        title: 'Conflict',
        detail: 'The assignment was changed by another session. Reload the latest state.',
      }),
    )
    expect(message).toMatch(/changed by another session/i)
  })
})
