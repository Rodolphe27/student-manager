import { useState, type ReactNode } from 'react';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';
import authService from '../services/authService';
import { AuthContext } from './auth-context';

// The hook lives in ./useAuth and the context object in ./auth-context so this
// file only exports a component (keeps React Fast Refresh working).

function readStoredUser(): AuthResponse | null {
  try {
    const stored = localStorage.getItem('user');
    return stored ? (JSON.parse(stored) as AuthResponse) : null;
  } catch {
    return null;
  }
}

// ── Provider ───────────────────────────────────────────────────────
export function AuthProvider({ children }: { children: ReactNode }) {
  // Resolve the session synchronously from localStorage so there is no
  // logged-out flash and no setState-in-effect on mount.
  const [user, setUser] = useState<AuthResponse | null>(readStoredUser);
  const [loading] = useState<boolean>(false);

  const login = async (data: LoginRequest): Promise<void> => {
    const response = await authService.login(data);
    const authData  = response.data;

    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify(authData));
    setUser(authData);
  };

  const register = async (data: RegisterRequest): Promise<void> => {
    const response = await authService.register(data);
    const authData  = response.data;

    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify(authData));
    setUser(authData);
  };

  const logout = (): void => {
    authService.logout();
    localStorage.removeItem('user');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      login,
      register,
      logout,
      isAuthenticated: user !== null,
    }}>
      {children}
    </AuthContext.Provider>
  );
}
