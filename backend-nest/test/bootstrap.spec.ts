import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { applySchemaIfEmpty, schemaFiles, seedDemoData } from '../src/db/bootstrap';
import { client, newDb, startApp } from './helpers';

describe('schema copy', () => {
  const spring = join(__dirname, '..', '..', 'backend', 'student-manager', 'src', 'main', 'resources', 'db', 'migration');

  // The NestJS app ships its own copy of Spring's Flyway migrations; they must never drift apart.
  (existsSync(spring) ? it : it.skip)('is identical to the Spring project\'s migrations', () => {
    const mine = schemaFiles();
    expect(mine).toEqual(readdirSync(spring).filter((f) => /^V\d+__.*\.sql$/.test(f)).sort((a, b) => parseInt(a.slice(1)) - parseInt(b.slice(1))));
    for (const file of mine) {
      expect(readFileSync(join(__dirname, '..', 'schema', file), 'utf8')).toBe(readFileSync(join(spring, file), 'utf8'));
    }
  });
});

describe('standalone start on an empty database', () => {
  it('creates the schema once, seeds sample data once, and the seeded logins work', async () => {
    const db = await newDb({ migrate: false });
    try {
      expect(await applySchemaIfEmpty(db)).toBe(true);
      expect(await applySchemaIfEmpty(db)).toBe(false); // second start leaves it alone
      expect(await seedDemoData(db, 'testuser12')).toBe(true);
      expect(await seedDemoData(db, 'testuser12')).toBe(false);

      const counts = await db.query(
        `select (select count(*) from users) as users, (select count(*) from students) as students,
                (select count(*) from teachers) as teachers, (select count(*) from courses) as courses,
                (select count(*) from enrollments) as enrollments`,
      );
      expect(counts.rows[0]).toEqual({ users: 9, students: 5, teachers: 3, courses: 4, enrollments: 5 });

      const app = await startApp(db);
      try {
        const admin = await client(app);
        await admin.login('admin', 'testuser12');
        expect((await admin.get('/api/courses')).body.page.totalElements).toBe(4);
        const lena = await client(app);
        await lena.login('lena.fischer', 'testuser12');
        const mine = await lena.get('/api/students/me');
        expect(mine.body.fullName).toBe('Lena Fischer');
        const enrollments = await lena.get(`/api/enrollments/student/${mine.body.id}`);
        expect(enrollments.body.map((e: { status: string }) => e.status).sort()).toEqual(['CONFIRMED', 'PENDING']);
      } finally {
        await app.close();
      }
    } finally {
      await db.close();
    }
  }, 300_000);

  it('does not touch a database that already has the tables', async () => {
    const db = await newDb(); // schema applied the way Spring's Flyway would
    try {
      expect(await applySchemaIfEmpty(db)).toBe(false);
    } finally {
      await db.close();
    }
  }, 300_000);
});
