import { CanActivate, ExecutionContext, Injectable, SetMetadata } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import type { Request } from 'express';
import { forbidden, unauthenticated } from './errors';

export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';
export const ROLES: readonly Role[] = ['STUDENT', 'TEACHER', 'ADMIN'];

export interface SessionUser {
  username: string;
  role: Role;
}

declare module 'express-session' {
  interface SessionData {
    user?: SessionUser;
  }
}

const ACCESS = 'access';
/** Who may call a handler: 'public', 'authenticated', or a list of roles (SecurityConfig's rules). */
export type AccessRule = 'public' | 'authenticated' | Role[];
export const Access = (rule: AccessRule) => SetMetadata(ACCESS, rule);

@Injectable()
export class AccessGuard implements CanActivate {
  constructor(private readonly reflector: Reflector) {}

  canActivate(context: ExecutionContext): boolean {
    const rule = this.reflector.getAllAndOverride<AccessRule | undefined>(ACCESS, [
      context.getHandler(),
      context.getClass(),
    ]) ?? 'authenticated';
    if (rule === 'public') return true;
    const user = context.switchToHttp().getRequest<Request>().session?.user;
    if (!user) throw unauthenticated();
    if (rule !== 'authenticated' && !rule.includes(user.role)) throw forbidden();
    return true;
  }
}

/** The logged-in user of this request (the guard has already made sure there is one). */
export const sessionUser = (req: Request): SessionUser => req.session.user as SessionUser;

export const isAdmin = (user: SessionUser) => user.role === 'ADMIN';
