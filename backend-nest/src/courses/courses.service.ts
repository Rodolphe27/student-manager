import { Inject, Injectable } from '@nestjs/common';
import { notFound, validation, versionConflict } from '../common/errors';
import { PageOf, PageRequest, Sorting, toPage } from '../common/paging';
import { currentActor, Db, DB, NOW, Row } from '../db/db';

export const COURSE_STATUSES = ['ACTIVE', 'INACTIVE', 'ARCHIVED'] as const;
export type CourseStatus = (typeof COURSE_STATUSES)[number];

export interface CourseDto {
  id: number;
  code: string;
  title: string;
  description: string | null;
  creditHours: number;
  status: CourseStatus;
  active: boolean;
  teacherId: number | null;
  teacherName: string | null;
  termId: number | null;
  termName: string | null;
  version: number;
}

export interface CourseInput {
  code: string;
  title: string;
  description: string | null;
  creditHours: number;
  status: CourseStatus | null;
  teacherId: number | null;
  termId: number | null;
  version: number | null;
}

export const COURSE_SORTING: Sorting = {
  columns: {
    id: 'c.id',
    code: 'c.code',
    title: 'c.title',
    description: 'c.description',
    creditHours: 'c.credit_hours',
    status: 'c.status',
    version: 'c.version',
    createdAt: 'c.created_at',
    updatedAt: 'c.updated_at',
    createdBy: 'c.created_by',
    updatedBy: 'c.updated_by',
    'teacher.id': 'c.teacher_id',
    'teacher.firstName': 't.first_name',
    'teacher.lastName': 't.last_name',
    'teacher.email': 't.email',
    'teacher.department': 't.department',
    'term.id': 'c.term_id',
    'term.name': 'tm.name',
    'term.startDate': 'tm.start_date',
    'term.endDate': 'tm.end_date',
  },
  defaults: [['c.code', 'asc']],
};

const SELECT = `select c.id, c.code, c.title, c.description, c.credit_hours, c.status, c.version,
       c.teacher_id, t.first_name as t_first, t.last_name as t_last, c.term_id, tm.name as term_name
  from courses c left join teachers t on t.id = c.teacher_id left join terms tm on tm.id = c.term_id`;

const toDto = (r: Row): CourseDto => ({
  id: r.id,
  code: r.code,
  title: r.title,
  description: r.description,
  creditHours: r.credit_hours,
  status: r.status,
  active: r.status === 'ACTIVE',
  teacherId: r.teacher_id,
  teacherName: r.teacher_id === null ? null : `${r.t_first} ${r.t_last}`,
  termId: r.term_id,
  termName: r.term_id === null ? null : r.term_name,
  version: r.version,
});

@Injectable()
export class CoursesService {
  constructor(@Inject(DB) private readonly db: Db) {}

  async search(query: string | null, status: CourseStatus | null, page: PageRequest): Promise<PageOf<CourseDto>> {
    const params: unknown[] = [];
    const conditions: string[] = [];
    if (query && query.trim() !== '') {
      params.push(`%${query.trim().toLowerCase()}%`);
      conditions.push(`(lower(c.code) like $${params.length} or lower(c.title) like $${params.length})`);
    }
    if (status) {
      params.push(status);
      conditions.push(`c.status = $${params.length}`);
    }
    const where = conditions.length ? `where ${conditions.join(' and ')}` : '';
    const total = (await this.db.query(`select count(*) as n from courses c ${where}`, params)).rows[0].n as number;
    const { rows } = await this.db.query(
      `${SELECT} ${where} order by ${page.orderBy}, c.id limit ${page.size} offset ${page.page * page.size}`,
      params,
    );
    return toPage(rows.map(toDto), total, page);
  }

  async options(teacherId: number | null) {
    const { rows } = await this.db.query(
      `select id, code, title, status from courses ${teacherId === null ? '' : 'where teacher_id = $1'} order by code`,
      teacherId === null ? [] : [teacherId],
    );
    return rows.map((r) => ({ id: r.id, code: r.code, title: r.title, status: r.status }));
  }

  async findById(id: number): Promise<CourseDto> {
    const { rows } = await this.db.query(`${SELECT} where c.id = $1`, [id]);
    if (!rows[0]) throw notFound('Course', id);
    return toDto(rows[0]);
  }

  async findByStatus(status: CourseStatus): Promise<CourseDto[]> {
    const { rows } = await this.db.query(`${SELECT} where c.status = $1 order by c.id`, [status]);
    return rows.map(toDto);
  }

  async create(input: CourseInput): Promise<CourseDto> {
    if (await this.codeTaken(input.code, null)) throw validation(`Course code already exists: ${input.code}`);
    await this.requireRefs(input);
    const { rows } = await this.db.query(
      `insert into courses (created_at, updated_at, created_by, updated_by, version,
                            code, title, description, credit_hours, status, teacher_id, term_id)
       values (${NOW}, ${NOW}, $1, $1, 0, $2, $3, $4, $5, $6, $7, $8) returning id`,
      [currentActor(), input.code, input.title, input.description, input.creditHours, input.status ?? 'ACTIVE',
        input.teacherId, input.termId],
    );
    return this.findById(rows[0].id);
  }

  async update(id: number, input: CourseInput): Promise<CourseDto> {
    const current = await this.findById(id);
    if (input.version !== null && input.version !== current.version) throw versionConflict();
    if (await this.codeTaken(input.code, id)) throw validation(`Course code already exists: ${input.code}`);
    await this.requireRefs(input);
    const { rowCount } = await this.db.query(
      `update courses set code = $3, title = $4, description = $5, credit_hours = $6, status = $7,
              teacher_id = $8, term_id = $9, updated_at = ${NOW}, updated_by = $10, version = version + 1
       where id = $1 and version = $2`,
      [id, current.version, input.code, input.title, input.description, input.creditHours,
        input.status ?? current.status, input.teacherId, input.termId, currentActor()],
    );
    if (rowCount === 0) throw versionConflict();
    return this.findById(id);
  }

  async delete(id: number): Promise<void> {
    const { rowCount } = await this.db.query('delete from courses where id = $1', [id]);
    if (rowCount === 0) throw notFound('Course', id);
  }

  async exists(id: number): Promise<boolean> {
    return (await this.db.query('select 1 from courses where id = $1', [id])).rows.length > 0;
  }

  async taughtBy(courseId: number, teacherId: number): Promise<boolean> {
    return (await this.db.query('select 1 from courses where id = $1 and teacher_id = $2', [courseId, teacherId]))
      .rows.length > 0;
  }

  private async codeTaken(code: string, exceptId: number | null): Promise<boolean> {
    const { rows } = await this.db.query('select 1 from courses where code = $1 and id <> $2', [code, exceptId ?? -1]);
    return rows.length > 0;
  }

  private async requireRefs(input: CourseInput): Promise<void> {
    if (input.teacherId !== null) {
      const t = await this.db.query('select 1 from teachers where id = $1', [input.teacherId]);
      if (!t.rows[0]) throw notFound('Teacher', input.teacherId);
    }
    if (input.termId !== null) {
      const t = await this.db.query('select 1 from terms where id = $1', [input.termId]);
      if (!t.rows[0]) throw notFound('Term', input.termId);
    }
  }
}
