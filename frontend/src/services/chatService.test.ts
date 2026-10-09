import { describe, it, expect } from 'vitest';
import { trimHistory, MAX_HISTORY, type ChatMessage } from './chatService';

const msg = (role: ChatMessage['role'], n: number): ChatMessage => ({ role, content: `m${n}` });

describe('trimHistory', () => {
  it('keeps a short conversation as it is', () => {
    const history = [msg('user', 1), msg('assistant', 2), msg('user', 3)];
    expect(trimHistory(history)).toEqual(history);
  });

  it('keeps at most MAX_HISTORY messages and starts with a user message', () => {
    const history = Array.from({ length: 45 }, (_, i) => msg(i % 2 === 0 ? 'user' : 'assistant', i));
    const trimmed = trimHistory(history);

    expect(trimmed.length).toBeLessThanOrEqual(MAX_HISTORY);
    expect(trimmed[0].role).toBe('user');
    expect(trimmed.at(-1)).toEqual(history.at(-1));
  });

  it('sends nothing when there is no user message', () => {
    expect(trimHistory([msg('assistant', 1)])).toEqual([]);
  });
});
