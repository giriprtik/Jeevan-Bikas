import axios from 'axios';

export const TOKEN_KEY = 'jb.token';
export const USER_KEY = 'jb.user';

/**
 * Same-origin calls: nginx (Docker) and the Vite dev server both proxy /api to the gateway,
 * so the browser never needs CORS. Override with VITE_API_BASE_URL if the UI is hosted elsewhere.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// An expired/invalid session (401) signs the user out; a 403 is shown to the user as a normal error.
api.interceptors.response.use(
  (res) => res,
  (error) => {
    const isLogin = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLogin) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      if (window.location.pathname !== '/login') window.location.assign('/login?expired=1');
    }
    return Promise.reject(error);
  },
);

/** Human-readable message from a problem+json (or legacy {message}) error body. */
export function apiError(err: unknown, fallback = 'Something went wrong.'): string {
  if (axios.isAxiosError(err)) {
    if (!err.response) return 'Cannot reach the server. Check that the services are running.';
    const data = err.response.data as { detail?: string; message?: string } | undefined;
    return data?.detail || data?.message || `${fallback} (HTTP ${err.response.status})`;
  }
  return fallback;
}

export default api;
