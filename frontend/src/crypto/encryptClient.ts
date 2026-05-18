import { DEMO_AES_KEY_STR } from './demoKey';
import { opeEncrypt } from './ope';

const enc = new TextEncoder();

const bytesToB64 = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes));

const deriveNonce = async (recordId: string, column: string) => {
  // 用记录号和列名派生 nonce，保证同一字段加密过程可复现。
  const data = enc.encode(`${recordId}:${column}`);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return new Uint8Array(digest).slice(0, 12);
};

let cachedEncKey: CryptoKey | null = null;

const importAesKey = async () => {
  if (cachedEncKey) return cachedEncKey;
  const raw = enc.encode(DEMO_AES_KEY_STR);
  cachedEncKey = await crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt']);
  return cachedEncKey;
};

export const encryptFieldClient = async (recordId: string, column: string, value: unknown, indexed: boolean) => {
  // 客户端加密模式：浏览器里先加密，再把密文和索引发给后端。
  const key = await importAesKey();
  const nonce = await deriveNonce(recordId, column);
  const aad = enc.encode(column);
  const plain = enc.encode(String(value));
  const cipherBuf = await crypto.subtle.encrypt({ name: 'AES-GCM', iv: nonce, additionalData: aad }, key, plain);
  const cipherBytes = new Uint8Array(cipherBuf);

  // 需要范围查询的字段才生成 rindex。
  const rindex = indexed ? await opeEncrypt(Number(value)) : null;
  return {
    column,
    ciphertextBase64: bytesToB64(cipherBytes),
    nonceBase64: bytesToB64(nonce),
    rindex,
    skindex: null,
    segmentId: null
  };
};
