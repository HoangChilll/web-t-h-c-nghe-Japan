import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../../features/auth/authStore';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const { refreshToken, setAuth, logout } = useAuthStore.getState();
  if (!refreshToken) return null;
  try {
    // Dùng axios gốc để không đi qua interceptor (tránh vòng lặp)
    const { data } = await axios.post(
      `${import.meta.env.VITE_API_URL ?? '/api'}/auth/refresh`,
      { refreshToken },
    );
    setAuth(data);
    return data.accessToken as string;
  } catch {
    logout();
    return null;
  }
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    if (error.response?.status === 401 && original && !original._retried) {
      original._retried = true;
      refreshing ??= refreshAccessToken().finally(() => {
        refreshing = null;
      });
      const newToken = await refreshing;
      if (newToken) {
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      }
    }
    return Promise.reject(error);
  },
);
