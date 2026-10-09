import { Inject, Injectable, Logger } from '@nestjs/common';
import { notFound, validation } from '../common/errors';
import { PageOf, PageRequest, Sorting, toPage } from '../common/paging';
import { currentActor, Db, DB, NOW, Row } from '../db/db';

export const ENROLLMENT_STATUSES = ['PENDING', 'CONFIRMED', 'CANCELLED'] as const;
export const GRADES = ['A', 'B', 'C', 'D', 'F', 'NOT_GRADED'] as const;
export type EnrollmentStatus = (typeof ENROLLMENT_STATUSES)[number];
export type Grade = (typeof GRADES)[number];

export interface EnrollmentDto {
  id: number;
  studentId: number;
  studentName: string;
  courseId: number;
  courseTitle: string;
  courseCode: string;
  enrolledAt: string;
  status: EnrollmentStatus;
  grade: Grade;
  confirmed: boolean;
  gradeSeen: boolean;
}

export const ENROLLMENT_SORTING: Sorting = {
  columns: {
    id: 'e.id',
    enrolledAt: 'e.enrolled_at',
    status: 'e.status',
    grade: 'e.grade',
    gradeSeen: 'e.grade_seen',
    version: 'e.version',
    createdAt: 'e.created_at',
    updatedAt: 'e.updated_at',
    createdBy: 'e.created_by',
    updatedBy: 'e.updated_by',
    'student.id': 'e.student_id',
    'student.firstName': 's.first_name',
    'student.lastName': 's.last_name',
    'student.matriculationNumber': 's.matriculation_number',
    'course.id': 'e.course_id',
    'course.code': 'c.code',
    'course.title': 'c.title',
  },
  defaults: [
    ['e.enrolled_at', 'desc'],
    ['e.id', 'desc'],
  ],
};

const SELECT = `select e.id, e.student_id, s.first_name, s.last_name, e.course_id, c.title, c.code,
       e.enrolled_at, e.status, e.grade, e.grade_seen
  from enrollments e join students s on s.id = e.student_id join courses c on c.id = e.course_id`;

const toDto = (r: Row): EnrollmentDto => ({
  id: r.id,
  studentId: r.student_id,
  studentName: `${r.first_name} ${r.last_name}`,
  courseId: r.course_id,
  courseTitle: r.title,
  courseCode: r.code,
  enrolledAt: r.enrolled_at,
  status: r.status,
  grade: r.grade,
  confirmed: r.status === 'CONFIRMED',
  gradeSeen: r.grade_seen,
});

/** Today in UTC as yyyy-mm-dd (LocalDate.now() on the server). */
const today = () => new Date().toISOString().slice(0, 10);

@Injectable()
export class EnrollmentsService {
  private readonly log = new Logger('Enrollments');

  constructor(@Inject(DB) private readonly db: Db) {}

  async search(
    filter: { status: EnrollmentStatus | null; studentId: number | null; courseId: number | null; teacherId: number | null },
    page: PageRequest,
  ): Promise<PageOf<EnrollmentDto>> {
    const params: unknown[] = [];
    const conditions: string[] = [];
    const add = (sql: string, value: unknown) => {
      params.push(value);
      conditions.push(sql.replace('?', `$${params.length}`));
    };
    if (filter.status) add('e.status = ?', filter.status);
    if (filter.studentId !== null) add('e.student_id = ?', filter.studentId);
    if (filter.courseId !== null) add('e.course_id = ?', filter.courseId);
    if (filter.teacherId !== null) add('c.teacher_id = ?', filter.teacherId);
    const where = conditions.length ? `where ${conditions.join(' and ')}` : '';
    const from = `from enrollments e join students s on s.id = e.student_id join courses c on c.id = e.course_id`;
    const total = (await this.db.query(`select count(*) as n ${from} ${where}`, params)).rows[0].n as number;
    const { rows } = await this.db.query(
      `${SELECT} ${where} order by ${page.orderBy} limit ${page.size} offset ${page.page * page.size}`,
      params,
    );
    return toPage(rows.map(toDto), total, page);
  }

  async findById(id: number): Promise<EnrollmentDto> {
    const { rows } = await this.db.query(`${SELECT} where e.id = $1`, [id]);
    if (!rows[0]) throw notFound('Enrollment', id);
    return toDto(rows[0]);
  }

  async findByStudentId(studentId: number): Promise<EnrollmentDto[]> {
    const { rows } = await this.db.query(`${SELECT} where e.student_id = $1 order by e.id`, [studentId]);
    return rows.map(toDto);
  }

  async findByCourseId(courseId: number): Promise<EnrollmentDto[]> {
    const { rows } = await this.db.query(`${SELECT} where e.course_id = $1 order by e.id`, [courseId]);
    return rows.map(toDto);
  }

  async create(studentId: number, courseId: number): Promise<EnrollmentDto> {
    this.log.log(`Enrolling student ${studentId} in course ${courseId}`);
    const student = await this.db.query('select 1 from students where id = $1', [studentId]);
    if (!student.rows[0]) throw notFound('Student', studentId);
    const course = await this.db.query('select code, status from courses where id = $1', [courseId]);
    if (!course.rows[0]) throw notFound('Course', courseId);
    if (course.rows[0].status !== 'ACTIVE') throw validation(`Course is not active: ${course.rows[0].code}`);

    const previous = await this.db.query(
      'select id, status from enrollments where student_id = $1 and course_id = $2',
      [studentId, courseId],
    );
    if (previous.rows[0] && previous.rows[0].status !== 'CANCELLED') {
      throw validation('Student already enrolled in this course');
    }
    let id: number;
    if (previous.rows[0]) {
      id = previous.rows[0].id;
      await this.db.query(
        `update enrollments set enrolled_at = $2, status = 'PENDING', grade = 'NOT_GRADED',
                updated_at = ${NOW}, updated_by = $3, version = version + 1 where id = $1`,
        [id, today(), currentActor()],
      );
    } else {
      const inserted = await this.db.query(
        `insert into enrollments (created_at, updated_at, created_by, updated_by, version,
                                  student_id, course_id, enrolled_at, status, grade, grade_seen)
         values (${NOW}, ${NOW}, $1, $1, 0, $2, $3, $4, 'PENDING', 'NOT_GRADED', true) returning id`,
        [currentActor(), studentId, courseId, today()],
      );
      id = inserted.rows[0].id;
    }
    return this.findById(id);
  }

  async confirm(id: number): Promise<EnrollmentDto> {
    const e = await this.load(id);
    if (e.status === 'CANCELLED') throw validation('Cannot confirm a cancelled enrollment');
    if (e.status === 'CONFIRMED') throw validation('Enrollment is already confirmed');
    await this.touch(id, `status = 'CONFIRMED'`);
    return this.findById(id);
  }

  async cancel(id: number): Promise<EnrollmentDto> {
    const e = await this.load(id);
    if (e.status === 'CANCELLED') throw validation('Enrollment is already cancelled');
    await this.touch(id, `grade = 'NOT_GRADED', grade_seen = true, status = 'CANCELLED'`);
    return this.findById(id);
  }

  async updateGrade(id: number, grade: Grade): Promise<EnrollmentDto> {
    const e = await this.load(id);
    if (e.status !== 'CONFIRMED') throw validation('Can only assign grade to confirmed enrollments');
    if (grade !== e.grade) {
      await this.touch(id, `grade = '${grade}', grade_seen = ${grade === 'NOT_GRADED'}`);
    }
    return this.findById(id);
  }

  async markGradeSeen(id: number): Promise<EnrollmentDto> {
    await this.load(id);
    await this.touch(id, 'grade_seen = true');
    return this.findById(id);
  }

  async delete(id: number): Promise<void> {
    const { rowCount } = await this.db.query('delete from enrollments where id = $1', [id]);
    if (rowCount === 0) throw notFound('Enrollment', id);
  }

  private async load(id: number): Promise<Row> {
    const { rows } = await this.db.query('select id, status, grade from enrollments where id = $1', [id]);
    if (!rows[0]) throw notFound('Enrollment', id);
    return rows[0];
  }

  /** `assignments` is built from constants/enum values only, never from request text. */
  private async touch(id: number, assignments: string): Promise<void> {
    await this.db.query(
      `update enrollments set ${assignments}, updated_at = ${NOW}, updated_by = $2, version = version + 1 where id = $1`,
      [id, currentActor()],
    );
  }
}
