import api from './api';
import type  { AuthResponse, LoginRequest } from '../types';
import type { AxiosResponse } from 'axios';

// The login itself lives in the HttpOnly SESSION cookie set by the backend — nothing
// here (or anywhere in the frontend) stores a credential.
const authService = {
  login: (data: LoginRequest): Promise<AxiosResponse<AuthResponse>> =>
    api.post<AuthResponse>('/auth/login', data),

  // The account behind the current session; 401 when not logged in.
  me: (): Promise<AxiosResponse<AuthResponse>> =>
    api.get<AuthResponse>('/auth/me'),

  // Invalidates the server-side session (204).
  logout: (): Promise<AxiosResponse<void>> =>
    api.post('/auth/logout'),
};

export default authService;
