import api from './api';
import type  { AuthResponse, LoginRequest, RegisterRequest } from '../types';
import type { AxiosResponse } from 'axios';

// TODO(FE-3) [CRITICAL]: logout/getToken/isAuthenticated all read/write localStorage directly,
// duplicating the storage decision made in AuthContext/api.ts — any fix for the localStorage
// token-storage issue (see AuthContext.tsx) must be applied consistently here too.
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
