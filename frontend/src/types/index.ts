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
}

export interface CreateStudentRequest {
  firstName: string;
  lastName: string;
  matriculationNumber: string;
  birthDate?: string;
  email: string;
}

export interface Course {
  id: number;
  code: string;
  title: string;
  description: string | null;
  creditHours: number;
  status: CourseStatus;
  active: boolean;
}

export interface CreateCourseRequest {
  code: string;
  title: string;
  description?: string;
  creditHours: number;
  status?: CourseStatus;
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

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
  role: Role;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}
