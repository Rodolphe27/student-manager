import api from './api';
import { Course, CreateCourseRequest, CourseStatus } from '../types';
import { AxiosResponse } from 'axios';

const courseService = {
  getAll: (): Promise<AxiosResponse<Course[]>> =>
    api.get<Course[]>('/courses'),

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
