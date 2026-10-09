import { Inject, Injectable, Logger } from '@nestjs/common';
import { CoursesService } from '../courses/courses.service';
import { Db, DB } from '../db/db';
import { StudentsService } from '../students/students.service';
import { TeachersService } from '../teachers/teachers.service';
import { isAdmin, SessionUser } from './access';

export const NO_TEACHER = -1;

/** Port of OwnershipGuard: who may touch which student / course / enrollment. */
@Injectable()
export class OwnershipService {
  private readonly log = new Logger('Ownership');

  constructor(
    @Inject(DB) private readonly db: Db,
    private readonly students: StudentsService,
    private readonly teachers: TeachersService,
    private readonly courses: CoursesService,
  ) {}

  private isStaff(u: SessionUser) {
    return u.role === 'TEACHER' || u.role === 'ADMIN';
  }

  async canAccessStudentData(studentId: number, u: SessionUser): Promise<boolean> {
    if (this.isStaff(u)) return true;
    const owns = await this.students.accountOwnsStudent(u.username, studentId);
    if (!owns) this.log.warn(`Blocked cross-student access: ${u.username} tried to read student ${studentId}`);
    return owns;
  }

  async canAccessCourseData(courseId: number, u: SessionUser): Promise<boolean> {
    if (isAdmin(u)) return true;
    if (!(await this.courses.exists(courseId))) return true; // let the service answer 404
    return this.teachesCourse(courseId, u);
  }

  async canManageCourse(courseId: number, u: SessionUser): Promise<boolean> {
    if (isAdmin(u)) return true;
    if (!(await this.courses.exists(courseId))) return true;
    const teaches = await this.teachesCourse(courseId, u);
    if (!teaches) this.log.warn(`Blocked cross-course edit: ${u.username} tried to change course ${courseId}`);
    return teaches;
  }

  async canManageEnrollment(enrollmentId: number, u: SessionUser): Promise<boolean> {
    if (isAdmin(u)) return true;
    if (!(await this.enrollmentExists(enrollmentId))) return true;
    const teacherId = await this.teachers.findIdByAccountUsername(u.username);
    if (teacherId === null) return false;
    const { rows } = await this.db.query(
      'select 1 from enrollments e join courses c on c.id = e.course_id where e.id = $1 and c.teacher_id = $2',
      [enrollmentId, teacherId],
    );
    if (rows.length === 0) {
      this.log.warn(`Blocked cross-course enrollment management: ${u.username} tried to act on enrollment ${enrollmentId}`);
    }
    return rows.length > 0;
  }

  async canEnroll(studentId: number | null, courseId: number | null, u: SessionUser): Promise<boolean> {
    if (studentId === null || courseId === null) return false;
    if (isAdmin(u)) return true;
    if (u.role === 'TEACHER') return this.canManageCourse(courseId, u);
    return this.canAccessStudentData(studentId, u);
  }

  async canCancelEnrollment(enrollmentId: number, u: SessionUser): Promise<boolean> {
    if (u.role === 'STUDENT') {
      const { rows } = await this.db.query('select status, student_id from enrollments where id = $1', [enrollmentId]);
      if (!rows[0]) return true;
      return rows[0].status === 'PENDING' && (await this.students.accountOwnsStudent(u.username, rows[0].student_id));
    }
    return this.canManageEnrollment(enrollmentId, u);
  }

  async canAcknowledgeGrade(enrollmentId: number, u: SessionUser): Promise<boolean> {
    const { rows } = await this.db.query('select student_id from enrollments where id = $1', [enrollmentId]);
    if (!rows[0]) return true;
    return this.students.accountOwnsStudent(u.username, rows[0].student_id);
  }

  /** null = no restriction (admin); NO_TEACHER = a teacher login without a teacher record; else the teacher's id. */
  async teacherScope(u: SessionUser): Promise<number | null> {
    if (isAdmin(u)) return null;
    return (await this.teachers.findIdByAccountUsername(u.username)) ?? NO_TEACHER;
  }

  private async teachesCourse(courseId: number, u: SessionUser): Promise<boolean> {
    const teacherId = await this.teachers.findIdByAccountUsername(u.username);
    return teacherId !== null && (await this.courses.taughtBy(courseId, teacherId));
  }

  private async enrollmentExists(id: number): Promise<boolean> {
    return (await this.db.query('select 1 from enrollments where id = $1', [id])).rows.length > 0;
  }
}
