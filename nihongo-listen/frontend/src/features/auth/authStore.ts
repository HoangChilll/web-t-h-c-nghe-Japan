import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  userId: number;
  email: string;
  role: 'FREE' | 'PREMIUM' | 'ADMIN';
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: { id: number; email: string; role: string } | null;
  setAuth: (resp: AuthResponse) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      setAuth: (resp) =>
        set({
          accessToken: resp.accessToken,
          refreshToken: resp.refreshToken,
          user: { id: resp.userId, email: resp.email, role: resp.role },
        }),
      logout: () => set({ accessToken: null, refreshToken: null, user: null }),
    }),
    { name: 'nihongo-auth' },
  ),
);
