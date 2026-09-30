import api from './api';
import type { Student, CreateStudentRequest, Page, PageRequest, StudentOption } from '../types';
import type { AxiosResponse } from 'axios';

const studentService = {
  // One page of students; `q` searches name, matriculation number and email.
  search: (params: PageRequest & { q?: string } = {}): Promise<AxiosResponse<Page<Student>>> =>
    api.get<Page<Student>>('/students', { params }),

  // Every student as id + name, for dropdowns.
  options: (): Promise<AxiosResponse<StudentOption[]>> =>
    api.get<StudentOption[]>('/students/options'),

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

};

export default studentService;
