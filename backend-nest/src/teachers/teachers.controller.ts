import { Body, Controller, Delete, Get, HttpCode, Param, Post, Put, Query, Req, Res } from '@nestjs/common';
import type { Request, Response } from 'express';
import { Access, sessionUser } from '../common/access';
import { pageRequest } from '../common/paging';
import { DEPARTMENT, parseBody, pathId, PASSWORD, Schema, USERNAME } from '../common/validate';
import { TEACHER_SORTING, TeacherInput, TeachersService } from './teachers.service';

const TEACHER_SCHEMA: Schema = {
  firstName: { kind: 'string', required: 'First name is required' },
  lastName: { kind: 'string', required: 'Last name is required' },
  email: { kind: 'string', required: 'Email is required', email: 'Email is invalid' },
  department: { kind: 'string', pattern: DEPARTMENT },
  createAccount: { kind: 'boolean' },
  accountUsername: { kind: 'string', pattern: USERNAME },
  accountPassword: { kind: 'string', pattern: PASSWORD },
  version: { kind: 'long' },
};

@Controller('api/teachers')
@Access(['TEACHER', 'ADMIN'])
export class TeachersController {
  constructor(private readonly service: TeachersService) {}

  @Get()
  list(@Query() query: Record<string, unknown>) {
    const q = typeof query.q === 'string' ? query.q : null;
    return this.service.search(q, pageRequest(query, TEACHER_SORTING));
  }

  @Get('me')
  me(@Req() req: Request) {
    return this.service.findMe(sessionUser(req).username);
  }

  @Get('options')
  options() {
    return this.service.options();
  }

  @Get(':id')
  get(@Param('id') id: string) {
    return this.service.findById(pathId(id));
  }

  @Post()
  @Access(['ADMIN'])
  async create(@Body() body: unknown, @Res() res: Response) {
    res.status(201).json(await this.service.create(parseBody<TeacherInput>(body, TEACHER_SCHEMA)));
  }

  @Put(':id')
  @Access(['ADMIN'])
  update(@Param('id') id: string, @Body() body: unknown) {
    return this.service.update(pathId(id), parseBody<TeacherInput>(body, TEACHER_SCHEMA));
  }

  @Delete(':id')
  @Access(['ADMIN'])
  @HttpCode(204)
  async remove(@Param('id') id: string) {
    await this.service.delete(pathId(id));
  }
}
