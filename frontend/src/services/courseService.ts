import api from './api';
import type { Course, CreateCourseRequest, CourseStatus, Page, PageRequest, CourseOption } from '../types';
import type { AxiosResponse } from 'axios';

const courseService = {
  // One page of courses; `q` searches code and title, `status` narrows to one status.
  search: (params: PageRequest & { q?: string; status?: CourseStatus } = {}): Promise<AxiosResponse<Page<Course>>> =>
    api.get<Page<Course>>('/courses', { params }),

  // Every course as id + code + title, for dropdowns.
  options: (): Promise<AxiosResponse<CourseOption[]>> =>
    api.get<CourseOption[]>('/courses/options'),

  getById: (id: number): Promise<AxiosResponse<Course>> =>
    api.get<Course>(`/courses/${id}`),

  getByStatus: (status: CourseStatus): Promise<AxiosResponse<Course[]>> =>
    api.get<Course[]>(`/courses/status/${status}`),

  create: (data: CreateCourseRequest): Promise<AxiosResponse<Course>> =>
    api.post<Course>('/courses', data),

  update: (id: number, data: CreateCourseRequest): Promise<AxiosResponse<Course>> =>
    api.put<Course>(`/courses/${id}`, data),

  delete: (id: number): Promise<AxiosResponse<void>> =>
    api.delete(`/courses/${id}`),
};

export default courseService;
