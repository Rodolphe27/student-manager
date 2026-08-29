import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatCard from './StatCard';

describe('StatCard', () => {
  it('renders the label, value, and sub text', () => {
    render(<StatCard label="Total Students" value={42} sub="All time" color="blue" />);

    expect(screen.getByText('Total Students')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('All time')).toBeInTheDocument();
  });

  it.each([
    ['blue', 'text-blue-600'],
    ['green', 'text-green-600'],
    ['orange', 'text-orange-500'],
    ['red', 'text-red-500'],
  ] as const)('applies the %s color class to the value', (color, expectedClass) => {
    render(<StatCard label="Label" value={0} sub="Sub" color={color} />);
    expect(screen.getByText('0')).toHaveClass(expectedClass);
  });
});
