import { Body, Controller, Get, HttpCode, Post, Req, Res } from '@nestjs/common';
import * as bcrypt from 'bcryptjs';
import type { Request, Response } from 'express';
import { Access, ROLES, Role, sessionUser } from '../common/access';
import { invalidCredentials, validation } from '../common/errors';
import { parseBody, PASSWORD, USERNAME } from '../common/validate';
import { AccountProvisioner } from './account-provisioner';
import { UsersRepo, UserRow } from './users.repo';

export interface AuthResponse {
  username: string;
  email: string;
  role: Role;
}

const toResponse = (u: UserRow): AuthResponse => ({ username: u.username, email: u.email, role: u.role });

/** Starts a fresh session for the user (never keeps a pre-login session id: session fixation). */
export function startSession(req: Request, account: AuthResponse): Promise<void> {
  return new Promise((resolve, reject) => {
    req.session.regenerate((err) => {
      if (err) return reject(err);
      req.session.user = { username: account.username, role: account.role };
      req.session.save((saveErr) => (saveErr ? reject(saveErr) : resolve()));
    });
  });
}

@Controller('api/auth')
export class AuthController {
  constructor(
    private readonly users: UsersRepo,
    private readonly provisioner: AccountProvisioner,
  ) {}

  @Post('register')
  @Access(['ADMIN'])
  async register(@Body() body: unknown, @Res() res: Response) {
    const r = parseBody<{ username: string; email: string; password: string; role: Role }>(body, {
      username: { kind: 'string', required: 'Username is required', pattern: USERNAME },
      email: { kind: 'string', required: 'Email is required', email: 'Email is invalid' },
      password: { kind: 'string', required: 'Password is required', pattern: PASSWORD },
      role: { kind: 'enum', values: ROLES, required: 'Role is required' },
    });
    const saved = await this.provisioner.create(r.username, r.email, r.password, r.role);
    res.status(201).json(toResponse(saved));
  }

  @Post('login')
  @Access('public')
  @HttpCode(200)
  async login(@Body() body: unknown, @Req() req: Request): Promise<AuthResponse> {
    const r = parseBody<{ username: string; password: string }>(body, {
      username: { kind: 'string', required: 'Username is required' },
      password: { kind: 'string', required: 'Password is required' },
    });
    // Every failure throws the same error, so a caller cannot tell an unknown username from a wrong password.
    const user = await this.users.findByUsername(r.username);
    if (!user || !user.active) throw invalidCredentials();
    if (!(await bcrypt.compare(r.password, user.passwordHash))) throw invalidCredentials();
    const account = toResponse(user);
    await startSession(req, account);
    return account;
  }

  @Get('me')
  async me(@Req() req: Request): Promise<AuthResponse> {
    const user = await this.users.findByUsername(sessionUser(req).username);
    if (!user || !user.active) throw invalidCredentials();
    return toResponse(user);
  }

  @Post('logout')
  @Access('public')
  @HttpCode(204)
  logout(@Req() req: Request, @Res({ passthrough: true }) res: Response): Promise<void> {
    return new Promise((resolve) => {
      req.session.destroy(() => {
        res.clearCookie('SESSION');
        resolve();
      });
    });
  }
}

export { validation };
