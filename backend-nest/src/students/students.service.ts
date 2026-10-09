import { Inject, Injectable } from '@nestjs/common';
import { AccountProvisioner } from '../auth/account-provisioner';
import { UsersRepo } from '../auth/users.repo';
import { notFound, notFoundMessage, validation, versionConflict } from '../common/errors';
import { PageOf, PageRequest, Sorting, toPage } from '../common/paging';
import { currentActor, Db, DB, NOW, Queryable, Row } from '../db/db';

export interface StudentDto {
  id: number;
  firstName: string;
  lastName: string;
  matriculationNumber: string;
  birthDate: string | null;
  email: string;
  fullName: string;
  version: number;
}

export interface StudentInput {
  firstName: string;
  lastName: string;
  matriculationNumber: string;
  birthDate: string | null;
  email: string;
  createAccount: boolean;
  accountUsername: string | null;
  accountPassword: string | null;
  version: number | null;
}

export const STUDENT_SORTING: Sorting = {
  columns: {
    id: 's.id',
    firstName: 's.first_name',
    lastName: 's.last_name',
    matriculationNumber: 's.matriculation_number',
    birthDate: 's.birth_date',
    email: 's.email',
    version: 's.version',
    createdAt: 's.created_at',
    updatedAt: 's.updated_at',
    createdBy: 's.created_by',
    updatedBy: 's.updated_by',
  },
  defaults: [
    ['s.last_name', 'asc'],
    ['s.first_name', 'asc'],
  ],
};

const COLUMNS = 's.id, s.first_name, s.last_name, s.matriculation_number, s.birth_date, s.email, s.version, s.user_id';

const toDto = (r: Row): StudentDto => ({
  id: r.id,
  firstName: r.first_name,
  lastName: r.last_name,
  matriculationNumber: r.matriculation_number,
  birthDate: r.birth_date,
  email: r.email,
  fullName: `${r.first_name} ${r.last_name}`,
  version: r.version,
});

@Injectable()
export class StudentsService {
  constructor(
    @Inject(DB) private readonly db: Db,
    private readonly users: UsersRepo,
    private readonly provisioner: AccountProvisioner,
  ) {}

  async search(query: string | null, page: PageRequest): Promise<PageOf<StudentDto>> {
    const params: unknown[] = [];
    let where = '';
    if (query && query.trim() !== '') {
      params.push(`%${query.trim().toLowerCase()}%`);
      where = `where lower(s.first_name) like $1 or lower(s.last_name) like $1
               or lower(s.matriculation_number) like $1 or lower(s.email) like $1`;
    }
    const total = (await this.db.query(`select count(*) as n from students s ${where}`, params)).rows[0].n as number;
    const { rows } = await this.db.query(
      `select ${COLUMNS} from students s ${where} order by ${page.orderBy}, s.id limit ${page.size} offset ${page.page * page.size}`,
      params,
    );
    return toPage(rows.map(toDto), total, page);
  }

  async options() {
    const { rows } = await this.db.query(
      `select id, first_name, last_name, matriculation_number from students order by last_name, first_name`,
    );
    return rows.map((r) => ({
      id: r.id,
      fullName: `${r.first_name} ${r.last_name}`,
      matriculationNumber: r.matriculation_number,
    }));
  }

  async findById(id: number): Promise<StudentDto> {
    return toDto(await this.load(id));
  }

  async findByAccountUsername(username: string): Promise<StudentDto> {
    const row = await this.resolveByAccount(username);
    if (!row) throw notFoundMessage(`No student record is linked to account: ${username}`);
    return toDto(row);
  }

  async accountOwnsStudent(username: string | null, studentId: number | null): Promise<boolean> {
    if (username === null || studentId === null) return false;
    const row = await this.resolveByAccount(username);
    return row !== null && row.id === studentId;
  }

  /** The student linked to a login: by user_id, else by matching e-mail address. */
  async resolveByAccount(username: string, q: Queryable = this.db): Promise<Row | null> {
    const linked = await q.query(
      `select ${COLUMNS} from students s join users u on u.id = s.user_id where u.username = $1`,
      [username],
    );
    if (linked.rows[0]) return linked.rows[0];
    const account = await this.users.findByUsername(username, q);
    if (!account) return null;
    const byEmail = await q.query(`select ${COLUMNS} from students s where s.email = $1`, [account.email]);
    return byEmail.rows[0] ?? null;
  }

  async create(input: StudentInput): Promise<StudentDto> {
    if (await this.exists('email = $1', [input.email])) throw validation(`Email already exists: ${input.email}`);
    if (await this.exists('matriculation_number = $1', [input.matriculationNumber])) {
      throw validation(`Matriculation number already exists: ${input.matriculationNumber}`);
    }
    return this.db.tx(async (q) => {
      const { rows } = await q.query(
        `insert into students (created_at, updated_at, created_by, updated_by, version,
                               first_name, last_name, matriculation_number, birth_date, email)
         values (${NOW}, ${NOW}, $1, $1, 0, $2, $3, $4, $5, $6)
         returning id, first_name, last_name, matriculation_number, birth_date, email, version`,
        [currentActor(), input.firstName, input.lastName, input.matriculationNumber, input.birthDate, input.email],
      );
      if (input.createAccount) {
        const account = await this.provisioner.create(
          input.accountUsername,
          input.email,
          input.accountPassword,
          'STUDENT',
          q,
        );
        await q.query('update students set user_id = $2 where id = $1', [rows[0].id, account.id]);
      }
      return toDto(rows[0]);
    });
  }

  async update(id: number, input: StudentInput): Promise<StudentDto> {
    const student = await this.load(id);
    if (input.version !== null && input.version !== student.version) throw versionConflict();
    if (await this.exists('email = $1 and id <> $2', [input.email, id])) {
      throw validation(`Email already exists: ${input.email}`);
    }
    if (await this.exists('matriculation_number = $1 and id <> $2', [input.matriculationNumber, id])) {
      throw validation(`Matriculation number already exists: ${input.matriculationNumber}`);
    }
    return this.db.tx(async (q) => {
      if (student.user_id) {
        const account = await this.users.findById(student.user_id, q);
        if (account && account.email.toLowerCase() !== input.email.toLowerCase()) {
          if (await this.users.existsByEmail(input.email, q)) throw validation(`Email already exists: ${input.email}`);
          await this.users.setEmail(account.id, input.email, q);
        }
      }
      const { rows } = await q.query(
        `update students set first_name = $3, last_name = $4, matriculation_number = $5, birth_date = $6, email = $7,
                updated_at = ${NOW}, updated_by = $8, version = version + 1
         where id = $1 and version = $2
         returning id, first_name, last_name, matriculation_number, birth_date, email, version`,
        [id, student.version, input.firstName, input.lastName, input.matriculationNumber, input.birthDate, input.email, currentActor()],
      );
      if (!rows[0]) throw versionConflict();
      return toDto(rows[0]);
    });
  }

  async delete(id: number): Promise<void> {
    const student = await this.load(id);
    await this.db.tx(async (q) => {
      await q.query('delete from students where id = $1', [id]);
      if (student.user_id) await this.users.delete(student.user_id, q);
    });
  }

  private async load(id: number): Promise<Row> {
    const { rows } = await this.db.query(`select ${COLUMNS} from students s where s.id = $1`, [id]);
    if (!rows[0]) throw notFound('Student', id);
    return rows[0];
  }

  private async exists(condition: string, params: unknown[]): Promise<boolean> {
    const { rows } = await this.db.query(`select 1 from students where ${condition}`, params);
    return rows.length > 0;
  }
}
