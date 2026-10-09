import { Injectable } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import type { Role } from '../common/access';
import { validation } from '../common/errors';
import { USERNAME } from '../common/validate';
import { config } from '../config';
import type { Queryable } from '../db/db';
import { UsersRepo, UserRow } from './users.repo';

/**
 * Creates login accounts on behalf of an ADMIN: directly via POST /api/auth/register, or together with a
 * new Student/Teacher. A missing password falls back to DEFAULT_ACCOUNT_PASSWORD, a missing username is
 * derived from the email.
 */
@Injectable()
export class AccountProvisioner {
  constructor(private readonly users: UsersRepo) {}

  async create(
    username: string | null,
    email: string,
    password: string | null,
    role: Role,
    q?: Queryable,
  ): Promise<UserRow> {
    const name = isBlank(username) ? deriveUsername(email) : (username as string).trim();
    if (await this.users.existsByUsername(name, q)) throw validation(`Username already exists: ${name}`);
    if (await this.users.existsByEmail(email, q)) throw validation(`Email already exists: ${email}`);
    const hash = await bcrypt.hash(isBlank(password) ? config().defaultPassword : (password as string), 10);
    return this.users.insert({ username: name, email, passwordHash: hash, role }, q);
  }
}

function deriveUsername(email: string): string {
  const at = email.indexOf('@');
  const local = at >= 0 ? email.substring(0, at) : email;
  let name = local.replace(/[^a-zA-Z0-9_.-]/g, '_');
  if (name.length > 32) name = name.substring(0, 32);
  if (!USERNAME[0].test(name)) throw validation('Could not derive a username from the email; please enter one');
  return name;
}

const isBlank = (v: string | null | undefined) => v === null || v === undefined || v.trim() === '';
