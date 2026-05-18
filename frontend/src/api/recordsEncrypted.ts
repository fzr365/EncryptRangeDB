import { jsonPost } from './http';

export interface EncryptedField {
  column: string;
  ciphertextBase64: string;
  nonceBase64: string;
  rindex: number | null;
  skindex: number | null;
  segmentId: number | null;
}

export interface EncryptedInsertRequest {
  table: string;
  recordId: string;
  fields: EncryptedField[];
}

export const insertEncrypted = (payload: EncryptedInsertRequest) => jsonPost('/records/encrypted', payload);
