import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import EnrollmentsPage from './EnrollmentsPage';
import { AuthProvider } from '../context/AuthContext';
import authService from '../services/authService';
import enrollmentService from '../services/enrollmentService';
import studentService from '../services/studentService';
import courseService from '../services/courseService';

vi.mock('../services/authService', () => ({
  default: { me: vi.fn(), login: vi.fn(), logout: vi.fn() },
}));
vi.mock('../services/enrollmentService', () => ({
  default: { search: vi.fn(), updateGrade: vi.fn(), confirm: vi.fn(), cancel: vi.fn(), delete: vi.fn(), create: vi.fn() },
}));
vi.mock('../services/studentService', () => ({ default: { options: vi.fn() } }));
vi.mock('../services/courseService', () => ({ default: { options: vi.fn() } }));

const confirmed = {
  id: 7, studentId: 1, studentName: 'Ada Lovelace', courseId: 2, courseTitle: 'Intro', courseCode: 'CS-101',
  enrolledAt: '2026-09-01', status: 'CONFIRMED', grade: 'NOT_GRADED', confirmed: true, gradeSeen: true,
};

describe('EnrollmentsPage grading', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(authService.me).mockResolvedValue({ data: { username: 't', email: 't@example.com', role: 'TEACHER' } } as never);
    vi.mocked(studentService.options).mockResolvedValue({ data: [] } as never);
    vi.mocked(courseService.options).mockResolvedValue({ data: [] } as never);
    vi.mocked(enrollmentService.search).mockResolvedValue({
      data: { content: [confirmed], page: { size: 10, number: 0, totalElements: 1, totalPages: 1 } },
    } as never);
    vi.mocked(enrollmentService.updateGrade).mockResolvedValue({ data: {} } as never);
  });

  it('only saves a grade after the teacher explicitly confirms it', async () => {
    render(<MemoryRouter><AuthProvider><EnrollmentsPage /></AuthProvider></MemoryRouter>);
    const select = await screen.findByRole('combobox', { name: /Grade for Ada Lovelace/ });
    expect(screen.queryByRole('button', { name: 'Save grade' })).not.toBeInTheDocument();

    await userEvent.selectOptions(select, 'B');

    // Picking a grade is only a draft: nothing is sent yet.
    expect(enrollmentService.updateGrade).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Save grade' }));

    expect(enrollmentService.updateGrade).toHaveBeenCalledWith(7, { grade: 'B' });
  });
});
