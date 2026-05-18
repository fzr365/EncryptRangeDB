import { DEMO_AES_KEY_STR } from './demoKey';

const enc = new TextEncoder();
const dec = new TextDecoder();

const b64ToBytes = (b64: string) => Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));

let cachedKey: CryptoKey | null = null;

const importKey = async () => {
  if (cachedKey) return cachedKey;
  const raw = enc.encode(DEMO_AES_KEY_STR);
  cachedKey = await crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['decrypt']);
  return cachedKey;
};

export const decryptField = async (column: string, ciphertextBase64: string, nonceBase64: string) => {
  // 前端演示解密时也带上列名 AAD，和加密时保持一致。
  const key = await importKey();
  const cipherBytes = b64ToBytes(ciphertextBase64);
  const nonce = b64ToBytes(nonceBase64);
  const aad = enc.encode(column);
  const plainBuf = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: nonce, additionalData: aad },
    key,
    cipherBytes
  );
  return dec.decode(plainBuf);
};
