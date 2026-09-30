import { createContext } from 'react';
import type { AuthResponse, LoginRequest } from '../types';

export interface AuthContextType {
  user: AuthResponse | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
}

export const AuthContext = createContext<AuthContextType | null>(null);
