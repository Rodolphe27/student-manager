import type { NestExpressApplication } from '@nestjs/platform-express';
import { AssistantClient, ChatMessage, Toolbox } from '../src/chat/assistant.client';
import { addCourse, addStudent, addTeacher, addUser, client, newDb, startApp, TestDb } from './helpers';

/** Stands in for Claude: runs a scripted list of tool calls, then answers with what the tools returned. */
class FakeAssistant extends AssistantClient {
  configured = true;
  script: Array<[string, Record<string, unknown>]> = [];
  seen: { system: string; history: ChatMessage[]; toolNames: string[]; results: string[] } | null = null;

  isConfigured() {
    return this.configured;
  }

  async reply(system: string, history: ChatMessage[], tools: Toolbox) {
    const results: string[] = [];
    for (const [name, input] of this.script) results.push(await tools.run(name, input));
    this.seen = { system, history, toolNames: tools.specs().map((s) => s.name), results };
    return `done: ${results.length} tool call(s)`;
  }
}

let db: TestDb;
let app: NestExpressApplication;
const fake = new FakeAssistant();
let courseId: number;
let studentId: number;

beforeAll(async () => {
  db = await newDb();
  app = await startApp(db, { assistant: fake });
  await addUser(db, 'admin', 'ADMIN');
  const teacher = await addTeacher(db, 'tina');
  await addTeacher(db, 'otto');
  studentId = await addStudent(db, 'sam', 'M-1');
  await addStudent(db, 'sue', 'M-2');
  courseId = await addCourse(db, 'CS-101', teacher);
  await addCourse(db, 'OLD-9', teacher, 'ARCHIVED');
}, 300_000);

afterAll(async () => {
  await app.close();
  await db.close();
}, 120_000);

beforeEach(() => {
  fake.configured = true;
  fake.script = [];
  fake.seen = null;
});

const as = async (username: string) => {
  const c = await client(app);
  await c.login(username);
  return c;
};
const ask = (text = 'hi') => ({ messages: [{ role: 'user', content: text }] });

describe('chat', () => {
  it('reports whether the assistant is configured and hides nothing else from the client', async () => {
    const sam = await as('sam');
    expect((await sam.get('/api/chat/status')).body).toEqual({ enabled: true });
    fake.configured = false;
    expect((await sam.get('/api/chat/status')).body).toEqual({ enabled: false });
    const res = await sam.post('/api/chat', ask());
    expect(res.status).toBe(503);
    expect(res.body.message).toBe('The assistant is not set up on this server.');
  });

  it('needs a login', async () => {
    const anon = await client(app);
    expect((await anon.get('/api/chat/status')).status).toBe(401);
    expect((await anon.post('/api/chat', ask())).status).toBe(401);
  });

  it('validates the conversation', async () => {
    const sam = await as('sam');
    expect((await sam.post('/api/chat', { messages: [] })).body.details).toEqual({ messages: 'must not be empty' });
    expect((await sam.post('/api/chat', {})).status).toBe(400);
    const endsWithAssistant = await sam.post('/api/chat', {
      messages: [{ role: 'user', content: 'a' }, { role: 'assistant', content: 'b' }],
    });
    expect(endsWithAssistant.body.message).toBe('The conversation must start and end with a message from the user');
    const startsWithAssistant = await sam.post('/api/chat', {
      messages: [{ role: 'assistant', content: 'b' }, { role: 'user', content: 'a' }],
    });
    expect(startsWithAssistant.status).toBe(400);
    const bad = await sam.post('/api/chat', { messages: [{ role: 'system', content: 'x'.repeat(2001) }] });
    expect(bad.body.details).toEqual({
      'messages[0].role': 'role must be user or assistant',
      'messages[0].content': 'message is too long',
    });
    const tooMany = await sam.post('/api/chat', {
      messages: Array.from({ length: 21 }, () => ({ role: 'user', content: 'x' })),
    });
    expect(tooMany.body.details).toEqual({ messages: 'conversation is too long' });
  });

  it('lets a student look things up and propose; nothing happens until the student confirms', async () => {
    const sam = await as('sam');
    fake.script = [['search_courses', { query: 'cs' }], ['propose_enrollment', { courseId }]];
    const res = await sam.post('/api/chat', ask('Enroll me in CS-101'));
    expect(res.status).toBe(200);
    expect(res.body.reply).toBe('done: 2 tool call(s)');
    expect(res.body.pendingActions).toHaveLength(1);
    expect(res.body.pendingActions[0]).toMatchObject({ type: 'ENROLL', description: 'Enroll in CS-101 Course CS-101' });
    expect(JSON.parse(fake.seen!.results[0])).toEqual([
      expect.objectContaining({ code: 'CS-101', status: 'ACTIVE', teacher: 'Tina Teach' }),
    ]);
    expect(fake.seen!.system).toContain('"sam" with the role STUDENT');
    expect((await db.query('select count(*) as n from enrollments')).rows[0].n).toBe(0);

    const actionId = res.body.pendingActions[0].id;
    // somebody else cannot use the proposal
    const sue = await as('sue');
    expect((await sue.post('/api/chat/confirm', { actionId })).status).toBe(404);

    const done = await sam.post('/api/chat/confirm', { actionId });
    expect(done.status).toBe(200);
    expect(done.body.message).toBe('Enrolled in CS-101 Course CS-101. Status: PENDING.');
    const enrollment = (await db.query('select student_id, status from enrollments')).rows[0];
    expect(enrollment).toEqual({ student_id: studentId, status: 'PENDING' });
    // a proposal works once
    const again = await sam.post('/api/chat/confirm', { actionId });
    expect(again.status).toBe(404);
    expect(again.body.message).toBe('This proposal has expired or was already used');
  });

  it('proposes a cancellation for the student\'s own pending enrollment only', async () => {
    const sam = await as('sam');
    const { id: enrollmentId } = (await db.query('select id from enrollments')).rows[0];
    fake.script = [['get_my_enrollments', {}], ['propose_cancel_enrollment', { enrollmentId }]];
    const res = await sam.post('/api/chat', ask('Cancel it'));
    expect(JSON.parse(fake.seen!.results[0])[0]).toMatchObject({ id: enrollmentId, status: 'PENDING', course: 'CS-101 Course CS-101' });
    expect(res.body.pendingActions[0].description).toBe('Cancel the enrollment in CS-101 Course CS-101');

    // another student is told no, and gets no proposal
    const sue = await as('sue');
    fake.script = [['propose_cancel_enrollment', { enrollmentId }]];
    const denied = await sue.post('/api/chat', ask('Cancel his'));
    expect(denied.body.pendingActions).toEqual([]);
    expect(fake.seen!.results[0]).toMatch(/^Error: you are not allowed to cancel this enrollment/);

    const done = await sam.post('/api/chat/confirm', { actionId: res.body.pendingActions[0].id });
    expect(done.body.message).toBe('Cancelled the enrollment in CS-101 Course CS-101.');
    expect((await db.query('select status from enrollments')).rows[0].status).toBe('CANCELLED');
  });

  it('re-checks the rights when the proposal is confirmed', async () => {
    const sam = await as('sam');
    fake.script = [['propose_enrollment', { courseId }]];
    const res = await sam.post('/api/chat', ask('Enroll me'));
    expect(res.body.pendingActions).toHaveLength(1);
    // the course is archived before the student presses confirm
    await db.query("update courses set status = 'ARCHIVED' where id = $1", [courseId]);
    const done = await sam.post('/api/chat/confirm', { actionId: res.body.pendingActions[0].id });
    expect(done.status).toBe(400);
    expect(done.body.message).toBe('Course is not active: CS-101');
    await db.query("update courses set status = 'ACTIVE' where id = $1", [courseId]);
  });

  it('gives staff their own tool set and keeps enrollment proposals for students', async () => {
    const tina = await as('tina');
    fake.script = [['propose_enrollment', { courseId }], ['get_my_enrollments', {}], ['no_such_tool', {}]];
    const res = await tina.post('/api/chat', ask('hello'));
    expect(res.body.pendingActions).toEqual([]);
    expect(fake.seen!.toolNames).toEqual(['search_courses', 'get_my_enrollments', 'propose_cancel_enrollment']);
    expect(fake.seen!.results[0]).toBe('Error: only students can enroll themselves through the assistant.');
    expect(JSON.parse(fake.seen!.results[1])[0]).toMatchObject({ student: 'Sam Student' });
    expect(fake.seen!.results[2]).toBe('Error: unknown tool no_such_tool');
    // the other teacher sees no enrollments of Tina's course
    const otto = await as('otto');
    fake.script = [['get_my_enrollments', {}]];
    await otto.post('/api/chat', ask('list'));
    expect(JSON.parse(fake.seen!.results[0])).toEqual([]);
  });

  it('explains bad tool input instead of failing', async () => {
    const sam = await as('sam');
    fake.script = [
      ['propose_enrollment', { courseId: 'one' }],
      ['propose_enrollment', { courseId: 99999 }],
      ['search_courses', { status: 'BOGUS' }],
    ];
    await sam.post('/api/chat', ask('x'));
    expect(fake.seen!.results).toEqual([
      'Error: courseId must be a whole number',
      'Error: Course not found with id: 99999',
      'Error: status must be ACTIVE, INACTIVE or ARCHIVED',
    ]);
  });

  it('limits a user to ten messages a minute', async () => {
    const admin = await as('admin');
    for (let i = 0; i < 10; i++) expect((await admin.post('/api/chat', ask())).status).toBe(200);
    const res = await admin.post('/api/chat', ask());
    expect(res.status).toBe(429);
    expect(res.body.message).toBe('Too many messages. Please wait a moment and try again.');
  });
});
