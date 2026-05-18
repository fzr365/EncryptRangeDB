import { http, jsonPost } from './http';

export interface LoginResponse {
  token: string;
  username: string;
  role: 'ADMIN' | 'USER' | 'AUDITOR';
  expiresAt: string;
}

export interface CurrentUser {
  username: string;
  role: 'ADMIN' | 'USER' | 'AUDITOR';
}

export const login = (payload: { username: string; password: string }) => jsonPost<LoginResponse>('/auth/login', payload);

export const logout = () => jsonPost('/auth/logout');

export const fetchMe = () => http.get<CurrentUser>('/auth/me');
