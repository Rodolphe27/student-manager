import { ApiError } from '../common/errors';
import type { SessionUser } from '../common/access';
import { OwnershipService } from '../common/ownership.service';
import { CourseStatus, COURSE_STATUSES, CoursesService } from '../courses/courses.service';
import { EnrollmentsService } from '../enrollments/enrollments.service';
import { StudentsService } from '../students/students.service';
import { PendingAction, PendingActionStore } from './pending-action.store';
import type { Toolbox, ToolSpec } from './assistant.client';

const COURSE_LIMIT = 10;
const ENROLLMENT_LIMIT = 20;

/** What the assistant may do for one signed-in user. Reads use the user's rights; writes are only proposed. */
export class AssistantToolbox implements Toolbox {
  private readonly proposedActions: PendingAction[] = [];

  constructor(
    private readonly courses: CoursesService,
    private readonly enrollments: EnrollmentsService,
    private readonly students: StudentsService,
    private readonly ownership: OwnershipService,
    private readonly store: PendingActionStore,
    private readonly user: SessionUser,
  ) {}

  proposed(): PendingAction[] {
    return [...this.proposedActions];
  }

  private isStudent() {
    return this.user.role === 'STUDENT';
  }

  specs(): ToolSpec[] {
    const specs: ToolSpec[] = [
      {
        name: 'search_courses',
        description: `Search the course catalogue by title or code. Returns at most ${COURSE_LIMIT} courses.`,
        properties: {
          query: { type: 'string', description: 'Text to look for in the title or code. Leave out to list courses.' },
          status: { type: 'string', enum: [...COURSE_STATUSES], description: 'Only courses with this status. Usually ACTIVE.' },
        },
        required: [],
      },
      {
        name: 'get_my_enrollments',
        description: this.isStudent()
          ? "List the signed-in student's own enrollments with status and grade."
          : 'List recent enrollments in the courses the signed-in user may manage.',
        properties: {},
        required: [],
      },
    ];
    if (this.isStudent()) {
      specs.push({
        name: 'propose_enrollment',
        description: 'Propose enrolling the signed-in student in a course. Nothing happens until the student confirms.',
        properties: { courseId: { type: 'integer', description: 'Id of the course, from search_courses.' } },
        required: ['courseId'],
      });
    }
    specs.push({
      name: 'propose_cancel_enrollment',
      description: 'Propose cancelling an enrollment. Nothing happens until the user confirms.',
      properties: { enrollmentId: { type: 'integer', description: 'Id of the enrollment, from get_my_enrollments.' } },
      required: ['enrollmentId'],
    });
    return specs;
  }

  async run(name: string, input: Record<string, unknown>): Promise<string> {
    try {
      switch (name) {
        case 'search_courses':
          return await this.searchCourses(input);
        case 'get_my_enrollments':
          return await this.myEnrollments();
        case 'propose_enrollment':
          return await this.proposeEnrollment(id(input, 'courseId'));
        case 'propose_cancel_enrollment':
          return await this.proposeCancel(id(input, 'enrollmentId'));
        default:
          return `Error: unknown tool ${name}`;
      }
    } catch (error) {
      // Not-found and validation problems are explained to the model; anything else is a real failure.
      if (error instanceof ApiError && (error.status === 404 || error.status === 400)) return `Error: ${error.message}`;
      throw error;
    }
  }

  private async searchCourses(input: Record<string, unknown>): Promise<string> {
    const query = typeof input.query === 'string' && input.query.trim() !== '' ? input.query.trim() : null;
    let status: CourseStatus | null = null;
    if (typeof input.status === 'string' && input.status.trim() !== '') {
      if (!(COURSE_STATUSES as readonly string[]).includes(input.status)) {
        throw new ApiError(400, 'status must be ACTIVE, INACTIVE or ARCHIVED');
      }
      status = input.status as CourseStatus;
    }
    const page = await this.courses.search(query, status, { page: 0, size: COURSE_LIMIT, orderBy: 'c.title asc' });
    return JSON.stringify(
      page.content.map((c) => ({
        id: c.id, code: c.code, title: c.title, creditHours: c.creditHours, status: c.status,
        teacher: c.teacherName, term: c.termName,
      })),
    );
  }

  private async myEnrollments(): Promise<string> {
    const student = this.isStudent();
    const found = student
      ? await this.enrollments.findByStudentId((await this.students.findByAccountUsername(this.user.username)).id)
      : (
          await this.enrollments.search(
            { status: null, studentId: null, courseId: null, teacherId: await this.ownership.teacherScope(this.user) },
            { page: 0, size: ENROLLMENT_LIMIT, orderBy: 'e.enrolled_at desc, e.id desc' },
          )
        ).content;
    return JSON.stringify(
      found.slice(0, ENROLLMENT_LIMIT).map((e) => ({
        id: e.id,
        ...(student ? {} : { student: e.studentName }),
        course: `${e.courseCode} ${e.courseTitle}`,
        courseId: e.courseId, status: e.status, grade: e.grade, enrolledAt: e.enrolledAt,
      })),
    );
  }

  private async proposeEnrollment(courseId: number): Promise<string> {
    if (!this.isStudent()) return 'Error: only students can enroll themselves through the assistant.';
    const studentId = (await this.students.findByAccountUsername(this.user.username)).id;
    const course = await this.courses.findById(courseId);
    if (!(await this.ownership.canEnroll(studentId, courseId, this.user))) {
      return 'Error: you are not allowed to enroll in this course.';
    }
    const action = this.store.propose(this.user.username, 'ENROLL', { courseId }, `Enroll in ${course.code} ${course.title}`);
    this.proposedActions.push(action);
    return `Proposal prepared: "${action.description}". The student must press the confirm button; tell them so. It is not done yet.`;
  }

  private async proposeCancel(enrollmentId: number): Promise<string> {
    if (!(await this.ownership.canCancelEnrollment(enrollmentId, this.user))) {
      return 'Error: you are not allowed to cancel this enrollment (students can only cancel their own pending enrollments).';
    }
    const e = await this.enrollments.findById(enrollmentId);
    const action = this.store.propose(
      this.user.username, 'CANCEL', { enrollmentId },
      `Cancel the enrollment in ${e.courseCode} ${e.courseTitle}${this.isStudent() ? '' : ` for ${e.studentName}`}`,
    );
    this.proposedActions.push(action);
    return `Proposal prepared: "${action.description}". The user must press the confirm button; tell them so. It is not done yet.`;
  }
}

function id(input: Record<string, unknown>, key: string): number {
  const value = input[key];
  if (typeof value === 'number' && Number.isInteger(value)) return value;
  throw new ApiError(400, `${key} must be a whole number`);
}
