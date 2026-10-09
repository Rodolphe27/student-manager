import { ArgumentsHost, Catch, ExceptionFilter, HttpException, Logger } from '@nestjs/common';
import type { Request, Response } from 'express';
import { ApiError } from './errors';

const DATA_INTEGRITY =
  'This record is still referenced by other data (or duplicates an existing one), so the change was not applied.';

/** LocalDateTime.now() as Spring prints it: ISO without a zone. */
export const nowIso = () => new Date().toISOString().replace('Z', '');

@Catch()
export class AllExceptionsFilter implements ExceptionFilter {
  private readonly log = new Logger('Errors');

  catch(exception: unknown, host: ArgumentsHost) {
    const http = host.switchToHttp();
    const res = http.getResponse<Response>();
    const req = http.getRequest<Request>();
    const { status, message, details, empty } = this.describe(exception, req);
    if (empty) {
      res.status(status).end();
      return;
    }
    res.status(status).json({ status, message, details, timestamp: nowIso() });
  }

  private describe(exception: unknown, req: Request) {
    if (exception instanceof ApiError) {
      if (exception.status >= 500) this.log.error(exception.message);
      else this.log.warn(`${exception.status} ${exception.message}`);
      return { status: exception.status, message: exception.message, details: exception.details, empty: exception.empty };
    }
    const e = exception as { type?: string; code?: string; status?: number };
    // body-parser: invalid JSON
    if (e?.type === 'entity.parse.failed' || e?.type === 'encoding.unsupported') {
      return { status: 400, message: 'Malformed request body', details: null, empty: false };
    }
    if (e?.type === 'entity.too.large') {
      return { status: 413, message: 'Request body too large', details: null, empty: false };
    }
    // Postgres: unique_violation / foreign_key_violation
    if (e?.code === '23505' || e?.code === '23503') {
      this.log.warn(`Data integrity violation: ${(exception as Error).message}`);
      return { status: 409, message: DATA_INTEGRITY, details: null, empty: false };
    }
    if (exception instanceof HttpException) {
      const status = exception.getStatus();
      // Nest wraps body-parser failures in a plain HttpException whose response is just the text.
      if (typeof exception.getResponse() === 'string') {
        if (status === 400) return { status, message: 'Malformed request body', details: null, empty: false };
        if (status === 413) return { status, message: 'Request body too large', details: null, empty: false };
      }
      // Spring answers 401 for an unknown path when nobody is logged in.
      if (status === 404 && !req.session?.user) {
        return { status: 401, message: 'Unauthorized', details: null, empty: true };
      }
      const text = status === 404 ? 'Not found' : exception.message;
      return { status, message: text, details: null, empty: false };
    }
    this.log.error(`Unexpected error: ${(exception as Error)?.message}`, (exception as Error)?.stack);
    return { status: 500, message: 'Internal server error', details: null, empty: false };
  }
}
