import { Injectable } from '@nestjs/common';
import { SessionUser } from '../common/access';
import { ApiError, forbidden, notFoundMessage, validation } from '../common/errors';
import { OwnershipService } from '../common/ownership.service';
import { CoursesService } from '../courses/courses.service';
import { EnrollmentsService } from '../enrollments/enrollments.service';
import { StudentsService } from '../students/students.service';
import { AssistantClient, ChatMessage } from './assistant.client';
import { assistantPrompt } from './assistant.prompt';
import { AssistantToolbox } from './assistant.toolbox';
import { PendingAction, PendingActionStore } from './pending-action.store';

export const MAX_MESSAGES = 20;
export const MAX_MESSAGE_CHARS = 2000;
export const REQUESTS_PER_MINUTE = 10;

@Injectable()
export class ChatService {
  private readonly recent = new Map<string, number[]>();

  constructor(
    private readonly client: AssistantClient,
    private readonly courses: CoursesService,
    private readonly enrollments: EnrollmentsService,
    private readonly students: StudentsService,
    private readonly ownership: OwnershipService,
    private readonly store: PendingActionStore,
  ) {}

  isAvailable(): boolean {
    return this.client.isConfigured();
  }

  async chat(history: ChatMessage[], user: SessionUser): Promise<{ reply: string; pendingActions: PendingAction[] }> {
    if (!this.client.isConfigured()) throw new ApiError(503, 'The assistant is not set up on this server.');
    if (history[0].role !== 'user' || history[history.length - 1].role !== 'user') {
      throw validation('The conversation must start and end with a message from the user');
    }
    this.enforceRateLimit(user.username);
    const tools = new AssistantToolbox(this.courses, this.enrollments, this.students, this.ownership, this.store, user);
    const reply = await this.client.reply(assistantPrompt(user.username, user.role), history, tools);
    return { reply, pendingActions: tools.proposed() };
  }

  /** Runs a proposal the user has confirmed, re-checking their rights at this moment. */
  async confirm(actionId: string, user: SessionUser): Promise<{ message: string }> {
    const entry = this.store.take(actionId, user.username);
    if (!entry) throw notFoundMessage('This proposal has expired or was already used');
    switch (entry.type) {
      case 'ENROLL': {
        const studentId = (await this.students.findByAccountUsername(user.username)).id;
        if (!(await this.ownership.canEnroll(studentId, entry.params.courseId, user))) throw forbidden();
        const done = await this.enrollments.create(studentId, entry.params.courseId);
        return { message: `Enrolled in ${done.courseCode} ${done.courseTitle}. Status: ${done.status}.` };
      }
      case 'CANCEL': {
        if (!(await this.ownership.canCancelEnrollment(entry.params.enrollmentId, user))) throw forbidden();
        const done = await this.enrollments.cancel(entry.params.enrollmentId);
        return { message: `Cancelled the enrollment in ${done.courseCode} ${done.courseTitle}.` };
      }
      default:
        throw validation('Unknown action');
    }
  }

  private enforceRateLimit(username: string) {
    const now = Date.now();
    const times = (this.recent.get(username) ?? []).filter((t) => t >= now - 60_000);
    if (times.length >= REQUESTS_PER_MINUTE) {
      throw new ApiError(429, 'Too many messages. Please wait a moment and try again.');
    }
    times.push(now);
    this.recent.set(username, times);
  }
}
