import { createContext } from 'react';
import type { AuthResponse, LoginRequest } from '../types';

export interface AuthContextType {
  user: AuthResponse | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  // Replaces the cached account after the user edits their own profile.
  updateUser: (user: AuthResponse) => void;
  isAuthenticated: boolean;
}

export const AuthContext = createContext<AuthContextType | null>(null);
