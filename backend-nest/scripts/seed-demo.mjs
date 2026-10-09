// Fills an EMPTY database with a little sample data (like the Spring backend's SEED_DEMO_DATA):
//   DATABASE_URL=postgresql://... node scripts/seed-demo.mjs
// The schema must already exist (start the Spring backend once, or apply its Flyway migrations).
// Logins: admin / teacher / student accounts, all with DEFAULT_ACCOUNT_PASSWORD (default testuser12).
import bcrypt from 'bcryptjs';
import pg from 'pg';

const url = process.env.DATABASE_URL ?? 'postgresql://postgres:postgres@localhost:5432/studentmanager';
const password = process.env.DEFAULT_ACCOUNT_PASSWORD ?? 'testuser12';
const client = new pg.Client({
  connectionString: url.replace(/[?&]sslmode=require/, ''),
  ssl: /sslmode=require|supabase\./.test(url) ? { rejectUnauthorized: false } : undefined,
});
await client.connect();

const count = async (t) => Number((await client.query(`select count(*) n from ${t}`)).rows[0].n);
for (const t of ['users', 'terms', 'teachers', 'students', 'courses']) {
  if ((await count(t)) > 0) {
    console.log(`Demo data skipped: ${t} is not empty`);
    await client.end();
    process.exit(0);
  }
}

const hash = await bcrypt.hash(password, 10);
const audit = `now() at time zone 'utc'`;
const one = async (sql, params) => (await client.query(sql, params)).rows[0].id;
const user = (username, email, role) =>
  one(
    `insert into users (created_at, updated_at, version, active, username, email, password_hash, role)
     values (${audit}, ${audit}, 0, true, $1, $2, $3, $4) returning id`,
    [username, email, hash, role],
  );

await client.query('begin');
await user('admin', 'admin@student-manager.local', 'ADMIN');
const terms = [];
for (const [name, start, end] of [
  ['Winter 2025/26', '2025-10-01', '2026-03-31'],
  ['Summer 2026', '2026-04-01', '2026-09-30'],
]) {
  terms.push(
    await one(
      `insert into terms (created_at, updated_at, version, name, start_date, end_date)
       values (${audit}, ${audit}, 0, $1, $2, $3) returning id`,
      [name, start, end],
    ),
  );
}
const teachers = [];
for (const [first, last, dept] of [['Priya', 'Raman', 'Mathematics'], ['Sofia', 'Moreau', 'Physics'], ['Amara', 'Diallo', 'Biology']]) {
  const email = `${first}.${last}`.toLowerCase() + '@student-manager.local';
  const uid = await user(`${first}.${last}`.toLowerCase(), email, 'TEACHER');
  teachers.push(
    await one(
      `insert into teachers (created_at, updated_at, version, first_name, last_name, email, department, user_id)
       values (${audit}, ${audit}, 0, $1, $2, $3, $4, $5) returning id`,
      [first, last, email, dept, uid],
    ),
  );
}
const students = [];
let n = 1;
for (const [first, last] of [['Lena', 'Fischer'], ['Noah', 'Weber'], ['Mia', 'Keller'], ['Ben', 'Braun'], ['Emma', 'Wolf']]) {
  const email = `${first}.${last}`.toLowerCase() + '@student-manager.local';
  const uid = await user(`${first}.${last}`.toLowerCase(), email, 'STUDENT');
  students.push(
    await one(
      `insert into students (created_at, updated_at, version, first_name, last_name, matriculation_number, birth_date, email, user_id)
       values (${audit}, ${audit}, 0, $1, $2, $3, '2002-05-17', $4, $5) returning id`,
      [first, last, `M-${String(1000 + n++)}`, email, uid],
    ),
  );
}
const courses = [];
for (const [code, title, credits, status, t, term] of [
  ['ST-220', 'Applied Statistics', 5, 'ACTIVE', 0, 1],
  ['PH-110', 'Classical Mechanics', 5, 'ACTIVE', 1, 1],
  ['BI-130', 'Cell Biology', 5, 'INACTIVE', 2, 0],
  ['MA-101', 'Linear Algebra', 6, 'ACTIVE', 0, 0],
]) {
  courses.push(
    await one(
      `insert into courses (created_at, updated_at, version, code, title, description, credit_hours, status, teacher_id, term_id)
       values (${audit}, ${audit}, 0, $1, $2, $3, $4, $5, $6, $7) returning id`,
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
]) {
  await client.query(
    `insert into enrollments (created_at, updated_at, version, student_id, course_id, enrolled_at, status, grade, grade_seen)
     values (${audit}, ${audit}, 0, $1, $2, current_date - 14, $3, $4, $5)`,
    [students[s], courses[c], status, grade, seen],
  );
}
await client.query('commit');
console.log(`Demo data ready. Log in as admin / ${password}`);
await client.end();
