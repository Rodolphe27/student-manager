import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:5030/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// TODO(FE-2) [CRITICAL]: reads the token from localStorage, same XSS-exfiltration surface as
// AuthContext.login/register — see the TODO there for the fix direction.
// Request interceptor — adds JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — treat a 401 as an expired session and bounce to login,
// EXCEPT on the auth endpoints themselves, where a 401 just means "bad
// credentials" and must surface to the calling page.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url: string = error.config?.url ?? '';
    const isAuthRequest = url.includes('/auth/');
    if (error.response?.status === 401 && !isAuthRequest) {
      // TODO(FE-11) [LOW]: doesn't clear the stored 'user' object (stale role/user data survives
      // a forced logout), and uses a hard reload instead of router-based navigation.
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
