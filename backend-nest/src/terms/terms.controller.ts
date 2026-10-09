import { Body, Controller, Get, Inject, Post, Res } from '@nestjs/common';
import type { Response } from 'express';
import { Access } from '../common/access';
import { validation } from '../common/errors';
import { parseBody } from '../common/validate';
import { currentActor, Db, DB, NOW, Row } from '../db/db';

const toDto = (r: Row) => ({ id: r.id, name: r.name, startDate: r.start_date, endDate: r.end_date });

@Controller('api/terms')
@Access('authenticated')
export class TermsController {
  constructor(@Inject(DB) private readonly db: Db) {}

  @Get()
  async list() {
    const { rows } = await this.db.query('select id, name, start_date, end_date from terms order by start_date desc, name desc');
    return rows.map(toDto);
  }

  @Post()
  @Access(['ADMIN'])
  async create(@Body() body: unknown, @Res() res: Response) {
    const r = parseBody<{ name: string; startDate: string | null; endDate: string | null }>(body, {
      name: {
        kind: 'string',
        required: 'Term name is required',
        max: [100, 'Term name must be at most 100 characters'],
      },
      startDate: { kind: 'date' },
      endDate: { kind: 'date' },
    });
    const name = r.name.trim();
    if ((await this.db.query('select 1 from terms where name = $1', [name])).rows.length > 0) {
      throw validation(`Term already exists: ${name}`);
    }
    if (r.startDate && r.endDate && r.endDate < r.startDate) {
      throw validation('Term end date must not be before its start date');
    }
    const { rows } = await this.db.query(
      `insert into terms (created_at, updated_at, created_by, updated_by, version, name, start_date, end_date)
       values (${NOW}, ${NOW}, $1, $1, 0, $2, $3, $4) returning id, name, start_date, end_date`,
      [currentActor(), name, r.startDate, r.endDate],
    );
    res.status(201).json(toDto(rows[0]));
  }
}
