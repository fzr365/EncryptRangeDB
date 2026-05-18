import { http } from './http';

export const translateSql = (sql: string) =>
  http.post('/sql/translate', sql, { headers: { 'Content-Type': 'text/plain' } });

