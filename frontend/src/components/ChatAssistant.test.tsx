import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ChatAssistant from './ChatAssistant';
import chatService from '../services/chatService';

vi.mock('../services/chatService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../services/chatService')>();
  return { ...actual, default: { status: vi.fn(), send: vi.fn(), confirm: vi.fn() } };
});

const mocked = vi.mocked(chatService);
const ok = <T,>(data: T) => ({ data }) as never;

async function openChat() {
  render(<ChatAssistant />);
  await userEvent.click(await screen.findByRole('button', { name: 'Open assistant' }));
}

describe('ChatAssistant', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocked.status.mockResolvedValue(ok({ enabled: true }));
  });

  it('is not shown when the server has no API key', async () => {
    mocked.status.mockResolvedValue(ok({ enabled: false }));
    render(<ChatAssistant />);

    await waitFor(() => expect(mocked.status).toHaveBeenCalled());
    expect(screen.queryByRole('button', { name: 'Open assistant' })).not.toBeInTheDocument();
  });

  it('sends what the user typed and shows the answer', async () => {
    mocked.send.mockResolvedValue(ok({ reply: 'You have 2 courses.', pendingActions: [] }));
    await openChat();

    await userEvent.type(screen.getByLabelText('Message'), 'What are my courses?');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(await screen.findByText('You have 2 courses.')).toBeInTheDocument();
    // The greeting is for the screen only; the request starts with the user's own message.
    expect(mocked.send).toHaveBeenCalledWith([{ role: 'user', content: 'What are my courses?' }]);
  });

  it('runs a proposed change only after Confirm is pressed', async () => {
    mocked.send.mockResolvedValue(
      ok({ reply: 'Shall I?', pendingActions: [{ id: 'a1', type: 'ENROLL', description: 'Enroll in CS101 Intro' }] }),
    );
    mocked.confirm.mockResolvedValue(ok({ message: 'Enrolled in CS101 Intro.' }));
    await openChat();
    await userEvent.type(screen.getByLabelText('Message'), 'enroll me in CS101');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    await screen.findByText('Enroll in CS101 Intro');
    expect(mocked.confirm).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Confirm' }));

    expect(mocked.confirm).toHaveBeenCalledWith('a1');
    expect(await screen.findByText('Enrolled in CS101 Intro.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Confirm' })).not.toBeInTheDocument();
  });

  it('drops a proposal on Dismiss without calling the server', async () => {
    mocked.send.mockResolvedValue(
      ok({ reply: 'Shall I?', pendingActions: [{ id: 'a1', type: 'ENROLL', description: 'Enroll in CS101 Intro' }] }),
    );
    await openChat();
    await userEvent.type(screen.getByLabelText('Message'), 'enroll me');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));
    await screen.findByText('Enroll in CS101 Intro');

    await userEvent.click(screen.getByRole('button', { name: 'Dismiss' }));

    expect(screen.queryByText('Enroll in CS101 Intro')).not.toBeInTheDocument();
    expect(mocked.confirm).not.toHaveBeenCalled();
  });

  it('shows a readable error when the assistant fails', async () => {
    mocked.send.mockRejectedValue({ response: { data: { message: 'Too many messages. Please wait a moment and try again.' } } });
    await openChat();
    await userEvent.type(screen.getByLabelText('Message'), 'hi');
    await userEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(await screen.findByText('Too many messages. Please wait a moment and try again.')).toBeInTheDocument();
  });
});
