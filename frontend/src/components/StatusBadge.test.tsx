import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatusBadge from './StatusBadge';

describe('StatusBadge', () => {
  it.each([
    ['ACTIVE', 'bg-green-100'],
    ['CONFIRMED', 'bg-green-100'],
    ['PENDING', 'bg-yellow-100'],
    ['INACTIVE', 'bg-gray-100'],
    ['CANCELLED', 'bg-red-100'],
    ['ARCHIVED', 'bg-red-100'],
  ] as const)('shows %s with the %s colour', (status, expectedClass) => {
    render(<StatusBadge status={status} />);

    expect(screen.getByText(status)).toHaveClass(expectedClass);
  });
});
