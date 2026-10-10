import { Logger } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { Db, NOW } from './db';

const log = new Logger('Bootstrap');

/** The Spring project's Flyway migrations, copied next to this app so it can start on an empty database. */
export const SCHEMA_DIR = join(__dirname, '..', '..', 'schema');

export function schemaFiles(dir = SCHEMA_DIR): string[] {
  return readdirSync(dir)
    .filter((f) => /^V\d+__.*\.sql$/.test(f))
    .sort((a, b) => Number(a.slice(1).split('__')[0]) - Number(b.slice(1).split('__')[0]));
}

/**
 * Creates the tables on an EMPTY database (no `users` table yet). A database that Spring already set up is left alone:
 * Spring's Flyway stays the owner of the schema there.
 */
export async function applySchemaIfEmpty(db: Db, dir = SCHEMA_DIR): Promise<boolean> {
  const { rows } = await db.query("select to_regclass('public.users') as t");
  if (rows[0].t !== null) return false;
  for (const file of schemaFiles(dir)) {
    await db.query(readFileSync(join(dir, file), 'utf8'));
    log.log(`applied ${file}`);
  }
  return true;
}

/**
 * Sample data for an empty database (like the Spring backend's SEED_DEMO_DATA): an admin, 3 teachers, 5 students,
 * 2 terms, 4 courses and a few enrollments. Every account gets `password`. Does nothing if any of the main tables has rows.
 */
export async function seedDemoData(db: Db, password: string): Promise<boolean> {
  for (const table of ['users', 'terms', 'teachers', 'students', 'courses']) {
    const { rows } = await db.query(`select count(*) as n from ${table}`);
    if (rows[0].n > 0) {
      log.log(`Demo data skipped: ${table} is not empty`);
      return false;
    }
  }
  const hash = await bcrypt.hash(password, 10);
  await db.tx(async (q) => {
    const one = async (sql: string, params: unknown[]) => (await q.query(sql, params)).rows[0].id as number;
    const user = (username: string, email: string, role: string) =>
      one(
        `insert into users (created_at, updated_at, version, active, username, email, password_hash, role)
         values (${NOW}, ${NOW}, 0, true, $1, $2, $3, $4) returning id`,
        [username, email, hash, role],
      );
    await user('admin', 'admin@student-manager.local', 'ADMIN');

    const terms: number[] = [];
    for (const [name, start, end] of [
      ['Winter 2025/26', '2025-10-01', '2026-03-31'],
      ['Summer 2026', '2026-04-01', '2026-09-30'],
    ]) {
      terms.push(
        await one(
          `insert into terms (created_at, updated_at, version, name, start_date, end_date)
           values (${NOW}, ${NOW}, 0, $1, $2, $3) returning id`,
          [name, start, end],
        ),
      );
    }

    const teachers: number[] = [];
    for (const [first, last, dept] of [['Priya', 'Raman', 'Mathematics'], ['Sofia', 'Moreau', 'Physics'], ['Amara', 'Diallo', 'Biology']]) {
      const email = `${first}.${last}`.toLowerCase() + '@student-manager.local';
      const uid = await user(`${first}.${last}`.toLowerCase(), email, 'TEACHER');
      teachers.push(
        await one(
          `insert into teachers (created_at, updated_at, version, first_name, last_name, email, department, user_id)
           values (${NOW}, ${NOW}, 0, $1, $2, $3, $4, $5) returning id`,
          [first, last, email, dept, uid],
        ),
      );
    }

    const students: number[] = [];
    let n = 1000;
    for (const [first, last] of [['Lena', 'Fischer'], ['Noah', 'Weber'], ['Mia', 'Keller'], ['Ben', 'Braun'], ['Emma', 'Wolf']]) {
      const email = `${first}.${last}`.toLowerCase() + '@student-manager.local';
      const uid = await user(`${first}.${last}`.toLowerCase(), email, 'STUDENT');
      students.push(
        await one(
          `insert into students (created_at, updated_at, version, first_name, last_name, matriculation_number, birth_date, email, user_id)
           values (${NOW}, ${NOW}, 0, $1, $2, $3, '2002-05-17', $4, $5) returning id`,
          [first, last, `M-${++n}`, email, uid],
        ),
      );
    }

    const courses: number[] = [];
    for (const [code, title, credits, status, t, term] of [
      ['ST-220', 'Applied Statistics', 5, 'ACTIVE', 0, 1],
      ['PH-110', 'Classical Mechanics', 5, 'ACTIVE', 1, 1],
      ['BI-130', 'Cell Biology', 5, 'INACTIVE', 2, 0],
      ['MA-101', 'Linear Algebra', 6, 'ACTIVE', 0, 0],
    ] as const) {
      courses.push(
        await one(
          `insert into courses (created_at, updated_at, version, code, title, description, credit_hours, status, teacher_id, term_id)
           values (${NOW}, ${NOW}, 0, $1, $2, $3, $4, $5, $6, $7) returning id`,
          [code, title, `${title} — sample course`, credits, status, teachers[t], terms[term]],
        ),
      );
    }

    for (const [s, c, status, grade, seen] of [
      [0, 0, 'CONFIRMED', 'A', false],
      [0, 1, 'PENDING', 'NOT_GRADED', true],
      [1, 0, 'CONFIRMED', 'B', true],
      [2, 3, 'CONFIRMED', 'NOT_GRADED', true],
      [3, 1, 'CANCELLED', 'NOT_GRADED', true],
    ] as const) {
      await q.query(
        `insert into enrollments (created_at, updated_at, version, student_id, course_id, enrolled_at, status, grade, grade_seen)
         values (${NOW}, ${NOW}, 0, $1, $2, (now() at time zone 'utc')::date - 14, $3, $4, $5)`,
        [students[s], courses[c], status, grade, seen],
      );
    }
  });
  log.log(`Demo data ready. Log in as admin / ${password}`);
  return true;
}
