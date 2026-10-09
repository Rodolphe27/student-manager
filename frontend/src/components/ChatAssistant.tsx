import { useEffect, useRef, useState } from 'react';
import chatService, { type ChatMessage, type PendingAction } from '../services/chatService';
import { getErrorMessage } from '../services/errorMessage';

interface Turn extends ChatMessage {
  actions?: PendingAction[];
  isError?: boolean;
}

const GREETING: Turn = {
  role: 'assistant',
  content: 'Hi! Ask me about courses, your enrollments or how Student Manager works.',
};

export default function ChatAssistant() {
  const [enabled, setEnabled] = useState(false);
  const [open, setOpen] = useState(false);
  const [turns, setTurns] = useState<Turn[]>([GREETING]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const bottom = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    chatService
      .status()
      .then((res) => !cancelled && setEnabled(res.data.enabled))
      .catch(() => !cancelled && setEnabled(false));
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    bottom.current?.scrollIntoView?.({ block: 'end' });
  }, [turns, open, busy]);

  if (!enabled) return null;

  const addTurn = (turn: Turn) => setTurns((prev) => [...prev, turn]);

  const send = async () => {
    const text = input.trim();
    if (!text || busy) return;
    const next: Turn[] = [...turns, { role: 'user', content: text }];
    setTurns(next);
    setInput('');
    setBusy(true);
    try {
      // The greeting and error notes are for the screen only, never part of the conversation.
      const history = next.filter((t) => !t.isError && t !== GREETING);
      const res = await chatService.send(history.map(({ role, content }) => ({ role, content })));
      addTurn({ role: 'assistant', content: res.data.reply, actions: res.data.pendingActions });
    } catch (err) {
      addTurn({
        role: 'assistant',
        isError: true,
        content: getErrorMessage(err, 'The assistant could not answer. Please try again.'),
      });
    } finally {
      setBusy(false);
    }
  };

  const confirm = async (turnIndex: number, action: PendingAction) => {
    setTurns((prev) =>
      prev.map((t, i) => (i === turnIndex ? { ...t, actions: t.actions?.filter((a) => a.id !== action.id) } : t)),
    );
    try {
      const res = await chatService.confirm(action.id);
      addTurn({ role: 'assistant', content: res.data.message });
    } catch (err) {
      addTurn({ role: 'assistant', isError: true, content: getErrorMessage(err, 'That change could not be made.') });
    }
  };

  const dismiss = (turnIndex: number, action: PendingAction) =>
    setTurns((prev) =>
      prev.map((t, i) => (i === turnIndex ? { ...t, actions: t.actions?.filter((a) => a.id !== action.id) } : t)),
    );

  return (
    <div className="fixed bottom-4 right-4 z-30 flex flex-col items-end gap-3">
      {open && (
        <section
          aria-label="Assistant"
          className="w-96 max-w-[calc(100vw-2rem)] h-[32rem] max-h-[calc(100vh-6rem)] bg-white rounded-xl border border-gray-100 shadow-lg flex flex-col"
        >
          <header className="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <h2 className="font-semibold text-gray-700 text-sm">Assistant</h2>
            <button
              onClick={() => setOpen(false)}
              className="text-gray-400 hover:text-gray-600 text-sm"
              aria-label="Close assistant"
            >
              ✕
            </button>
          </header>

          <div className="flex-1 overflow-auto px-4 py-3 space-y-3" role="log" aria-live="polite">
            {turns.map((turn, i) => (
              <div key={i} className={turn.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
                <div className="max-w-[85%]">
                  <div
                    className={`rounded-lg px-3 py-2 text-sm whitespace-pre-wrap ${
                      turn.role === 'user'
                        ? 'bg-blue-600 text-white'
                        : turn.isError
                          ? 'bg-red-50 border border-red-100 text-red-600'
                          : 'bg-gray-100 text-gray-700'
                    }`}
                  >
                    {turn.content}
                  </div>
                  {turn.actions?.map((action) => (
                    <div key={action.id} className="mt-2 rounded-lg border border-amber-200 bg-amber-50 p-2">
                      <p className="text-xs text-gray-600 mb-2">{action.description}</p>
                      <div className="flex gap-2">
                        <button
                          onClick={() => confirm(i, action)}
                          className="px-3 py-1 text-xs font-medium rounded-lg bg-blue-600 text-white hover:bg-blue-700"
                        >
                          Confirm
                        </button>
                        <button
                          onClick={() => dismiss(i, action)}
                          className="px-3 py-1 text-xs font-medium rounded-lg bg-gray-100 text-gray-600 hover:bg-gray-200"
                        >
                          Dismiss
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            ))}
            {busy && <p className="text-xs text-gray-400">Thinking…</p>}
            <div ref={bottom} />
          </div>

          <form
            className="flex gap-2 p-3 border-t border-gray-100"
            onSubmit={(e) => {
              e.preventDefault();
              void send();
            }}
          >
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              maxLength={2000}
              placeholder="Ask something…"
              aria-label="Message"
              className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <button
              type="submit"
              disabled={busy || !input.trim()}
              className="px-3 py-2 text-sm font-medium rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              Send
            </button>
          </form>
        </section>
      )}

      <button
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-label={open ? 'Close assistant' : 'Open assistant'}
        className="w-12 h-12 rounded-full bg-blue-600 text-white text-xl shadow-lg hover:bg-blue-700 flex items-center justify-center"
      >
        💬
      </button>
    </div>
  );
}
