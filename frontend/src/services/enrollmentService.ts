import api from './api';
import type {
  Enrollment,
  EnrollmentStatus,
  CreateEnrollmentRequest,
  UpdateGradeRequest,
  Page,
  PageRequest,
} from '../types';
import type { AxiosResponse } from 'axios';

export interface EnrollmentFilter {
  status?: EnrollmentStatus;
  studentId?: number;
  courseId?: number;
}

const enrollmentService = {
  // One page of enrollments (newest first by default), optionally filtered.
  search: (params: PageRequest & EnrollmentFilter = {}): Promise<AxiosResponse<Page<Enrollment>>> =>
    api.get<Page<Enrollment>>('/enrollments', { params }),

  getByStudent: (studentId: number): Promise<AxiosResponse<Enrollment[]>> =>
    api.get<Enrollment[]>(`/enrollments/student/${studentId}`),

  create: (data: CreateEnrollmentRequest): Promise<AxiosResponse<Enrollment>> =>
    api.post<Enrollment>('/enrollments', data),

  confirm: (id: number): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/confirm`),

  cancel: (id: number): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/cancel`),

  updateGrade: (id: number, data: UpdateGradeRequest): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/grade`, data),

  // Student only: acknowledge the grade of their own enrollment (clears the "new grade" notice).
  markGradeSeen: (id: number): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/grade-seen`),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/enrollments/${id}`),
};

export default enrollmentService;
