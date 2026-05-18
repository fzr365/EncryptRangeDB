import { http } from './http';

export const importSqlFile = (file: File) => {
  const form = new FormData();
  form.append('file', file);
  return http.post('/sql/import', form);
};

