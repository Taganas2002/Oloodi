import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { StaffingProgress } from './StaffingProgress'

describe('StaffingProgress', () => {
  it('renders filled fraction and remaining from backend counts', () => {
    render(<StaffingProgress filled={2} remaining={1} required={3} />)
    expect(screen.getByTestId('staffing-progress-fraction')).toHaveTextContent('2/3 filled')
    expect(screen.getByTestId('staffing-progress-label')).toHaveTextContent('1 remaining')
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '67')
  })

  it('shows Fully staffed when remaining is 0', () => {
    render(<StaffingProgress filled={3} remaining={0} required={3} />)
    expect(screen.getByTestId('staffing-progress-label')).toHaveTextContent('Fully staffed')
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100')
  })
})
