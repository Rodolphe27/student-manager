import type { NestExpressApplication } from '@nestjs/platform-express';
import {
  addCourse, addStudent, addTeacher, addUser, client, newDb, PASSWORD, TestDb, startApp,
} from './helpers';

let db: TestDb;
let app: NestExpressApplication;
let ids: { teacher: number; otherTeacher: number; student: number; otherStudent: number; course: number; otherCourse: number };

beforeAll(async () => {
  db = await newDb();
  app = await startApp(db);
  await addUser(db, 'admin', 'ADMIN', 'admin@x.test');
  const teacher = await addTeacher(db, 'tina', 'Tina', 'Teach');
  const otherTeacher = await addTeacher(db, 'otto', 'Otto', 'Other');
  const student = await addStudent(db, 'sam', 'M-001', 'Sam', 'Student');
  const otherStudent = await addStudent(db, 'sue', 'M-002', 'Sue', 'Other');
  const course = await addCourse(db, 'CS-101', teacher);
  const otherCourse = await addCourse(db, 'MA-201', otherTeacher);
  ids = { teacher, otherTeacher, student, otherStudent, course, otherCourse };
}, 300_000);

afterAll(async () => {
  await app.close();
  await db.close();
}, 120_000);

const as = async (username: string) => {
  const c = await client(app);
  await c.login(username);
  return c;
};

describe('session and CSRF', () => {
  it('refuses a state-changing request without the XSRF header', async () => {
    const c = await client(app);
    const res = await c.raw.post('/api/auth/login').send({ username: 'admin', password: PASSWORD });
    expect(res.status).toBe(403);
  });

  it('answers a wrong password with the generic 401 body', async () => {
    const c = await client(app);
    const res = await c.post('/api/auth/login', { username: 'admin', password: 'nope-nope1' });
    expect(res.status).toBe(401);
    expect(res.body).toMatchObject({ status: 401, message: 'Invalid username or password', details: null });
    expect(typeof res.body.timestamp).toBe('string');
    const unknown = await c.post('/api/auth/login', { username: 'ghost', password: 'whatever1' });
    expect(unknown.body.message).toBe('Invalid username or password');
  });

  it('reports validation problems per field', async () => {
    const c = await client(app);
    const res = await c.post('/api/auth/login', { username: '', password: '' });
    expect(res.status).toBe(400);
    expect(res.body.message).toBe('Validation failed');
    expect(res.body.details).toEqual({ username: 'Username is required', password: 'Password is required' });
  });

  it('gives 401 with an empty body when nobody is logged in, and the account after login', async () => {
    const c = await client(app);
    const anon = await c.get('/api/auth/me');
    expect(anon.status).toBe(401);
    expect(anon.text).toBe('');
    await c.login('admin');
    const me = await c.get('/api/auth/me');
    expect(me.status).toBe(200);
    expect(me.body).toEqual({ username: 'admin', email: 'admin@x.test', role: 'ADMIN' });
  });

  it('logs out (204) and the session is gone afterwards', async () => {
    const c = await as('admin');
    expect((await c.post('/api/auth/logout')).status).toBe(204);
    expect((await c.get('/api/auth/me')).status).toBe(401);
  });

  it('is up without a login', async () => {
    const c = await client(app);
    const res = await c.get('/actuator/health');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'UP' });
  });

  it('answers unknown paths with 401 when anonymous and 404 when logged in', async () => {
    const c = await client(app);
    expect((await c.get('/api/nothing-here')).status).toBe(401);
    await c.login('admin');
    expect((await c.get('/api/nothing-here')).status).toBe(404);
  });
});

describe('role rules', () => {
  it('lets only staff list students; a student reads their own record', async () => {
    const student = await as('sam');
    expect((await student.get('/api/students')).status).toBe(403);
    const me = await student.get('/api/students/me');
    expect(me.status).toBe(200);
    expect(me.body).toMatchObject({ id: ids.student, fullName: 'Sam Student', matriculationNumber: 'M-001' });
    const admin = await as('admin');
    expect((await admin.get('/api/students')).status).toBe(200);
  });

  it('lets only an admin register accounts', async () => {
    const teacher = await as('tina');
    const body = { username: 'newbie', email: 'newbie@x.test', password: 'abcdefg1', role: 'STUDENT' };
    expect((await teacher.post('/api/auth/register', body)).status).toBe(403);
    const admin = await as('admin');
    const created = await admin.post('/api/auth/register', body);
    expect(created.status).toBe(201);
    expect(created.body).toEqual({ username: 'newbie', email: 'newbie@x.test', role: 'STUDENT' });
    const again = await admin.post('/api/auth/register', body);
    expect(again.status).toBe(400);
    expect(again.body.message).toBe('Username already exists: newbie');
    const weak = await admin.post('/api/auth/register', { ...body, username: 'x', password: 'short' });
    expect(weak.body.details.username).toMatch(/Username must be 3-32/);
    expect(weak.body.details.password).toMatch(/at least 8 characters/);
  });
});

describe('courses', () => {
  it('pages, searches and sorts like Spring Data', async () => {
    const c = await as('sam');
    const res = await c.get('/api/courses?size=1&sort=code,desc');
    expect(res.status).toBe(200);
    expect(res.body.page).toEqual({ size: 1, number: 0, totalElements: 2, totalPages: 2 });
    expect(res.body.content[0]).toMatchObject({
      code: 'MA-201', title: 'Course MA-201', creditHours: 5, status: 'ACTIVE', active: true,
      teacherName: 'Otto Other', termId: null, termName: null,
    });
    const search = await c.get('/api/courses?q=cs-1&status=ACTIVE');
    expect(search.body.content.map((x: { code: string }) => x.code)).toEqual(['CS-101']);
    const bad = await c.get('/api/courses?sort=nope');
    expect(bad.status).toBe(400);
    expect(bad.body.message).toBe("Invalid sort property 'nope'");
    const badStatus = await c.get('/api/courses?status=WHAT');
    expect(badStatus.body.message).toBe("Invalid value for parameter 'status'");
    expect((await c.get('/api/courses/abc')).body.message).toBe("Invalid value for parameter 'id'");
    expect((await c.get('/api/courses/9999')).body).toMatchObject({ status: 404, message: 'Course not found with id: 9999' });
  });

  it('keeps the course dropdown list for staff only, limited to a teacher\'s own courses', async () => {
    expect((await (await as('sam')).get('/api/courses/options')).status).toBe(403);
    const teacher = await as('tina');
    expect((await teacher.get('/api/courses/options')).body.map((o: { code: string }) => o.code)).toEqual(['CS-101']);
    const admin = await as('admin');
    expect((await admin.get('/api/courses/options')).body).toHaveLength(2);
  });

  it('lets a teacher create courses only for themselves and edit only their own', async () => {
    const teacher = await as('tina');
    const created = await teacher.post('/api/courses', {
      code: 'PH-300', title: 'Optics', creditHours: 4, teacherId: ids.otherTeacher,
    });
    expect(created.status).toBe(201);
    expect(created.body.teacherId).toBe(ids.teacher); // pinned to the caller
    expect(created.body.status).toBe('ACTIVE');
    const foreign = await teacher.put(`/api/courses/${ids.otherCourse}`, { code: 'MA-201', title: 'x', creditHours: 5 });
    expect(foreign.status).toBe(403);
    expect(foreign.body).toMatchObject({ status: 403, message: 'Access denied' });
    expect((await teacher.delete(`/api/courses/${ids.otherCourse}`)).status).toBe(403);
    const own = await teacher.put(`/api/courses/${created.body.id}`, {
      code: 'PH-300', title: 'Optics II', creditHours: 4, version: created.body.version,
    });
    expect(own.status).toBe(200);
    expect(own.body).toMatchObject({ title: 'Optics II', version: created.body.version + 1 });
    const stale = await teacher.put(`/api/courses/${created.body.id}`, {
      code: 'PH-300', title: 'Optics III', creditHours: 4, version: created.body.version,
    });
    expect(stale.status).toBe(409);
    expect(stale.body.message).toBe('This record was changed by someone else. Reload it and try again.');
    expect((await teacher.delete(`/api/courses/${created.body.id}`)).status).toBe(204);
  });

  it('validates the course body', async () => {
    const admin = await as('admin');
    const res = await admin.post('/api/courses', { code: 'bad code', title: '', creditHours: 99 });
    expect(res.status).toBe(400);
    expect(res.body.details).toEqual({
      code: "Course code must be alphanumeric segments separated by '-' (e.g. CS-101)",
      title: 'Course title is required',
      creditHours: 'Credit hours must be at most 10',
    });
    const dupe = await admin.post('/api/courses', { code: 'CS-101', title: 'again', creditHours: 3 });
    expect(dupe.body.message).toBe('Course code already exists: CS-101');
    const malformed = await admin.raw.post('/api/courses').set('X-XSRF-TOKEN', admin.token)
      .set('Content-Type', 'application/json').send('{not json');
    expect(malformed.status).toBe(400);
    expect(malformed.body.message).toBe('Malformed request body');
    const wrongType = await admin.post('/api/courses', { code: 'ZZ-1', title: 't', creditHours: 'five' });
    expect(wrongType.body.message).toBe('Malformed request body');
  });
});

describe('terms', () => {
  it('lists for everyone, creates for admins only, and checks dates and duplicates', async () => {
    const admin = await as('admin');
    const created = await admin.post('/api/terms', { name: ' Winter 2030 ', startDate: '2030-10-01', endDate: '2031-03-31' });
    expect(created.status).toBe(201);
    expect(created.body).toMatchObject({ name: 'Winter 2030', startDate: '2030-10-01', endDate: '2031-03-31' });
    expect((await admin.post('/api/terms', { name: 'Winter 2030' })).body.message).toBe('Term already exists: Winter 2030');
    expect((await admin.post('/api/terms', { name: 'Odd', startDate: '2030-05-01', endDate: '2030-04-01' })).body.message)
      .toBe('Term end date must not be before its start date');
    const teacher = await as('tina');
    expect((await teacher.post('/api/terms', { name: 'Nope' })).status).toBe(403);
    expect((await teacher.get('/api/terms')).body[0].name).toBe('Winter 2030');
  });
});

describe('enrollments', () => {
  let enrollmentId: number;

  it('lets a student enroll themselves but nobody else', async () => {
    const sam = await as('sam');
    expect((await sam.post('/api/enrollments', { studentId: ids.otherStudent, courseId: ids.course })).status).toBe(403);
    const res = await sam.post('/api/enrollments', { studentId: ids.student, courseId: ids.course });
    expect(res.status).toBe(201);
    expect(res.body).toMatchObject({
      studentId: ids.student, studentName: 'Sam Student', courseId: ids.course, courseCode: 'CS-101',
      status: 'PENDING', grade: 'NOT_GRADED', confirmed: false, gradeSeen: true,
    });
    enrollmentId = res.body.id;
    const dupe = await sam.post('/api/enrollments', { studentId: ids.student, courseId: ids.course });
    expect(dupe.body.message).toBe('Student already enrolled in this course');
    const missing = await sam.post('/api/enrollments', { courseId: ids.course });
    expect(missing.body.details).toEqual({ studentId: 'Student ID is required' });
  });

  it('shows a student only their own list', async () => {
    const sam = await as('sam');
    expect((await sam.get(`/api/enrollments/student/${ids.student}`)).body).toHaveLength(1);
    expect((await sam.get(`/api/enrollments/student/${ids.otherStudent}`)).status).toBe(403);
    expect((await sam.get('/api/enrollments')).status).toBe(403);
  });

  it('keeps the teachers apart: only the course\'s teacher confirms and grades', async () => {
    const otto = await as('otto');
    expect((await otto.patch(`/api/enrollments/${enrollmentId}/confirm`)).status).toBe(403);
    const tina = await as('tina');
    const list = await tina.get('/api/enrollments');
    expect(list.body.content.map((e: { id: number }) => e.id)).toEqual([enrollmentId]);
    expect((await otto.get('/api/enrollments')).body.content).toHaveLength(0);

    expect((await tina.patch(`/api/enrollments/${enrollmentId}/grade`, { grade: 'A' })).body.message)
      .toBe('Can only assign grade to confirmed enrollments');
    const confirmed = await tina.patch(`/api/enrollments/${enrollmentId}/confirm`);
    expect(confirmed.body).toMatchObject({ status: 'CONFIRMED', confirmed: true });
    expect((await tina.patch(`/api/enrollments/${enrollmentId}/confirm`)).body.message).toBe('Enrollment is already confirmed');
    const graded = await tina.patch(`/api/enrollments/${enrollmentId}/grade`, { grade: 'B' });
    expect(graded.body).toMatchObject({ grade: 'B', gradeSeen: false });
    const bad = await tina.patch(`/api/enrollments/${enrollmentId}/grade`, { grade: 'Z' });
    expect(bad.status).toBe(400);
  });

  it('lets only the student acknowledge a grade, and only a pending enrollment be withdrawn', async () => {
    const sam = await as('sam');
    const sue = await as('sue');
    expect((await sue.patch(`/api/enrollments/${enrollmentId}/grade-seen`)).status).toBe(403);
    const seen = await sam.patch(`/api/enrollments/${enrollmentId}/grade-seen`);
    expect(seen.body.gradeSeen).toBe(true);
    expect((await sam.patch(`/api/enrollments/${enrollmentId}/cancel`)).status).toBe(403); // confirmed → no withdrawal
    const tina = await as('tina');
    const cancelled = await tina.patch(`/api/enrollments/${enrollmentId}/cancel`);
    expect(cancelled.body).toMatchObject({ status: 'CANCELLED', grade: 'NOT_GRADED', gradeSeen: true });
    // re-enrolling after a cancellation reuses the row
    const again = await sam.post('/api/enrollments', { studentId: ids.student, courseId: ids.course });
    expect(again.status).toBe(201);
    expect(again.body.id).toBe(enrollmentId);
    expect(again.body.status).toBe('PENDING');
    const withdrawn = await sam.patch(`/api/enrollments/${enrollmentId}/cancel`);
    expect(withdrawn.body.status).toBe('CANCELLED');
  });

  it('refuses inactive courses and lets only admins delete', async () => {
    const inactive = await addCourse(db, 'OLD-1', ids.teacher, 'INACTIVE');
    const sam = await as('sam');
    expect((await sam.post('/api/enrollments', { studentId: ids.student, courseId: inactive })).body.message)
      .toBe('Course is not active: OLD-1');
    const tina = await as('tina');
    expect((await tina.delete(`/api/enrollments/${enrollmentId}`)).status).toBe(403);
    const admin = await as('admin');
    expect((await admin.delete(`/api/enrollments/${enrollmentId}`)).status).toBe(204);
    expect((await admin.get(`/api/enrollments/${enrollmentId}`)).status).toBe(404);
  });
});

describe('students and teachers', () => {
  it('creates a student with a login, and writes the audit columns', async () => {
    const admin = await as('admin');
    const res = await admin.post('/api/students', {
      firstName: 'Nina', lastName: 'New', matriculationNumber: 'M-100', email: 'nina@x.test',
      birthDate: '2001-02-03', createAccount: true,
    });
    expect(res.status).toBe(201);
    expect(res.body).toMatchObject({ fullName: 'Nina New', birthDate: '2001-02-03', version: 0 });
    const { rows } = await db.query('select created_by, updated_by, user_id from students where id = $1', [res.body.id]);
    expect(rows[0].created_by).toBe('admin');
    expect(rows[0].user_id).not.toBeNull();
    const account = await db.query('select username, role from users where id = $1', [rows[0].user_id]);
    expect(account.rows[0]).toEqual({ username: 'nina.new'.replace('.new', ''), role: 'STUDENT' });
    // the new login works with the default password
    const nina = await client(app);
    await nina.login('nina', 'testuser12');
    expect((await nina.get('/api/students/me')).body.matriculationNumber).toBe('M-100');
    const dupe = await admin.post('/api/students', {
      firstName: 'A', lastName: 'B', matriculationNumber: 'M-100', email: 'other@x.test',
    });
    expect(dupe.body.message).toBe('Matriculation number already exists: M-100');
  });

  it('searches and pages students', async () => {
    const admin = await as('admin');
    const res = await admin.get('/api/students?q=STUD&size=5');
    expect(res.body.content.map((s: { lastName: string }) => s.lastName)).toEqual(['Student']);
    const all = await admin.get('/api/students?sort=lastName,desc&sort=firstName');
    expect(all.body.page.totalElements).toBeGreaterThanOrEqual(3);
    const options = await admin.get('/api/students/options');
    expect(options.body[0]).toEqual(expect.objectContaining({ fullName: expect.any(String), matriculationNumber: expect.any(String) }));
  });

  it('updates with optimistic locking, keeps the login e-mail in step, and blocks deleting enrolled students', async () => {
    const admin = await as('admin');
    const sam = (await admin.get(`/api/students/${ids.student}`)).body;
    const update = await admin.put(`/api/students/${ids.student}`, { ...sam, email: 'sam.new@x.test' });
    expect(update.status).toBe(200);
    expect(update.body).toMatchObject({ email: 'sam.new@x.test', version: sam.version + 1 });
    expect((await db.query("select email from users where username = 'sam'")).rows[0].email).toBe('sam.new@x.test');
    const stale = await admin.put(`/api/students/${ids.student}`, { ...sam, firstName: 'Late' });
    expect(stale.status).toBe(409);

    const tina = await as('tina');
    await (await as('sam')).post('/api/enrollments', { studentId: ids.student, courseId: ids.course });
    expect(tina).toBeDefined();
    const blocked = await admin.delete(`/api/students/${ids.student}`);
    expect(blocked.status).toBe(409);
    expect(blocked.body.message).toMatch(/still referenced by other data/);
    const gone = await admin.get('/api/students/9999');
    expect(gone.body.message).toBe('Student not found with id: 9999');
  });

  it('manages teachers; teachers can read them but not change them', async () => {
    const tina = await as('tina');
    const me = await tina.get('/api/teachers/me');
    expect(me.body).toMatchObject({ id: ids.teacher, fullName: 'Tina Teach', department: 'Physics' });
    expect((await tina.post('/api/teachers', {})).status).toBe(403);
    const admin = await as('admin');
    const bad = await admin.post('/api/teachers', { firstName: 'X', lastName: 'Y', email: 'x@y.test', department: '1' });
    expect(bad.body.details.department).toMatch(/Department must be 2-100 characters/);
    const created = await admin.post('/api/teachers', {
      firstName: 'Tom', lastName: 'Tutor', email: 'tom@x.test', department: 'Chemistry', createAccount: true, accountUsername: 'tom',
    });
    expect(created.status).toBe(201);
    expect((await admin.delete(`/api/teachers/${created.body.id}`)).status).toBe(204);
    expect((await db.query("select 1 from users where username = 'tom'")).rows).toHaveLength(0);
    expect((await (await as('sam')).get('/api/teachers/options')).status).toBe(403);
  });

  it('answers /students/me with 404 when the login has no student record', async () => {
    await addUser(db, 'loner', 'STUDENT');
    const loner = await as('loner');
    const res = await loner.get('/api/students/me');
    expect(res.status).toBe(404);
    expect(res.body.message).toBe('No student record is linked to account: loner');
  });
});

describe('profile', () => {
  it('shows and changes the profile, and keeps the session when the username changes', async () => {
    const sue = await as('sue');
    const profile = await sue.get('/api/profile');
    expect(profile.body).toMatchObject({
      username: 'sue', role: 'STUDENT', firstName: 'Sue', lastName: 'Other', matriculationNumber: 'M-002', department: null,
    });
    const updated = await sue.put('/api/profile', {
      username: 'sue2', email: 'sue2@x.test', firstName: 'Susan', lastName: 'Other', birthDate: '2000-01-01',
    });
    expect(updated.status).toBe(200);
    expect(updated.body).toMatchObject({ username: 'sue2', firstName: 'Susan', birthDate: '2000-01-01' });
    expect((await sue.get('/api/auth/me')).body.username).toBe('sue2');
    const clash = await sue.put('/api/profile', { username: 'admin', email: 'sue2@x.test', firstName: 'S', lastName: 'O' });
    expect(clash.body.message).toBe('Username already exists: admin');
    const noName = await sue.put('/api/profile', { username: 'sue2', email: 'sue2@x.test', firstName: ' ', lastName: 'O' });
    expect(noName.body.message).toBe('First and last name are required');
  });

  it('changes the password only with the current one', async () => {
    const admin = await as('admin');
    const wrong = await admin.put('/api/profile/password', { currentPassword: 'wrong-one1', newPassword: 'brandnew9' });
    expect(wrong.body.message).toBe('Current password is incorrect');
    const weak = await admin.put('/api/profile/password', { currentPassword: PASSWORD, newPassword: 'short' });
    expect(weak.body.details.newPassword).toMatch(/at least 8 characters/);
    expect((await admin.put('/api/profile/password', { currentPassword: PASSWORD, newPassword: 'brandnew9' })).status).toBe(204);
    const fresh = await client(app);
    await fresh.login('admin', 'brandnew9');
    // an admin has no student/teacher record, so the name lives on the account
    const named = await fresh.put('/api/profile', { username: 'admin', email: 'admin@x.test', firstName: 'Ada', lastName: 'Min' });
    expect(named.body).toMatchObject({ firstName: 'Ada', lastName: 'Min', matriculationNumber: null });
  });
});
