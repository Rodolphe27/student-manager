import { Body, Controller, Get, HttpCode, Inject, Put, Req } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import type { Request } from 'express';
import { startSession } from '../auth/auth.controller';
import { UsersRepo, UserRow } from '../auth/users.repo';
import { Access, Role, sessionUser } from '../common/access';
import { invalidCredentials, validation } from '../common/errors';
import { DEPARTMENT, parseBody, PASSWORD, USERNAME } from '../common/validate';
import { currentActor, Db, DB, NOW, Row } from '../db/db';

interface ProfileDto {
  username: string;
  email: string;
  role: Role;
  firstName: string | null;
  lastName: string | null;
  matriculationNumber: string | null;
  birthDate: string | null;
  department: string | null;
}

interface UpdateProfile {
  username: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  birthDate: string | null;
  department: string | null;
}

const blank = (v: string | null) => v === null || v.trim() === '';

@Controller('api/profile')
@Access('authenticated')
export class ProfileController {
  constructor(
    @Inject(DB) private readonly db: Db,
    private readonly users: UsersRepo,
  ) {}

  @Get()
  async get(@Req() req: Request): Promise<ProfileDto> {
    const user = await this.load(sessionUser(req).username);
    return this.toDto(user, await this.student(user), await this.teacher(user));
  }

  @Put()
  async update(@Body() body: unknown, @Req() req: Request): Promise<ProfileDto> {
    const r = parseBody<UpdateProfile>(body, {
      username: { kind: 'string', required: 'Username is required', pattern: USERNAME },
      email: { kind: 'string', required: 'Email is required', email: 'Email is invalid' },
      firstName: { kind: 'string' },
      lastName: { kind: 'string' },
      birthDate: { kind: 'date' },
      department: { kind: 'string', pattern: DEPARTMENT },
    });
    const sessionName = sessionUser(req).username;
    const user = await this.load(sessionName);
    const student = await this.student(user);
    const teacher = await this.teacher(user);

    const usernameChanged = user.username !== r.username;
    const emailChanged = user.email.toLowerCase() !== r.email.toLowerCase();
    if (usernameChanged && (await this.users.existsByUsername(r.username))) {
      throw validation(`Username already exists: ${r.username}`);
    }
    if (emailChanged && (await this.users.existsByEmail(r.email))) {
      throw validation(`Email already exists: ${r.email}`);
    }
    if ((student || teacher) && (blank(r.firstName) || blank(r.lastName))) {
      throw validation('First and last name are required');
    }

    await this.db.tx(async (q) => {
      if (student) {
        if (emailChanged) await this.requireFreeEmail('students', r.email, student.id);
        await q.query(
          `update students set first_name = $2, last_name = $3, birth_date = $4, email = $5, user_id = $6,
                  updated_at = ${NOW}, updated_by = $7, version = version + 1 where id = $1`,
          [student.id, r.firstName!.trim(), r.lastName!.trim(), r.birthDate, r.email, user.id, currentActor()],
        );
      }
      if (teacher) {
        if (emailChanged) await this.requireFreeEmail('teachers', r.email, teacher.id);
        await q.query(
          `update teachers set first_name = $2, last_name = $3, department = $4, email = $5, user_id = $6,
                  updated_at = ${NOW}, updated_by = $7, version = version + 1 where id = $1`,
          [teacher.id, r.firstName!.trim(), r.lastName!.trim(), r.department, r.email, user.id, currentActor()],
        );
      }
      const personal = !student && !teacher;
      await this.users.update(
        user.id,
        {
          username: r.username,
          email: r.email,
          passwordHash: user.passwordHash,
          firstName: personal ? (blank(r.firstName) ? null : r.firstName!.trim()) : user.firstName,
          lastName: personal ? (blank(r.lastName) ? null : r.lastName!.trim()) : user.lastName,
        },
        q,
      );
    });

    const updated = await this.load(r.username);
    // A changed username must not leave the old session pointing at a name that no longer exists.
    if (updated.username !== sessionName) {
      await startSession(req, { username: updated.username, email: updated.email, role: updated.role });
    }
    return this.toDto(updated, await this.student(updated), await this.teacher(updated));
  }

  @Put('password')
  @HttpCode(204)
  async changePassword(@Body() body: unknown, @Req() req: Request): Promise<void> {
    const r = parseBody<{ currentPassword: string; newPassword: string }>(body, {
      currentPassword: { kind: 'string', required: 'Current password is required' },
      newPassword: { kind: 'string', required: 'New password is required', pattern: PASSWORD },
    });
    const user = await this.load(sessionUser(req).username);
    if (!(await bcrypt.compare(r.currentPassword, user.passwordHash))) {
      throw validation('Current password is incorrect');
    }
    await this.users.update(user.id, {
      username: user.username,
      email: user.email,
      passwordHash: await bcrypt.hash(r.newPassword, 10),
      firstName: user.firstName,
      lastName: user.lastName,
    });
  }

  private async load(username: string): Promise<UserRow> {
    const user = await this.users.findByUsername(username);
    if (!user || !user.active) throw invalidCredentials();
    return user;
  }

  private async student(user: UserRow): Promise<Row | null> {
    if (user.role !== 'STUDENT') return null;
    return this.profileRow('students', user, 'matriculation_number, birth_date');
  }

  private async teacher(user: UserRow): Promise<Row | null> {
    if (user.role !== 'TEACHER') return null;
    return this.profileRow('teachers', user, 'department');
  }

  /** The Student/Teacher row of a login: linked by user_id, else by e-mail. `table`/`extra` are constants. */
  private async profileRow(table: 'students' | 'teachers', user: UserRow, extra: string): Promise<Row | null> {
    const cols = `t.id, t.first_name, t.last_name, ${extra.split(', ').map((c) => 't.' + c).join(', ')}`;
    const linked = await this.db.query(
      `select ${cols} from ${table} t join users u on u.id = t.user_id where u.username = $1`,
      [user.username],
    );
    if (linked.rows[0]) return linked.rows[0];
    const byEmail = await this.db.query(`select ${cols} from ${table} t where t.email = $1`, [user.email]);
    return byEmail.rows[0] ?? null;
  }

  private async requireFreeEmail(table: 'students' | 'teachers', email: string, exceptId: number) {
    const { rows } = await this.db.query(`select 1 from ${table} where email = $1 and id <> $2`, [email, exceptId]);
    if (rows.length > 0) throw validation(`Email already exists: ${email}`);
  }

  private toDto(user: UserRow, student: Row | null, teacher: Row | null): ProfileDto {
    const dto: ProfileDto = {
      username: user.username,
      email: user.email,
      role: user.role,
      firstName: null,
      lastName: null,
      matriculationNumber: null,
      birthDate: null,
      department: null,
    };
    if (!student && !teacher) {
      dto.firstName = user.firstName;
      dto.lastName = user.lastName;
    }
    if (student) {
      dto.firstName = student.first_name;
      dto.lastName = student.last_name;
      dto.matriculationNumber = student.matriculation_number;
      dto.birthDate = student.birth_date;
    }
    if (teacher) {
      dto.firstName = teacher.first_name;
      dto.lastName = teacher.last_name;
      dto.department = teacher.department;
    }
    return dto;
  }
}
