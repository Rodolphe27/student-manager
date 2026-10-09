import { Body, Controller, Get, HttpCode, Post, Req } from '@nestjs/common';
import type { Request } from 'express';
import { Access, sessionUser } from '../common/access';
import { malformedBody, validationFailed } from '../common/errors';
import { parseBody } from '../common/validate';
import { ChatMessage } from './assistant.client';
import { ChatService, MAX_MESSAGE_CHARS, MAX_MESSAGES } from './chat.service';

/** Same rules as the Spring ChatRequest: 1..20 messages of role user/assistant, up to 2000 characters each. */
function parseConversation(body: unknown): ChatMessage[] {
  if (body === null || typeof body !== 'object' || !Array.isArray((body as { messages?: unknown }).messages)) {
    if (body !== null && typeof body === 'object' && (body as { messages?: unknown }).messages === undefined) {
      throw validationFailed({ messages: 'must not be empty' });
    }
    throw malformedBody();
  }
  const messages = (body as { messages: unknown[] }).messages;
  const errors: Record<string, string> = {};
  if (messages.length === 0) errors.messages = 'must not be empty';
  else if (messages.length > MAX_MESSAGES) errors.messages = 'conversation is too long';
  messages.forEach((raw, i) => {
    if (raw === null || typeof raw !== 'object') throw malformedBody();
    const { role, content } = raw as { role?: unknown; content?: unknown };
    if ((role !== undefined && role !== null && typeof role !== 'string') ||
        (content !== undefined && content !== null && typeof content !== 'string')) {
      throw malformedBody();
    }
    if (role == null || (role as string).trim() === '') errors[`messages[${i}].role`] = 'must not be blank';
    else if (role !== 'user' && role !== 'assistant') errors[`messages[${i}].role`] = 'role must be user or assistant';
    if (content == null || (content as string).trim() === '') errors[`messages[${i}].content`] = 'must not be blank';
    else if ((content as string).length > MAX_MESSAGE_CHARS) errors[`messages[${i}].content`] = 'message is too long';
  });
  if (Object.keys(errors).length > 0) throw validationFailed(errors);
  return messages as ChatMessage[];
}

@Controller('api/chat')
@Access('authenticated')
export class ChatController {
  constructor(private readonly service: ChatService) {}

  @Get('status')
  status() {
    return { enabled: this.service.isAvailable() };
  }

  @Post()
  @HttpCode(200)
  chat(@Body() body: unknown, @Req() req: Request) {
    return this.service.chat(parseConversation(body), sessionUser(req));
  }

  @Post('confirm')
  @HttpCode(200)
  confirm(@Body() body: unknown, @Req() req: Request) {
    const r = parseBody<{ actionId: string }>(body, { actionId: { kind: 'string', required: 'must not be blank' } });
    return this.service.confirm(r.actionId, sessionUser(req));
  }
}
