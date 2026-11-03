import axios from 'axios';
import { getAccessToken, setAccessToken } from './authToken';

// The backend runs on http://localhost:8080 with prefix /api/v1
// Vite proxy routes /api to http://localhost:8080
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    // Graceful error reporting without exposing sensitive data
    const message = error.response?.data?.message || error.message || 'An unexpected error occurred';
    console.warn(`[API Error] ${error.config?.method?.toUpperCase()} ${error.config?.url}:`, message);
    return Promise.reject(error);
  }
);

let refreshing: Promise<string | null> | null = null;
apiClient.interceptors.response.use((response) => response, async (error) => {
  const original = error.config;
  if (error.response?.status === 401 && original && !original._retried && !String(original.url).includes('/auth/')) {
    original._retried = true;
    refreshing ??= apiClient.get('/auth/csrf').then(() => apiClient.post('/auth/refresh')).then((res) => {
      setAccessToken(res.data.accessToken as string);
      return res.data.accessToken as string;
    }).catch(() => { setAccessToken(null); window.dispatchEvent(new Event('auth:expired')); return null; }).finally(() => { refreshing = null; });
    const token = await refreshing;
    if (token) { original.headers.Authorization = `Bearer ${token}`; return apiClient(original); }
  }
  return Promise.reject(error);
});
