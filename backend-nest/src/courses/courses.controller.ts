import { Body, Controller, Delete, Get, HttpCode, Param, Post, Put, Query, Req, Res } from '@nestjs/common';
import type { Request, Response } from 'express';
import { Access, sessionUser } from '../common/access';
import { forbidden } from '../common/errors';
import { pageRequest } from '../common/paging';
import { enumParam, parseBody, pathId, Schema } from '../common/validate';
import { OwnershipService, NO_TEACHER } from '../common/ownership.service';
import { COURSE_SORTING, COURSE_STATUSES, CourseInput, CoursesService } from './courses.service';

const COURSE_SCHEMA: Schema = {
  code: {
    kind: 'string',
    required: 'Course code is required',
    pattern: [/^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$/, "Course code must be alphanumeric segments separated by '-' (e.g. CS-101)"],
  },
  title: { kind: 'string', required: 'Course title is required' },
  description: { kind: 'string' },
  creditHours: {
    kind: 'int',
    min: [1, 'Credit hours must be at least 1'],
    max: [10, 'Credit hours must be at most 10'],
  },
  status: { kind: 'enum', values: COURSE_STATUSES },
  teacherId: { kind: 'long' },
  termId: { kind: 'long' },
  version: { kind: 'long' },
};

@Controller('api/courses')
@Access('authenticated')
export class CoursesController {
  constructor(
    private readonly service: CoursesService,
    private readonly ownership: OwnershipService,
  ) {}

  @Get()
  list(@Query() query: Record<string, unknown>) {
    const q = typeof query.q === 'string' ? query.q : null;
    const status = enumParam(query.status, COURSE_STATUSES, 'status');
    return this.service.search(q, status, pageRequest(query, COURSE_SORTING));
  }

  @Get('options')
  @Access(['TEACHER', 'ADMIN'])
  async options(@Req() req: Request) {
    return this.service.options(await this.ownership.teacherScope(sessionUser(req)));
  }

  @Get('status/:status')
  byStatus(@Param('status') status: string) {
    return this.service.findByStatus(enumParam(status, COURSE_STATUSES, 'status')!);
  }

  @Get(':id')
  get(@Param('id') id: string) {
    return this.service.findById(pathId(id));
  }

  @Post()
  @Access(['TEACHER', 'ADMIN'])
  async create(@Body() body: unknown, @Req() req: Request, @Res() res: Response) {
    const input = parseBody<CourseInput>(body, COURSE_SCHEMA);
    await this.pinTeacher(input, req);
    res.status(201).json(await this.service.create(input));
  }

  @Put(':id')
  @Access(['TEACHER', 'ADMIN'])
  async update(@Param('id') id: string, @Body() body: unknown, @Req() req: Request) {
    const courseId = pathId(id);
    const input = parseBody<CourseInput>(body, COURSE_SCHEMA);
    if (!(await this.ownership.canManageCourse(courseId, sessionUser(req)))) throw forbidden();
    await this.pinTeacher(input, req);
    return this.service.update(courseId, input);
  }

  @Delete(':id')
  @Access(['TEACHER', 'ADMIN'])
  @HttpCode(204)
  async remove(@Param('id') id: string, @Req() req: Request) {
    const courseId = pathId(id);
    if (!(await this.ownership.canManageCourse(courseId, sessionUser(req)))) throw forbidden();
    await this.service.delete(courseId);
  }

  /** A teacher can only create/edit their own courses: the teacher is forced to their own record. */
  private async pinTeacher(input: CourseInput, req: Request) {
    const scope = await this.ownership.teacherScope(sessionUser(req));
    if (scope === null) return;
    if (scope === NO_TEACHER) throw forbidden();
    input.teacherId = scope;
  }
}
