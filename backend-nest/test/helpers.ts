import { mkdtempSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { createServer } from 'node:net';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import * as bcrypt from 'bcryptjs';
import request from 'supertest';
import type { NestExpressApplication } from '@nestjs/platform-express';
import { AppOptions, createApp } from '../src/app.factory';
import { Db, PgDb } from '../src/db/db';

/** The schema the app ships with (a copy of Spring's Flyway migrations; test/schema.spec.ts keeps the two identical). */
const MIGRATIONS = join(__dirname, '..', 'schema');

const freePort = () =>
  new Promise<number>((resolve, reject) => {
    const server = createServer();
    server.listen(0, () => {
      const { port } = server.address() as { port: number };
      server.close(() => resolve(port));
    });
    server.on('error', reject);
  });

/** A real Postgres (the same engine and `pg` driver as production). */
export class TestDb extends PgDb {
  constructor(url: string, private readonly cleanup: () => Promise<void>) {
    super(url);
  }
  async close() {
    await super.close();
    await this.cleanup();
  }
}

const MIGRATION_FILES = () =>
  readdirSync(MIGRATIONS)
    .filter((f) => /^V\d+__.*\.sql$/.test(f))
    .sort((a, b) => Number(a.slice(1).split('__')[0]) - Number(b.slice(1).split('__')[0]));

async function migrate(db: Db) {
  for (const file of MIGRATION_FILES()) await db.query(readFileSync(join(MIGRATIONS, file), 'utf8'));
}

/**
 * A fresh database with the Spring schema. With TEST_DATABASE_URL (see scripts/test-db.mjs) it is a new database
 * on that server; otherwise a Postgres is started just for this test file.
 */
export async function newDb(options: { migrate?: boolean } = {}): Promise<TestDb> {
  const admin = process.env.TEST_DATABASE_URL;
  if (admin) {
    const name = `t_${Date.now()}_${Math.floor(Math.random() * 1e6)}`;
    const adminDb = new PgDb(admin);
    await adminDb.query(`create database ${name}`);
    const db = new TestDb(admin.replace(/\/[^/]*$/, `/${name}`), async () => {
      await adminDb.query(`drop database if exists ${name} with (force)`);
      await adminDb.close();
    });
    if (options.migrate !== false) await migrate(db);
    return db;
  }
  const dir = mkdtempSync(join(tmpdir(), 'nest-pg-'));
  const port = await freePort();
  // embedded-postgres is an ES module; a real dynamic import keeps ts-jest's CommonJS output from turning it into require().
  const { default: EmbeddedPostgres } = await (new Function('m', 'return import(m)') as (m: string) => Promise<any>)('embedded-postgres');
  const server = new EmbeddedPostgres({
    databaseDir: join(dir, 'data'), user: 'postgres', password: 'postgres', port, persistent: false,
    onLog: () => undefined, onError: () => undefined,
  });
  await server.initialise();
  await server.start();
  await server.createDatabase('studentmanager');
  const db = new TestDb(`postgresql://postgres:postgres@localhost:${port}/studentmanager`, async () => {
    await server.stop();
    rmSync(dir, { recursive: true, force: true });
  });
  if (options.migrate !== false) await migrate(db);
  return db;
}

export const PASSWORD = 'secret123';

export async function addUser(db: Db, username: string, role: 'STUDENT' | 'TEACHER' | 'ADMIN', email = `${username}@x.test`) {
  const hash = await bcrypt.hash(PASSWORD, 4);
  const { rows } = await db.query(
    `insert into users (created_at, updated_at, version, active, username, email, password_hash, role)
     values (now(), now(), 0, true, $1, $2, $3, $4) returning id`,
    [username, email, hash, role],
  );
  return rows[0].id as number;
}

export async function addTeacher(db: Db, username: string, first = 'Tina', last = 'Teach') {
  const userId = await addUser(db, username, 'TEACHER');
  const { rows } = await db.query(
    `insert into teachers (created_at, updated_at, version, first_name, last_name, email, department, user_id)
     values (now(), now(), 0, $1, $2, $3, 'Physics', $4) returning id`,
    [first, last, `${username}@x.test`, userId],
  );
  return rows[0].id as number;
}

export async function addStudent(db: Db, username: string, matriculation: string, first = 'Sam', last = 'Student') {
  const userId = await addUser(db, username, 'STUDENT');
  const { rows } = await db.query(
    `insert into students (created_at, updated_at, version, first_name, last_name, matriculation_number, email, user_id)
     values (now(), now(), 0, $1, $2, $3, $4, $5) returning id`,
    [first, last, matriculation, `${username}@x.test`, userId],
  );
  return rows[0].id as number;
}

export async function addCourse(db: Db, code: string, teacherId: number | null, status = 'ACTIVE') {
  const { rows } = await db.query(
    `insert into courses (created_at, updated_at, version, code, title, credit_hours, status, teacher_id)
     values (now(), now(), 0, $1, $2, 5, $3, $4) returning id`,
    [code, `Course ${code}`, status, teacherId],
  );
  return rows[0].id as number;
}

export async function startApp(db: Db, options: AppOptions = {}): Promise<NestExpressApplication> {
  const app = await createApp(db, { logger: false, ...options });
  await app.init();
  return app;
}

/** A browser-like client: keeps cookies and sends the XSRF header, like axios does. */
export async function client(app: NestExpressApplication) {
  const agent = request.agent(app.getHttpServer());
  const first = await agent.get('/api/auth/me'); // sets the XSRF-TOKEN cookie
  const cookie = (first.headers['set-cookie'] as unknown as string[]).find((c) => c.startsWith('XSRF-TOKEN='))!;
  const token = cookie.split(';')[0].split('=')[1];
  const withToken = (r: request.Test) => r.set('X-XSRF-TOKEN', token);
  return {
    token,
    get: (url: string) => agent.get(url),
    post: (url: string, body?: object) => withToken(agent.post(url)).send(body),
    put: (url: string, body?: object) => withToken(agent.put(url)).send(body),
    patch: (url: string, body?: object) => withToken(agent.patch(url)).send(body),
    delete: (url: string) => withToken(agent.delete(url)),
    raw: agent,
    async login(username: string, password = PASSWORD) {
      const res = await withToken(agent.post('/api/auth/login')).send({ username, password });
      if (res.status !== 200) throw new Error(`login ${username} failed: ${res.status} ${JSON.stringify(res.body)}`);
    },
  };
}
