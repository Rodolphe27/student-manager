import api from './api';
import type { AxiosResponse } from 'axios';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

// A change the assistant proposed. It only happens when the user confirms it.
export interface PendingAction {
  id: string;
  type: string;
  description: string;
}

export interface ChatReply {
  reply: string;
  pendingActions: PendingAction[];
}

// The server accepts at most this many messages per request.
export const MAX_HISTORY = 20;

// The server keeps no chat history: every request carries the conversation so far.
// It must start with a user message and stay within MAX_HISTORY, so cut from the end.
export function trimHistory(messages: ChatMessage[]): ChatMessage[] {
  const recent = messages.slice(-MAX_HISTORY);
  const firstUser = recent.findIndex((m) => m.role === 'user');
  return firstUser === -1 ? [] : recent.slice(firstUser);
}

const chatService = {
  // False when the server has no API key; the chat button is then not shown at all.
  status: (): Promise<AxiosResponse<{ enabled: boolean }>> =>
    api.get<{ enabled: boolean }>('/chat/status'),

  send: (messages: ChatMessage[]): Promise<AxiosResponse<ChatReply>> =>
    api.post<ChatReply>('/chat', { messages: trimHistory(messages) }),

  confirm: (actionId: string): Promise<AxiosResponse<{ message: string }>> =>
    api.post<{ message: string }>('/chat/confirm', { actionId }),
};

export default chatService;
