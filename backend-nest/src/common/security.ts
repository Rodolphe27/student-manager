import { randomUUID, timingSafeEqual } from 'node:crypto';
import type { NextFunction, Request, Response } from 'express';
import { requestContext } from '../db/db';
import { ApiError } from './errors';
import { nowIso } from './error.filter';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);

function sameToken(a: string | undefined, b: string | undefined): boolean {
  if (!a || !b) return false;
  const x = Buffer.from(a);
  const y = Buffer.from(b);
  return x.length === y.length && timingSafeEqual(x, y);
}

/**
 * CSRF like Spring's CookieCsrfTokenRepository: a JS-readable XSRF-TOKEN cookie that the SPA copies
 * into the X-XSRF-TOKEN header. State-changing requests without a matching header are refused (403).
 */
export function csrfProtection(req: Request, res: Response, next: NextFunction) {
  let token: string | undefined = req.cookies?.['XSRF-TOKEN'];
  if (!token) {
    token = randomUUID();
    req.cookies = { ...(req.cookies ?? {}), 'XSRF-TOKEN': token };
    res.cookie('XSRF-TOKEN', token, { path: '/', httpOnly: false, secure: req.secure });
  }
  if (SAFE_METHODS.has(req.method) || sameToken(token, req.get('X-XSRF-TOKEN'))) return next();
  res.status(403).json({ status: 403, message: 'Access denied', details: null, timestamp: nowIso() });
}

/** Makes the acting username available to the audit columns for the rest of the request. */
export function auditContext(req: Request, _res: Response, next: NextFunction) {
  requestContext.run({ actor: req.session?.user?.username ?? null }, next);
}

export { ApiError };
