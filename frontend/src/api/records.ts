import { http, jsonPost } from './http';

export interface PlainField {
  column: string;
  value: unknown;
  indexed: boolean;
}

export interface PlainInsertRequest {
  table: string;
  recordId: string;
  fields: PlainField[];
}

export const insertPlain = (payload: PlainInsertRequest) => jsonPost('/records/plain', payload);
export const rangeQuery = (payload: unknown) => jsonPost('/records/range', payload);
export const latestRecords = (params?: { table?: string; column?: string; limit?: number }) =>
  http.get('/records/latest', { params });
export const recordDecryptAudit = (payload: { table?: string; recordId?: string; fieldCount?: number }) =>
  jsonPost('/records/decrypt-audit', payload);
