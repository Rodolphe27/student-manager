import axios from 'axios';

// The API is always reached same-origin under /api — proxied by Vite in dev/preview,
// by nginx in docker-compose, and by a Vercel rewrite in production. That keeps the
// HttpOnly SESSION cookie first-party, and lets axios copy the XSRF-TOKEN cookie into
// the X-XSRF-TOKEN header automatically (it only does so for same-origin requests).
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor — treat a 401 as an expired session and bounce to login,
// EXCEPT on the auth endpoints themselves, where a 401 means "bad credentials"
// (login) or "not logged in yet" (the startup /auth/me check) and must surface
// to the caller instead.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url: string = error.config?.url ?? '';
    const isAuthRequest = url.includes('/auth/');
    if (error.response?.status === 401 && !isAuthRequest) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
