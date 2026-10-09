import { Body, Controller, Delete, Get, HttpCode, Param, Post, Put, Query, Req, Res } from '@nestjs/common';
import type { Request, Response } from 'express';
import { Access, sessionUser } from '../common/access';
import { pageRequest } from '../common/paging';
import { parseBody, pathId, Schema, PASSWORD, USERNAME } from '../common/validate';
import { STUDENT_SORTING, StudentInput, StudentsService } from './students.service';

export const STUDENT_SCHEMA: Schema = {
  firstName: { kind: 'string', required: 'First name is required' },
  lastName: { kind: 'string', required: 'Last name is required' },
  matriculationNumber: {
    kind: 'string',
    required: 'Matriculation number is required',
    pattern: [/^[A-Za-z0-9-]{2,40}$/, "Matriculation number must be 2-40 characters: letters, digits or '-'"],
  },
  birthDate: { kind: 'date' },
  email: { kind: 'string', required: 'Email is required', email: 'Email is invalid' },
  createAccount: { kind: 'boolean' },
  accountUsername: { kind: 'string', pattern: USERNAME },
  accountPassword: { kind: 'string', pattern: PASSWORD },
  version: { kind: 'long' },
};

@Controller('api/students')
export class StudentsController {
  constructor(private readonly service: StudentsService) {}

  @Get()
  @Access(['TEACHER', 'ADMIN'])
  list(@Query() query: Record<string, unknown>) {
    const q = typeof query.q === 'string' ? query.q : null;
    return this.service.search(q, pageRequest(query, STUDENT_SORTING));
  }

  @Get('options')
  @Access(['TEACHER', 'ADMIN'])
  options() {
    return this.service.options();
  }

  @Get('me')
  @Access('authenticated')
  me(@Req() req: Request) {
    return this.service.findByAccountUsername(sessionUser(req).username);
  }

  @Get(':id')
  @Access(['TEACHER', 'ADMIN'])
  get(@Param('id') id: string) {
    return this.service.findById(pathId(id));
  }

  @Post()
  @Access(['ADMIN'])
  async create(@Body() body: unknown, @Res() res: Response) {
    const created = await this.service.create(parseBody<StudentInput>(body, STUDENT_SCHEMA));
    res.status(201).json(created);
  }

  @Put(':id')
  @Access(['ADMIN'])
  update(@Param('id') id: string, @Body() body: unknown) {
    return this.service.update(pathId(id), parseBody<StudentInput>(body, STUDENT_SCHEMA));
  }

  @Delete(':id')
  @Access(['ADMIN'])
  @HttpCode(204)
  async remove(@Param('id') id: string) {
    await this.service.delete(pathId(id));
  }
}
