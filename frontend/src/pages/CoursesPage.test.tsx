import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CoursesPage from './CoursesPage';
import { AuthProvider } from '../context/AuthContext';
import authService from '../services/authService';
import courseService from '../services/courseService';
import teacherService from '../services/teacherService';
import termService from '../services/termService';

vi.mock('../services/authService', () => ({
  default: { me: vi.fn(), login: vi.fn(), logout: vi.fn() },
}));
vi.mock('../services/courseService', () => ({
  default: { search: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn() },
}));
vi.mock('../services/teacherService', () => ({
  default: { options: vi.fn(), getMe: vi.fn() },
}));
vi.mock('../services/termService', () => ({
  default: { list: vi.fn(), create: vi.fn() },
}));

const course = (id: number, code: string, teacherId: number | null) => ({
  id, code, title: `Title ${code}`, description: null, creditHours: 5, status: 'ACTIVE', active: true,
  teacherId, teacherName: teacherId ? `Teacher ${teacherId}` : null, termId: null, termName: null, version: 0,
});

function renderPage(role: 'STUDENT' | 'TEACHER' | 'ADMIN') {
  vi.mocked(authService.me).mockResolvedValue({ data: { username: 'r', email: 'r@example.com', role } } as never);
  return render(
    <MemoryRouter>
      <AuthProvider>
        <CoursesPage />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('CoursesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(courseService.search).mockResolvedValue({
      data: {
        content: [course(1, 'CS-101', 7), course(2, 'MA-101', 8)],
        page: { size: 10, number: 0, totalElements: 2, totalPages: 1 },
      },
    } as never);
    vi.mocked(termService.list).mockResolvedValue({ data: [] } as never);
    vi.mocked(teacherService.options).mockResolvedValue({ data: [] } as never);
  });

  it('shows a STUDENT the catalogue read-only, without any management controls', async () => {
    renderPage('STUDENT');

    expect(await screen.findByText('CS-101')).toBeInTheDocument();
    expect(screen.getByText('Teacher 7')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '+ Add Course' })).not.toBeInTheDocument();
    expect(screen.queryByText('Actions')).not.toBeInTheDocument();
    expect(termService.list).not.toHaveBeenCalled();
  });

  it('lets a TEACHER edit and delete only the courses they run', async () => {
    vi.mocked(teacherService.getMe).mockResolvedValue({ data: { id: 7 } } as never);

    renderPage('TEACHER');

    expect(await screen.findByText('CS-101')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: '+ Add Course' })).toBeInTheDocument();
    // One Edit/Delete pair: for CS-101 (teacher 7), none for MA-101 (teacher 8).
    expect(screen.getAllByRole('button', { name: 'Edit' })).toHaveLength(1);
    expect(screen.getAllByRole('button', { name: 'Delete' })).toHaveLength(1);
  });

  it('tells a TEACHER without a teacher profile why they cannot manage courses', async () => {
    vi.mocked(teacherService.getMe).mockRejectedValue({ response: { status: 404 } });

    renderPage('TEACHER');

    expect(await screen.findByRole('alert')).toHaveTextContent('not linked to a teacher profile');
    expect(screen.queryByRole('button', { name: '+ Add Course' })).not.toBeInTheDocument();
  });

  it('lets an ADMIN manage every course and add courses', async () => {
    renderPage('ADMIN');

    expect(await screen.findByText('CS-101')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: '+ Add Course' })).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Edit' })).toHaveLength(2);
  });
});
