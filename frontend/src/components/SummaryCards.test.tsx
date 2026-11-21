import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { SummaryCards } from './SummaryCards'

describe('SummaryCards', () => {
  it('renders backend counts', () => {
    render(
      <SummaryCards counts={{ filled: 2, remaining: 1, checkedIn: 1, checkedOut: 0 }} />,
    )
    expect(screen.getByTestId('count-filled')).toHaveTextContent('2')
    expect(screen.getByTestId('count-remaining')).toHaveTextContent('1')
    expect(screen.getByTestId('count-checkedIn')).toHaveTextContent('1')
    expect(screen.getByTestId('count-checkedOut')).toHaveTextContent('0')
  })
})
