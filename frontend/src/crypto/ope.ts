import { DEMO_OPE_KEY_STR } from './demoKey';

const enc = new TextEncoder();

const hmacSha256 = async (keyBytes: Uint8Array, msgBytes: Uint8Array) => {
  const key = await crypto.subtle.importKey('raw', keyBytes, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const mac = await crypto.subtle.sign('HMAC', key, msgBytes);
  return new Uint8Array(mac);
};

const toSeed = (mac: Uint8Array) => {
  // 取前 8 字节转成正数，作为确定性噪声的种子。
  let x = 0n;
  for (let i = 0; i < 8; i++) {
    x = (x << 8n) | BigInt(mac[i]);
  }
  return x & ((1n << 63n) - 1n);
};

type Segment = { a: bigint; b: bigint };

const selectSegment = (value: bigint): Segment => {
  if (value < 100n) return { a: 3n, b: 10n };
  if (value < 500n) return { a: 5n, b: 20n };
  return { a: 10n, b: 30n };
};

export const opeEncrypt = async (value: number, sensitivity: number = 32) => {
  // 和后端保持同一套分段规则，前端才能提交可查询的 rindex。
  const v = BigInt(Math.trunc(value));
  const seg = selectSegment(v);
  const sk = seg.a * v + seg.b;
  const bound = seg.a * BigInt(Math.max(0, sensitivity));
  let noise = 0n;
  if (bound > 0n) {
    const keyBytes = enc.encode(DEMO_OPE_KEY_STR);
    const msg = enc.encode(`default:${v.toString()}`);
    const mac = await hmacSha256(keyBytes, msg);
    const seed = toSeed(mac);
    noise = seed % (bound + 1n);
  }
  return Number(sk + noise);
};
