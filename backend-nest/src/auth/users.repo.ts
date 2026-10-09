import { Inject, Injectable } from '@nestjs/common';
import { currentActor, Db, DB, NOW, Queryable, Row } from '../db/db';
import type { Role } from '../common/access';

export interface UserRow {
  id: number;
  username: string;
  email: string;
  passwordHash: string;
  role: Role;
  active: boolean;
  firstName: string | null;
  lastName: string | null;
}

const COLUMNS = 'id, username, email, password_hash, role, active, first_name, last_name';

const toUser = (r: Row): UserRow => ({
  id: r.id,
  username: r.username,
  email: r.email,
  passwordHash: r.password_hash,
  role: r.role,
  active: r.active,
  firstName: r.first_name,
  lastName: r.last_name,
});

@Injectable()
export class UsersRepo {
  constructor(@Inject(DB) private readonly db: Db) {}

  async findByUsername(username: string, q: Queryable = this.db): Promise<UserRow | null> {
    const { rows } = await q.query(`select ${COLUMNS} from users where username = $1`, [username]);
    return rows[0] ? toUser(rows[0]) : null;
  }

  async findById(id: number, q: Queryable = this.db): Promise<UserRow | null> {
    const { rows } = await q.query(`select ${COLUMNS} from users where id = $1`, [id]);
    return rows[0] ? toUser(rows[0]) : null;
  }

  async existsByUsername(username: string, q: Queryable = this.db): Promise<boolean> {
    const { rows } = await q.query('select 1 from users where username = $1', [username]);
    return rows.length > 0;
  }

  async existsByEmail(email: string, q: Queryable = this.db): Promise<boolean> {
    const { rows } = await q.query('select 1 from users where email = $1', [email]);
    return rows.length > 0;
  }

  async insert(
    u: { username: string; email: string; passwordHash: string; role: Role },
    q: Queryable = this.db,
  ): Promise<UserRow> {
    const { rows } = await q.query(
      `insert into users (created_at, updated_at, created_by, updated_by, version, active, username, email, password_hash, role)
       values (${NOW}, ${NOW}, $1, $1, 0, true, $2, $3, $4, $5) returning ${COLUMNS}`,
      [currentActor(), u.username, u.email, u.passwordHash, u.role],
    );
    return toUser(rows[0]);
  }

  async update(
    id: number,
    u: { username: string; email: string; passwordHash: string; firstName: string | null; lastName: string | null },
    q: Queryable = this.db,
  ): Promise<void> {
    await q.query(
      `update users set username = $2, email = $3, password_hash = $4, first_name = $5, last_name = $6,
              updated_at = ${NOW}, updated_by = $7, version = version + 1 where id = $1`,
      [id, u.username, u.email, u.passwordHash, u.firstName, u.lastName, currentActor()],
    );
  }

  async setEmail(id: number, email: string, q: Queryable = this.db): Promise<void> {
    await q.query(
      `update users set email = $2, updated_at = ${NOW}, updated_by = $3, version = version + 1 where id = $1`,
      [id, email, currentActor()],
    );
  }

  async delete(id: number, q: Queryable = this.db): Promise<void> {
    await q.query('delete from users where id = $1', [id]);
  }
}
