import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { AuthProvider } from '../context/AuthContext';
import studentService from '../services/studentService';
import courseService from '../services/courseService';
import enrollmentService from '../services/enrollmentService';
import authService from '../services/authService';

vi.mock('../services/authService', () => ({
  default: { me: vi.fn(), login: vi.fn(), logout: vi.fn() },
}));
vi.mock('../services/studentService', () => ({
  default: { search: vi.fn(), getMe: vi.fn() },
}));
vi.mock('../services/courseService', () => ({
  default: { search: vi.fn() },
}));
vi.mock('../services/enrollmentService', () => ({
  default: { search: vi.fn(), getByStudent: vi.fn() },
}));

// A paged API response carrying `totalElements` rows in total.
function page<T>(content: T[], totalElements = content.length) {
  return { data: { content, page: { size: content.length, number: 0, totalElements, totalPages: 1 } } } as never;
}

function renderDashboard(role: 'STUDENT' | 'TEACHER' | 'ADMIN') {
  // The startup session check (/auth/me) resolves to this user.
  vi.mocked(authService.me).mockResolvedValue({ data: { username: 'r', email: 'r@example.com', role } } as never);
  return render(
    <MemoryRouter>
      <AuthProvider>
        <Dashboard />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('Dashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('for a STUDENT, scopes to their own data and never calls the staff-only list endpoints', async () => {
    vi.mocked(courseService.search).mockResolvedValue(page([{ id: 1 }], 2));
    vi.mocked(studentService.getMe).mockResolvedValue({ data: { id: 9 } } as never);
    vi.mocked(enrollmentService.getByStudent).mockResolvedValue({
      data: [
        { id: 1, status: 'CONFIRMED', studentName: 'R', courseTitle: 'CS', enrolledAt: '2026-01-01', grade: 'A' },
        { id: 2, status: 'PENDING', studentName: 'R', courseTitle: 'Math', enrolledAt: '2026-01-02', grade: 'NOT_GRADED' },
      ],
    } as never);

    renderDashboard('STUDENT');

    expect(await screen.findByText('My Enrollments')).toBeInTheDocument();
    expect(screen.getByText('Available Courses')).toBeInTheDocument();
    expect(screen.queryByText('Total Students')).not.toBeInTheDocument();
    expect(screen.queryByText(/could not load dashboard data/i)).not.toBeInTheDocument();

    expect(studentService.search).not.toHaveBeenCalled();
    expect(enrollmentService.search).not.toHaveBeenCalled();
    expect(enrollmentService.getByStudent).toHaveBeenCalledWith(9);
  });

  it('for a STUDENT with no linked student record (404), still renders without error', async () => {
    vi.mocked(courseService.search).mockResolvedValue(page([{ id: 1 }]));
    vi.mocked(studentService.getMe).mockRejectedValue({ response: { status: 404 } });

    renderDashboard('STUDENT');

    expect(await screen.findByText('My Enrollments')).toBeInTheDocument();
    expect(screen.queryByText(/could not load dashboard data/i)).not.toBeInTheDocument();
    expect(enrollmentService.getByStudent).not.toHaveBeenCalled();
  });

  it('for staff, reads totals from page counts instead of downloading every row', async () => {
    vi.mocked(studentService.search).mockResolvedValue(page([{ id: 1 }], 42));
    vi.mocked(courseService.search).mockResolvedValue(page([{ id: 1 }], 8));
    vi.mocked(enrollmentService.search).mockImplementation((params) =>
      Promise.resolve(params?.status === 'PENDING'
        ? page([{ id: 1 }], 6)
        : page([{ id: 1, status: 'PENDING', studentName: 'A', courseTitle: 'C', enrolledAt: '2026-01-01', grade: 'NOT_GRADED' }], 57)),
    );

    renderDashboard('ADMIN');

    expect(await screen.findByText('Total Students')).toBeInTheDocument();
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('57')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(studentService.search).toHaveBeenCalledWith({ size: 1 });
    expect(courseService.search).toHaveBeenCalledWith({ status: 'ACTIVE', size: 1 });
    expect(enrollmentService.search).toHaveBeenCalledWith({ status: 'PENDING', size: 1 });
  });
});
