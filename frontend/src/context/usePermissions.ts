import { useAuth } from './useAuth';

export interface Permissions {
  isAdmin: boolean;
  isTeacher: boolean;
  isStudent: boolean;
  /** TEACHER or ADMIN. */
  isStaff: boolean;
}

/** What the logged-in role is, in one place, so pages don't compare role strings themselves. */
export function usePermissions(): Permissions {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const isTeacher = user?.role === 'TEACHER';
  return { isAdmin, isTeacher, isStudent: user?.role === 'STUDENT', isStaff: isAdmin || isTeacher };
}
