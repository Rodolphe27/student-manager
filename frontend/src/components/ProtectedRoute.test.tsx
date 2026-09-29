import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { useAuth } from '../context/useAuth';

vi.mock('../context/useAuth', () => ({
  useAuth: vi.fn(),
}));

function renderAt(path: string, roles?: Array<'STUDENT' | 'TEACHER' | 'ADMIN'>) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route path="/" element={<div>Dashboard</div>} />
        <Route
          path="/students"
          element={
            <ProtectedRoute roles={roles}>
              <div>Students Page</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  it('shows a loading spinner while auth is still resolving', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      loading: true,
      isAuthenticated: false,
    } as never);

    const { container } = renderAt('/students');

    expect(container.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('redirects to /login when not authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      loading: false,
      isAuthenticated: false,
    } as never);

    renderAt('/students');

    expect(screen.getByText('Login Page')).toBeInTheDocument();
  });

  it('renders the children when authenticated and no roles are required', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { username: 'stu', email: 'stu@example.com', role: 'STUDENT' },
      loading: false,
      isAuthenticated: true,
    } as never);

    renderAt('/students');

    expect(screen.getByText('Students Page')).toBeInTheDocument();
  });

  it('redirects to / when authenticated but the role is not allowed', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { username: 'stu', email: 'stu@example.com', role: 'STUDENT' },
      loading: false,
      isAuthenticated: true,
    } as never);

    renderAt('/students', ['TEACHER', 'ADMIN']);

    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('renders the children when the role is allowed', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { username: 'admin', email: 'admin@example.com', role: 'ADMIN' },
      loading: false,
      isAuthenticated: true,
    } as never);

    renderAt('/students', ['TEACHER', 'ADMIN']);

    expect(screen.getByText('Students Page')).toBeInTheDocument();
  });
});
