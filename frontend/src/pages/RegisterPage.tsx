import { useState, type FormEvent } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { getErrorMessage } from '../services/errorMessage';
import  type { RegisterRequest } from '../types';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate     = useNavigate();
  const [searchParams] = useSearchParams();

  // A registration link sent via InviteModal carries ?code=... — prefill it so
  // the person doesn't have to copy/paste it separately.
  const [form, setForm]       = useState<RegisterRequest>({
    username: '',
    email: '',
    password: '',
    registrationCode: searchParams.get('code') ?? '',
  });
  const [error, setError]     = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // Only send registrationCode when it's actually filled in — an empty
      // string is meaningless to the backend and keeps the payload the same
      // shape as a plain (code-less) registration.
      const code = form.registrationCode?.trim();
      await register(code ? form : { username: form.username, email: form.email, password: form.password });
      navigate('/');
    } catch (err: unknown) {
      setError(getErrorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 w-full max-w-sm">

        {/* Logo */}
        <div className="flex items-center gap-3 mb-8">
          <div className="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center text-white font-bold">
            SM
          </div>
          <div>
            <h1 className="text-lg font-bold text-gray-800">StudentManager</h1>
            <p className="text-xs text-gray-400">FH Dortmund</p>
          </div>
        </div>

        <h2 className="text-xl font-bold text-gray-800 mb-1">Create account</h2>
        <p className="text-sm text-gray-400 mb-6">Register a new account</p>

        {/* Error */}
        {error && (
          <div className="bg-red-50 border border-red-100 text-red-600 text-sm px-4 py-3 rounded-lg mb-4">
            {error}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
              Username
            </label>
            <input
              id="username"
              type="text"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Choose a username"
              required
            />
          </div>

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email
            </label>
            <input
              id="email"
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Enter your email"
              required
            />
          </div>

          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            {/* UX-only hint matching the backend's actual rule (RegisterRequest.password
                @Pattern) — the backend remains the source of truth and re-validates regardless. */}
            <input
              id="password"
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Choose a password"
              minLength={8}
              pattern="(?=.*[A-Za-z])(?=.*\d).{8,}"
              title="At least 8 characters, including a letter and a digit"
              required
            />
            <p className="text-xs text-gray-400 mt-1">
              At least 8 characters, including a letter and a digit.
            </p>
          </div>

          <div>
            <label htmlFor="registrationCode" className="block text-sm font-medium text-gray-700 mb-1">
              Registration Code <span className="text-gray-400 font-normal">(optional)</span>
            </label>
            <input
              id="registrationCode"
              type="text"
              value={form.registrationCode}
              onChange={(e) => setForm({ ...form, registrationCode: e.target.value })}
              className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="From your invite link, if you have one"
            />
            <p className="text-xs text-gray-400 mt-1">
              Links your account to a student or teacher profile an admin already set up for you.
            </p>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 text-white py-2 rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {loading ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        {/* Login link */}
        <p className="text-center text-sm text-gray-400 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
