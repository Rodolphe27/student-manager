import { Injectable } from '@nestjs/common';
import { randomUUID } from 'node:crypto';

export interface PendingAction {
  id: string;
  type: string;
  description: string;
}

interface Entry extends PendingAction {
  username: string;
  params: Record<string, number>;
  expiresAt: number;
}

export type TakenAction = Pick<Entry, 'id' | 'type' | 'params'>;

/**
 * Write actions the assistant proposes. Nothing is executed until the same user confirms the proposal, once,
 * within ten minutes; at most five stay open per user.
 */
@Injectable()
export class PendingActionStore {
  static readonly TTL_MS = 10 * 60 * 1000;
  static readonly MAX_PER_USER = 5;

  private readonly byUser = new Map<string, Entry[]>();

  /** Overridable clock (tests). */
  now: () => number = Date.now;

  propose(username: string, type: string, params: Record<string, number>, description: string): PendingAction {
    const entry: Entry = {
      id: randomUUID(), type, description, username, params: { ...params },
      expiresAt: this.now() + PendingActionStore.TTL_MS,
    };
    const queue = (this.byUser.get(username) ?? []).filter((e) => e.expiresAt >= this.now());
    queue.push(entry);
    while (queue.length > PendingActionStore.MAX_PER_USER) queue.shift();
    this.byUser.set(username, queue);
    return { id: entry.id, type, description };
  }

  /** Removes and returns the proposal (single use), or null if unknown, foreign or expired. */
  take(id: string, username: string): TakenAction | null {
    const queue = this.byUser.get(username);
    if (!queue) return null;
    const index = queue.findIndex((e) => e.id === id);
    if (index < 0) return null;
    const [entry] = queue.splice(index, 1);
    return entry.expiresAt < this.now() ? null : entry;
  }
}
