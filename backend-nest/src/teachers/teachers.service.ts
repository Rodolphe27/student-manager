import { Inject, Injectable } from '@nestjs/common';
import { AccountProvisioner } from '../auth/account-provisioner';
import { UsersRepo } from '../auth/users.repo';
import { notFound, notFoundMessage, validation, versionConflict } from '../common/errors';
import { PageOf, PageRequest, Sorting, toPage } from '../common/paging';
import { currentActor, Db, DB, NOW, Queryable, Row } from '../db/db';

export interface TeacherDto {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string | null;
  fullName: string;
  version: number;
}

export interface TeacherInput {
  firstName: string;
  lastName: string;
  email: string;
  department: string | null;
  createAccount: boolean;
  accountUsername: string | null;
  accountPassword: string | null;
  version: number | null;
}

export const TEACHER_SORTING: Sorting = {
  columns: {
    id: 't.id',
    firstName: 't.first_name',
    lastName: 't.last_name',
    email: 't.email',
    department: 't.department',
    version: 't.version',
    createdAt: 't.created_at',
    updatedAt: 't.updated_at',
    createdBy: 't.created_by',
    updatedBy: 't.updated_by',
  },
  defaults: [
    ['t.last_name', 'asc'],
    ['t.first_name', 'asc'],
  ],
};

const COLUMNS = 't.id, t.first_name, t.last_name, t.email, t.department, t.version, t.user_id';

const toDto = (r: Row): TeacherDto => ({
  id: r.id,
  firstName: r.first_name,
  lastName: r.last_name,
  email: r.email,
  department: r.department,
  fullName: `${r.first_name} ${r.last_name}`,
  version: r.version,
});

@Injectable()
export class TeachersService {
  constructor(
    @Inject(DB) private readonly db: Db,
    private readonly users: UsersRepo,
    private readonly provisioner: AccountProvisioner,
  ) {}

  async search(query: string | null, page: PageRequest): Promise<PageOf<TeacherDto>> {
    const params: unknown[] = [];
    let where = '';
    if (query && query.trim() !== '') {
      params.push(`%${query.trim().toLowerCase()}%`);
      where = `where lower(t.first_name) like $1 or lower(t.last_name) like $1
               or lower(t.email) like $1 or lower(t.department) like $1`;
    }
    const total = (await this.db.query(`select count(*) as n from teachers t ${where}`, params)).rows[0].n as number;
    const { rows } = await this.db.query(
      `select ${COLUMNS} from teachers t ${where} order by ${page.orderBy}, t.id limit ${page.size} offset ${page.page * page.size}`,
      params,
    );
    return toPage(rows.map(toDto), total, page);
  }

  async options() {
    const { rows } = await this.db.query(
      'select id, first_name, last_name, department from teachers order by last_name, first_name',
    );
    return rows.map((r) => ({ id: r.id, fullName: `${r.first_name} ${r.last_name}`, department: r.department }));
  }

  async findById(id: number): Promise<TeacherDto> {
    return toDto(await this.load(id));
  }

  /** The teacher linked to a login: by user_id, else by matching e-mail address. */
  async findIdByAccountUsername(username: string | null, q: Queryable = this.db): Promise<number | null> {
    if (username === null) return null;
    const linked = await q.query(
      'select t.id from teachers t join users u on u.id = t.user_id where u.username = $1',
      [username],
    );
    if (linked.rows[0]) return linked.rows[0].id;
    const account = await this.users.findByUsername(username, q);
    if (!account) return null;
    const byEmail = await q.query('select id from teachers where email = $1', [account.email]);
    return byEmail.rows[0]?.id ?? null;
  }

  async findMe(username: string): Promise<TeacherDto> {
    const id = await this.findIdByAccountUsername(username);
    if (id === null) throw notFoundMessage(`No teacher record is linked to account: ${username}`);
    return this.findById(id);
  }

  async create(input: TeacherInput): Promise<TeacherDto> {
    if (await this.exists('email = $1', [input.email])) throw validation(`Email already exists: ${input.email}`);
    return this.db.tx(async (q) => {
      const { rows } = await q.query(
        `insert into teachers (created_at, updated_at, created_by, updated_by, version,
                               first_name, last_name, email, department)
         values (${NOW}, ${NOW}, $1, $1, 0, $2, $3, $4, $5)
         returning id, first_name, last_name, email, department, version`,
        [currentActor(), input.firstName, input.lastName, input.email, input.department],
      );
      if (input.createAccount) {
        const account = await this.provisioner.create(
          input.accountUsername,
          input.email,
          input.accountPassword,
          'TEACHER',
          q,
        );
        await q.query('update teachers set user_id = $2 where id = $1', [rows[0].id, account.id]);
      }
      return toDto(rows[0]);
    });
  }

  async update(id: number, input: TeacherInput): Promise<TeacherDto> {
    const teacher = await this.load(id);
    if (input.version !== null && input.version !== teacher.version) throw versionConflict();
    if (await this.exists('email = $1 and id <> $2', [input.email, id])) {
      throw validation(`Email already exists: ${input.email}`);
    }
    return this.db.tx(async (q) => {
      if (teacher.user_id) {
        const account = await this.users.findById(teacher.user_id, q);
        if (account && account.email.toLowerCase() !== input.email.toLowerCase()) {
          if (await this.users.existsByEmail(input.email, q)) throw validation(`Email already exists: ${input.email}`);
          await this.users.setEmail(account.id, input.email, q);
        }
      }
      const { rows } = await q.query(
        `update teachers set first_name = $3, last_name = $4, email = $5, department = $6,
                updated_at = ${NOW}, updated_by = $7, version = version + 1
         where id = $1 and version = $2
         returning id, first_name, last_name, email, department, version`,
        [id, teacher.version, input.firstName, input.lastName, input.email, input.department, currentActor()],
      );
      if (!rows[0]) throw versionConflict();
      return toDto(rows[0]);
    });
  }

  async delete(id: number): Promise<void> {
    const teacher = await this.load(id);
    await this.db.tx(async (q) => {
      await q.query('delete from teachers where id = $1', [id]);
      if (teacher.user_id) await this.users.delete(teacher.user_id, q);
    });
  }

  private async load(id: number): Promise<Row> {
    const { rows } = await this.db.query(`select ${COLUMNS} from teachers t where t.id = $1`, [id]);
    if (!rows[0]) throw notFound('Teacher', id);
    return rows[0];
  }

  private async exists(condition: string, params: unknown[]): Promise<boolean> {
    const { rows } = await this.db.query(`select 1 from teachers where ${condition}`, params);
    return rows.length > 0;
  }
}
