import api from './api';
import { AuthResponse, LoginRequest, RegisterRequest } from '../types';
import { AxiosResponse } from 'axios';

const authService = {
  login: (data: LoginRequest): Promise<AxiosResponse<AuthResponse>> =>
    api.post<AuthResponse>('/auth/login', data),

  register: (data: RegisterRequest): Promise<AxiosResponse<AuthResponse>> =>
    api.post<AuthResponse>('/auth/register', data),

  logout: (): void => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  },

  getToken: (): string | null =>
    localStorage.getItem('token'),

  isAuthenticated: (): boolean =>
    localStorage.getItem('token') !== null,
};

export default authService;
