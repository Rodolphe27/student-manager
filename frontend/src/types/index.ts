export type CourseStatus = 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
export type EnrollmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED';
export type Grade = 'A' | 'B' | 'C' | 'D' | 'F' | 'NOT_GRADED';
export type Role = 'STUDENT' | 'TEACHER' | 'ADMIN';

export interface Student {
  id: number;
  firstName: string;
  lastName: string;
  matriculationNumber: string;
  birthDate: string | null;
  email: string;
  fullName: string;
  version: number;
}

export interface CreateStudentRequest {
  firstName: string;
  lastName: string;
  matriculationNumber: string;
  birthDate?: string;
  email: string;
  // Create only: also create a login account for the new profile (see AccountFields).
  createAccount?: boolean;
  accountUsername?: string;
  accountPassword?: string;
  // Version of the record being edited; a stale one makes the update fail with 409.
  version?: number;
}

export interface Course {
  id: number;
  code: string;
  title: string;
  description: string | null;
  creditHours: number;
  status: CourseStatus;
  active: boolean;
  version: number;
}

export interface CreateCourseRequest {
  code: string;
  title: string;
  description?: string;
  creditHours: number;
  status?: CourseStatus;
  version?: number;
}

export interface Enrollment {
  id: number;
  studentId: number;
  studentName: string;
  courseId: number;
  courseTitle: string;
  courseCode: string;
  enrolledAt: string;
  status: EnrollmentStatus;
  grade: Grade;
  confirmed: boolean;
}

export interface CreateEnrollmentRequest {
  studentId: number;
  courseId: number;
}

export interface UpdateGradeRequest {
  grade: Grade;
}

export interface Teacher {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string | null;
  fullName: string;
  version: number;
}

export interface CreateTeacherRequest {
  firstName: string;
  lastName: string;
  email: string;
  department?: string;
  // Create only: also create a login account for the new profile (see AccountFields).
  createAccount?: boolean;
  accountUsername?: string;
  accountPassword?: string;
  version?: number;
}

export interface AuthResponse {
  username: string;
  email: string;
  role: Role;
}

export interface LoginRequest {
  username: string;
  password: string;
}

// ── Paging ─────────────────────────────────────────────────────────
// Shape of every paged list endpoint (Spring Data's PagedModel).
export interface Page<T> {
  content: T[];
  page: {
    size: number;
    number: number;        // 0-based
    totalElements: number;
    totalPages: number;
  };
}

export interface PageRequest {
  page?: number;           // 0-based
  size?: number;
  sort?: string;           // e.g. 'enrolledAt,desc'
}

// Lightweight projections for selection lists.
export interface StudentOption {
  id: number;
  fullName: string;
  matriculationNumber: string;
}

export interface CourseOption {
  id: number;
  code: string;
  title: string;
  status: CourseStatus;
}

// ── Profile (self-service) ─────────────────────────────────────────
// Profile fields are null for accounts without a Student/Teacher profile (e.g. ADMIN).
export interface Profile {
  username: string;
  email: string;
  role: Role;
  firstName: string | null;
  lastName: string | null;
  matriculationNumber: string | null;
  birthDate: string | null;
  department: string | null;
}

export interface UpdateProfileRequest {
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  birthDate?: string;
  department?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}
