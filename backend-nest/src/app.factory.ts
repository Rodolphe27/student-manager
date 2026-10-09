import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import cookieParser from 'cookie-parser';
import { json } from 'express';
import type { NextFunction, Request, Response } from 'express';
import session from 'express-session';
import { AppModule } from './app.module';
import type { AssistantClient } from './chat/assistant.client';
import { nowIso } from './common/error.filter';
import { auditContext, csrfProtection } from './common/security';
import { config } from './config';
import { Db } from './db/db';

export interface AppOptions {
  /** Where sessions live. Default: in memory (tests). */
  sessionStore?: session.Store;
  logger?: false;
  /** Replaces the Claude client (tests). */
  assistant?: AssistantClient;
}

/** Builds the app with the same cross-cutting behaviour as the Spring backend (session cookie, CSRF, CORS). */
export async function createApp(db: Db, options: AppOptions = {}): Promise<NestExpressApplication> {
  const cfg = config();
  const app = await NestFactory.create<NestExpressApplication>(AppModule.forRoot(db, options.assistant), {
    logger: options.logger === false ? false : ['log', 'warn', 'error'],
    bodyParser: false, // registered below, so a malformed body can be answered in the API's error format
  });
  app.set('trust proxy', 1);
  app.disable('x-powered-by');
  app.enableCors({
    origin: cfg.allowedOrigins,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  });
  app.use((_req: unknown, res: { setHeader(k: string, v: string): void }, next: () => void) => {
    res.setHeader('Strict-Transport-Security', 'max-age=31536000 ; includeSubDomains');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    next();
  });
  app.use(cookieParser());
  app.use(
    session({
      name: 'SESSION',
      secret: cfg.sessionSecret,
      resave: false,
      saveUninitialized: false,
      rolling: true,
      store: options.sessionStore,
      cookie: { httpOnly: true, sameSite: 'lax', secure: 'auto', maxAge: 30 * 60 * 1000 },
    }),
  );
  app.use(csrfProtection);
  app.use(json());
  app.use(bodyErrors);
  app.use(auditContext);
  return app;
}

/** body-parser failures happen before Nest's router, so its exception filter never sees them. */
function bodyErrors(err: { type?: string }, _req: Request, res: Response, next: NextFunction) {
  const tooLarge = err?.type === 'entity.too.large';
  if (!err || (!tooLarge && err.type !== 'entity.parse.failed' && err.type !== 'encoding.unsupported')) return next(err);
  const status = tooLarge ? 413 : 400;
  res.status(status).json({
    status,
    message: tooLarge ? 'Request body too large' : 'Malformed request body',
    details: null,
    timestamp: nowIso(),
  });
}
