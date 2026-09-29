import { useEffect, useState, type ReactNode } from 'react';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';
import authService from '../services/authService';
import { AuthContext } from './auth-context';

// The hook lives in ./useAuth and the context object in ./auth-context so this
// file only exports a component (keeps React Fast Refresh working).

// ── Provider ───────────────────────────────────────────────────────
export function AuthProvider({ children }: { children: ReactNode }) {
  // The session is an HttpOnly cookie JS can't read, so on startup ask the backend
  // who we are. `loading` stays true until that answer arrives (ProtectedRoute
  // shows a spinner meanwhile instead of bouncing to /login).
  const [user, setUser]       = useState<AuthResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    // Drop credentials left behind by the old localStorage-JWT version of the app.
    localStorage.removeItem('token');
    localStorage.removeItem('user');

    let cancelled = false;
    authService.me()
      .then((response) => { if (!cancelled) setUser(response.data); })
      .catch(() => { /* 401 = not logged in — user stays null */ })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const login = async (data: LoginRequest): Promise<void> => {
    const response = await authService.login(data);
    setUser(response.data);
  };

  const register = async (data: RegisterRequest): Promise<void> => {
    const response = await authService.register(data);
    setUser(response.data);
  };

  const logout = async (): Promise<void> => {
    try {
      await authService.logout();
    } catch {
      // Even if the server call fails (e.g. session already expired), the user
      // asked to leave — clear the client state regardless.
    }
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
