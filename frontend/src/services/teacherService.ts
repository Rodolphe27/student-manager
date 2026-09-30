import api from './api';
import type { Teacher, CreateTeacherRequest, Page, PageRequest, TeacherOption } from '../types';
import type { AxiosResponse } from 'axios';

const teacherService = {
  // One page of teachers; `q` searches name, email and department.
  search: (params: PageRequest & { q?: string } = {}): Promise<AxiosResponse<Page<Teacher>>> =>
    api.get<Page<Teacher>>('/teachers', { params }),

  // Every teacher as id + name, for dropdowns.
  options: (): Promise<AxiosResponse<TeacherOption[]>> =>
    api.get<TeacherOption[]>('/teachers/options'),

  // The teacher profile behind the logged-in account (404 when there is none).
  getMe: (): Promise<AxiosResponse<Teacher>> =>
    api.get<Teacher>('/teachers/me'),

  getById: (id: number): Promise<AxiosResponse<Teacher>> =>
    api.get<Teacher>(`/teachers/${id}`),

  create: (data: CreateTeacherRequest): Promise<AxiosResponse<Teacher>> =>
    api.post<Teacher>('/teachers', data),

  update: (id: number, data: CreateTeacherRequest): Promise<AxiosResponse<Teacher>> =>
    api.put<Teacher>(`/teachers/${id}`, data),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/teachers/${id}`),

};

export default teacherService;
