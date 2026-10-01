import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ErrorAlert from './ErrorAlert';

describe('ErrorAlert', () => {
  it('announces the message as an alert', () => {
    render(<ErrorAlert message="Something broke" />);

    expect(screen.getByRole('alert')).toHaveTextContent('Something broke');
  });

  it('only renders the action button when both label and handler are given', async () => {
    const onAction = vi.fn();
    const { rerender } = render(<ErrorAlert message="x" actionLabel="Retry" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();

    rerender(<ErrorAlert message="x" actionLabel="Retry" onAction={onAction} />);
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }));

    expect(onAction).toHaveBeenCalledOnce();
  });
});
