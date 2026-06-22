import api from './api';
import { Student, CreateStudentRequest } from '../types';
import { AxiosResponse } from 'axios';

const studentService = {
  getAll: (): Promise<AxiosResponse<Student[]>> =>
    api.get<Student[]>('/students'),

  getById: (id: number): Promise<AxiosResponse<Student>> =>
    api.get<Student>(`/students/${id}`),

  create: (data: CreateStudentRequest): Promise<AxiosResponse<Student>> =>
    api.post<Student>('/students', data),

  update: (id: number, data: CreateStudentRequest): Promise<AxiosResponse<Student>> =>
    api.put<Student>(`/students/${id}`, data),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/students/${id}`),
};

export default studentService;
