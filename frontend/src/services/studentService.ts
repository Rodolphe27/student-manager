import api from './api';
import type { Student, CreateStudentRequest, RegistrationInvite } from '../types';
import type { AxiosResponse } from 'axios';

const studentService = {
  getAll: (): Promise<AxiosResponse<Student[]>> =>
    api.get<Student[]>('/students'),

  getById: (id: number): Promise<AxiosResponse<Student>> =>
    api.get<Student>(`/students/${id}`),

  getMe: (): Promise<AxiosResponse<Student>> =>
    api.get<Student>('/students/me'),

  create: (data: CreateStudentRequest): Promise<AxiosResponse<Student>> =>
    api.post<Student>('/students', data),

  update: (id: number, data: CreateStudentRequest): Promise<AxiosResponse<Student>> =>
    api.put<Student>(`/students/${id}`, data),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/students/${id}`),

  issueInvite: (id: number): Promise<AxiosResponse<RegistrationInvite>> =>
    api.post<RegistrationInvite>(`/students/${id}/invite`),
};

export default studentService;
