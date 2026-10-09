import 'reflect-metadata';
import { Logger } from '@nestjs/common';
import connectPgSimple from 'connect-pg-simple';
import session from 'express-session';
import { createApp } from './app.factory';
import { config } from './config';
import { PgDb } from './db/db';

async function bootstrap() {
  const cfg = config();
  if (cfg.production && cfg.sessionSecret === 'dev-only-secret-change-me') {
    throw new Error('SESSION_SECRET must be set in production');
  }
  const db = new PgDb(cfg.databaseUrl);
  const PgStore = connectPgSimple(session);
  // Own session table: the Spring backend keeps its sessions in spring_session, so the two never clash.
  const sessionStore = new PgStore({ pool: db.pool, tableName: 'nest_session', createTableIfMissing: true });
  const app = await createApp(db, { sessionStore });
  await app.listen(cfg.port, '0.0.0.0');
  new Logger('Bootstrap').log(`Student Manager (NestJS) listening on port ${cfg.port}`);
}

bootstrap().catch((error) => {
  // eslint-disable-next-line no-console
  console.error('Startup failed', error);
  process.exit(1);
});
