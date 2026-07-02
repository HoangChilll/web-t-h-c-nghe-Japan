import { api } from '../../shared/api/client';
import type { AuthResponse } from './authStore';

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { email, password });
  return data;
}

export async function register(
  email: string,
  password: string,
  displayName?: string,
): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/register', {
    email,
    password,
    displayName,
  });
  return data;
}

export interface Profile {
  id: number;
  email: string;
  role: string;
  subscriptionStatus: string;
  displayName: string | null;
  jlptTarget: string | null;
  dailyGoalMinutes: number;
}

export async function fetchProfile(): Promise<Profile> {
  const { data } = await api.get<Profile>('/me/profile');
  return data;
}
