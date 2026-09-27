import api from './api';
import type { Teacher, CreateTeacherRequest, RegistrationInvite } from '../types';
import type { AxiosResponse } from 'axios';

const teacherService = {
  getAll: (): Promise<AxiosResponse<Teacher[]>> =>
    api.get<Teacher[]>('/teachers'),

  getById: (id: number): Promise<AxiosResponse<Teacher>> =>
    api.get<Teacher>(`/teachers/${id}`),

  create: (data: CreateTeacherRequest): Promise<AxiosResponse<Teacher>> =>
    api.post<Teacher>('/teachers', data),

  update: (id: number, data: CreateTeacherRequest): Promise<AxiosResponse<Teacher>> =>
    api.put<Teacher>(`/teachers/${id}`, data),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/teachers/${id}`),

  issueInvite: (id: number): Promise<AxiosResponse<RegistrationInvite>> =>
    api.post<RegistrationInvite>(`/teachers/${id}/invite`),
};

export default teacherService;
