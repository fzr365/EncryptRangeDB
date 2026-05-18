import axios from 'axios';

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

export const jsonPost = <T = unknown>(url: string, payload?: unknown) =>
  http.post<T>(url, payload, {
    headers: { 'Content-Type': 'application/json' }
  });

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('erdb_token');
  if (token) {
    config.headers.set('X-Auth-Token', token);
  }
  return config;
});

http.interceptors.response.use(
  (resp) => resp,
  (err) => {
    if (err?.response?.status === 401) {
      localStorage.removeItem('erdb_token');
      localStorage.removeItem('erdb_user');
    }
    console.error('API error', err);
    return Promise.reject(err);
  }
);
