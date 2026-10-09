import { Body, Controller, Delete, Get, HttpCode, Param, Patch, Post, Query, Req, Res } from '@nestjs/common';
import type { Request, Response } from 'express';
import { Access, sessionUser } from '../common/access';
import { forbidden } from '../common/errors';
import { OwnershipService } from '../common/ownership.service';
import { pageRequest } from '../common/paging';
import { enumParam, longParam, parseBody, pathId } from '../common/validate';
import {
  ENROLLMENT_SORTING,
  ENROLLMENT_STATUSES,
  EnrollmentsService,
  Grade,
  GRADES,
} from './enrollments.service';

/** Mirrors SecurityConfig + the @PreAuthorize rules of the Spring EnrollmentController. */
@Controller('api/enrollments')
@Access(['TEACHER', 'ADMIN'])
export class EnrollmentsController {
  constructor(
    private readonly service: EnrollmentsService,
    private readonly ownership: OwnershipService,
  ) {}

  @Get()
  async list(@Query() query: Record<string, unknown>, @Req() req: Request) {
    const filter = {
      status: enumParam(query.status, ENROLLMENT_STATUSES, 'status'),
      studentId: longParam(query.studentId, 'studentId'),
      courseId: longParam(query.courseId, 'courseId'),
      teacherId: await this.ownership.teacherScope(sessionUser(req)),
    };
    return this.service.search(filter, pageRequest(query, ENROLLMENT_SORTING));
  }

  @Get('student/:studentId')
  @Access('authenticated')
  async byStudent(@Param('studentId') raw: string, @Req() req: Request) {
    const studentId = pathId(raw, 'studentId');
    if (!(await this.ownership.canAccessStudentData(studentId, sessionUser(req)))) throw forbidden();
    return this.service.findByStudentId(studentId);
  }

  @Get('course/:courseId')
  async byCourse(@Param('courseId') raw: string, @Req() req: Request) {
    const courseId = pathId(raw, 'courseId');
    if (!(await this.ownership.canAccessCourseData(courseId, sessionUser(req)))) throw forbidden();
    return this.service.findByCourseId(courseId);
  }

  @Get(':id')
  get(@Param('id') id: string) {
    return this.service.findById(pathId(id));
  }

  @Post()
  @Access(['STUDENT', 'TEACHER', 'ADMIN'])
  async create(@Body() body: unknown, @Req() req: Request, @Res() res: Response) {
    const r = parseBody<{ studentId: number; courseId: number }>(body, {
      studentId: { kind: 'long', required: 'Student ID is required' },
      courseId: { kind: 'long', required: 'Course ID is required' },
    });
    if (!(await this.ownership.canEnroll(r.studentId, r.courseId, sessionUser(req)))) throw forbidden();
    res.status(201).json(await this.service.create(r.studentId, r.courseId));
  }

  @Patch(':id/confirm')
  async confirm(@Param('id') raw: string, @Req() req: Request) {
    const id = pathId(raw);
    if (!(await this.ownership.canManageEnrollment(id, sessionUser(req)))) throw forbidden();
    return this.service.confirm(id);
  }

  @Patch(':id/cancel')
  @Access(['STUDENT', 'TEACHER', 'ADMIN'])
  async cancel(@Param('id') raw: string, @Req() req: Request) {
    const id = pathId(raw);
    if (!(await this.ownership.canCancelEnrollment(id, sessionUser(req)))) throw forbidden();
    return this.service.cancel(id);
  }

  @Patch(':id/grade')
  async grade(@Param('id') raw: string, @Body() body: unknown, @Req() req: Request) {
    const id = pathId(raw);
    const r = parseBody<{ grade: Grade }>(body, {
      grade: { kind: 'enum', values: GRADES, required: 'Grade is required' },
    });
    if (!(await this.ownership.canManageEnrollment(id, sessionUser(req)))) throw forbidden();
    return this.service.updateGrade(id, r.grade);
  }

  @Patch(':id/grade-seen')
  @Access(['STUDENT'])
  async gradeSeen(@Param('id') raw: string, @Req() req: Request) {
    const id = pathId(raw);
    if (!(await this.ownership.canAcknowledgeGrade(id, sessionUser(req)))) throw forbidden();
    return this.service.markGradeSeen(id);
  }

  @Delete(':id')
  @Access(['ADMIN'])
  @HttpCode(204)
  async remove(@Param('id') id: string) {
    await this.service.delete(pathId(id));
  }
}
