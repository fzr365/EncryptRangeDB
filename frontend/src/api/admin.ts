import { http, jsonPost } from './http';

export interface ExperimentStats {
  totalRecords: number;
  totalIndexedRows: number;
  totalChainNodes: number;
  totalAuditLogs: number;
  avgQueryLatencyMs: number;
  latestQueryHitCount: number;
  latestRangeSpan: number;
  activePolicyName: string;
  activePolicySensitivity: number;
  activePolicySegments: number;
}

export interface OpePolicySegment {
  minValue: number;
  a: number;
  b: number;
  label?: string;
}

export interface OpePolicy {
  id: number | null;
  policyName: string;
  sensitivity: number;
  segments: OpePolicySegment[];
  active: boolean;
  updatedAt?: string;
}

export interface AuditLogItem {
  id: number;
  actionType: string;
  sqlText: string | null;
  tableName: string | null;
  columnName: string | null;
  lowerIndex: number | null;
  upperIndex: number | null;
  hitCount: number | null;
  elapsedMs: number | null;
  status: string;
  detailText: string | null;
  createdAt: string;
}

export interface RebuildEafsResponse {
  rebuiltBuckets: number;
  buckets: string[];
}

export interface KeyStatus {
  activeVersion: string;
  aesFingerprint: string;
  opeFingerprint: string;
  integrityFingerprint: string;
  aesSource: string;
  opeSource: string;
  integritySource: string;
  rotatedAt: string;
}

export interface IntegrityRepairResult {
  repairedRecords: number;
  skippedRecords: number;
}

export const fetchExperimentStats = () => http.get<ExperimentStats>('/admin/stats');

export const fetchOpePolicy = () => http.get<OpePolicy>('/admin/ope-policy');

export const saveOpePolicy = (payload: {
  policyName: string;
  sensitivity: number;
  segments: OpePolicySegment[];
}) => jsonPost<OpePolicy>('/admin/ope-policy', payload);

export const fetchAuditLogs = (limit = 20) =>
  http.get<AuditLogItem[]>('/admin/audit-logs', {
    params: { limit }
  });

export const rebuildEafs = (payload?: { table?: string; column?: string; rebuildAll?: boolean }) =>
  jsonPost<RebuildEafsResponse>('/admin/rebuild-eafs', payload ?? { rebuildAll: true });

export const fetchKeyStatus = () => http.get<KeyStatus>('/admin/keys');

export const rotateDemoKeyVersion = () => jsonPost<KeyStatus>('/admin/keys/rotate-demo');

export const repairIntegrityTags = () => jsonPost<IntegrityRepairResult>('/admin/repair-integrity-tags');
