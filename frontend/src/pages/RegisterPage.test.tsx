import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from './RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import authService from '../services/authService';

vi.mock('../services/authService', () => ({
  default: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
  },
}));

function renderRegisterPage() {
  return render(
    <MemoryRouter>
      <AuthProvider>
        <RegisterPage />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.mocked(authService.register).mockReset();
    localStorage.clear();
  });

  it('renders all required fields and the submit button', () => {
    renderRegisterPage();

    expect(screen.getByLabelText(/username/i)).toBeRequired();
    expect(screen.getByLabelText(/email/i)).toBeRequired();
    expect(screen.getByLabelText(/password/i)).toBeRequired();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
  });

  it('submits the form data to authService.register and stores the session on success', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.register).mockResolvedValueOnce({
      data: { token: 'fake-token', username: 'newuser', role: 'STUDENT' },
    } as never);

    renderRegisterPage();

    await user.type(screen.getByLabelText(/username/i), 'newuser');
    await user.type(screen.getByLabelText(/email/i), 'newuser@example.com');
    await user.type(screen.getByLabelText(/password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(authService.register).toHaveBeenCalledWith({
        username: 'newuser',
        email: 'newuser@example.com',
        password: 'Password123!',
      });
    });
    await waitFor(() => expect(localStorage.getItem('token')).toBe('fake-token'));
  });

  it('shows the backend error message when registration fails', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.register).mockRejectedValueOnce({
      response: { data: { message: 'Username already exists: newuser' } },
    });

    renderRegisterPage();

    await user.type(screen.getByLabelText(/username/i), 'newuser');
    await user.type(screen.getByLabelText(/email/i), 'newuser@example.com');
    await user.type(screen.getByLabelText(/password/i), 'Password123!');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Username already exists: newuser')).toBeInTheDocument();
  });
});
