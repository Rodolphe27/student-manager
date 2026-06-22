import api from './api';
import {
  Enrollment,
  CreateEnrollmentRequest,
  UpdateGradeRequest
} from '../types';
import { AxiosResponse } from 'axios';

const enrollmentService = {
  getAll: (): Promise<AxiosResponse<Enrollment[]>> =>
    api.get<Enrollment[]>('/enrollments'),

  getById: (id: number): Promise<AxiosResponse<Enrollment>> =>
    api.get<Enrollment>(`/enrollments/${id}`),

  getByStudent: (studentId: number): Promise<AxiosResponse<Enrollment[]>> =>
    api.get<Enrollment[]>(`/enrollments/student/${studentId}`),

  getByCourse: (courseId: number): Promise<AxiosResponse<Enrollment[]>> =>
    api.get<Enrollment[]>(`/enrollments/course/${courseId}`),

  create: (data: CreateEnrollmentRequest): Promise<AxiosResponse<Enrollment>> =>
    api.post<Enrollment>('/enrollments', data),

  confirm: (id: number): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/confirm`),

  cancel: (id: number): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/cancel`),

  updateGrade: (id: number, data: UpdateGradeRequest): Promise<AxiosResponse<Enrollment>> =>
    api.patch<Enrollment>(`/enrollments/${id}/grade`, data),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/enrollments/${id}`),
};

export default enrollmentService;
