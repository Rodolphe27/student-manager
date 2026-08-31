import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';
import { AuthProvider } from '../context/AuthContext';
import studentService from '../services/studentService';
import courseService from '../services/courseService';
import enrollmentService from '../services/enrollmentService';

vi.mock('../services/studentService', () => ({
  default: { getAll: vi.fn(), getMe: vi.fn() },
}));
vi.mock('../services/courseService', () => ({
  default: { getAll: vi.fn() },
}));
vi.mock('../services/enrollmentService', () => ({
  default: { getAll: vi.fn(), getByStudent: vi.fn() },
}));

function renderDashboard(role: 'STUDENT' | 'TEACHER' | 'ADMIN') {
  localStorage.setItem('user', JSON.stringify({ token: 't', username: 'r', email: 'r@example.com', role }));
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
    vi.mocked(courseService.getAll).mockResolvedValue({ data: [{ id: 1 }, { id: 2 }] } as never);
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

    expect(studentService.getAll).not.toHaveBeenCalled();
    expect(enrollmentService.getAll).not.toHaveBeenCalled();
    expect(enrollmentService.getByStudent).toHaveBeenCalledWith(9);
  });

  it('for a STUDENT with no linked student record (404), still renders without error', async () => {
    vi.mocked(courseService.getAll).mockResolvedValue({ data: [{ id: 1 }] } as never);
    vi.mocked(studentService.getMe).mockRejectedValue({ response: { status: 404 } });

    renderDashboard('STUDENT');

    expect(await screen.findByText('My Enrollments')).toBeInTheDocument();
    expect(screen.queryByText(/could not load dashboard data/i)).not.toBeInTheDocument();
    expect(enrollmentService.getByStudent).not.toHaveBeenCalled();
  });

  it('for staff, loads totals across everyone', async () => {
    vi.mocked(studentService.getAll).mockResolvedValue({ data: [{ id: 1 }, { id: 2 }, { id: 3 }] } as never);
    vi.mocked(courseService.getAll).mockResolvedValue({ data: [{ id: 1 }] } as never);
    vi.mocked(enrollmentService.getAll).mockResolvedValue({
      data: [{ id: 1, status: 'PENDING', studentName: 'A', courseTitle: 'C', enrolledAt: '2026-01-01', grade: 'NOT_GRADED' }],
    } as never);

    renderDashboard('ADMIN');

    expect(await screen.findByText('Total Students')).toBeInTheDocument();
    expect(studentService.getAll).toHaveBeenCalled();
    expect(enrollmentService.getAll).toHaveBeenCalled();
  });
});
