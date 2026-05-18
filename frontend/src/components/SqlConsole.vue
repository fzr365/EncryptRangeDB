<template>
  <div class="query-page">
    <div class="stats">
      <div class="stat-card"><div class="stat-k">查询表</div><div class="stat-v">{{ store.range?.table || 'employees' }}</div></div>
      <div class="stat-card"><div class="stat-k">索引字段</div><div class="stat-v">{{ store.range?.column || 'salary' }}</div></div>
      <div class="stat-card"><div class="stat-k">命中记录</div><div class="stat-v">{{ store.rows.length }}</div></div>
      <div class="stat-card"><div class="stat-k">当前模式</div><div class="stat-v">密文范围查询</div></div>
    </div>

    <Card class="query-card" dis-hover>
      <div class="section-title">SQL 范围查询</div>
      <div class="section-sub">输入包含 BETWEEN 或上下界条件的 SQL，系统会翻译为索引区间后执行密文查询。</div>
      <Input v-model="sql" type="textarea" :rows="5" placeholder="SELECT * FROM employees WHERE salary BETWEEN 5000 AND 9000" />
      <div class="actions">
        <Button type="primary" @click="translate">翻译</Button>
        <Button type="success" @click="execute">执行</Button>
        <Button :disabled="!store.rows.length" @click="decryptAll">{{ showPlain ? '重新解密' : '解密结果' }}</Button>
      </div>
      <Alert v-if="store.range" type="info" show-icon>索引范围：{{ store.range.lowerIndex }} ~ {{ store.range.upperIndex }}</Alert>
    </Card>

    <Card class="metric-card" dis-hover>
      <div class="section-title">查询结果指标</div>
      <div class="metric-grid">
        <div class="metric-item"><span>翻译耗时</span><strong>{{ metrics.translateMs }} ms</strong></div>
        <div class="metric-item"><span>查询耗时</span><strong>{{ metrics.queryMs }} ms</strong></div>
        <div class="metric-item"><span>解密耗时</span><strong>{{ metrics.decryptMs }} ms</strong></div>
        <div class="metric-item"><span>命中条数</span><strong>{{ metrics.hitCount }}</strong></div>
        <div class="metric-item"><span>索引跨度</span><strong>{{ metrics.indexSpan }}</strong></div>
        <div class="metric-item"><span>结果状态</span><strong>{{ metrics.hitCount ? '已返回结果' : '待执行' }}</strong></div>
      </div>
    </Card>

    <Card class="flow-card" dis-hover>
      <div class="section-title">查询流程可视化</div>
      <div class="flow-pipeline">
        <div v-for="(step, index) in flowSteps" :key="step.title" class="flow-step" :class="{ 'flow-step--active': index <= activeStepIndex }">
          <div class="flow-step__head"><div class="flow-step__no">{{ index + 1 }}</div><div class="flow-step__title">{{ step.title }}</div></div>
          <div class="flow-step__body">{{ step.detail }}</div>
        </div>
      </div>
    </Card>

    <Card class="result-card" dis-hover>
      <div class="section-title">查询结果</div>
      <div v-if="!store.rows.length" class="empty-state">当前还没有查询结果</div>
      <div v-else class="table-scroll-shell"><Table class="wide-table" :columns="columns" :data="tableData" border /></div>
    </Card>

    <Modal v-model="modalOpen" title="密文载荷详情" :footer-hide="true" width="860">
      <pre class="modal-pre">{{ modalText }}</pre>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue';
import { translateSql } from '../api/sql';
import { rangeQuery, recordDecryptAudit } from '../api/records';
import { useResultsStore } from '../store/useResults';
import { decryptField } from '../crypto/aesgcm';

const store = useResultsStore();
const sql = ref('SELECT * FROM employees WHERE salary BETWEEN 5000 AND 9000');
const showPlain = ref(false);
const modalOpen = ref(false);
const modalText = ref('');
const activeStepIndex = ref(0);
const metrics = reactive({ translateMs: 0, queryMs: 0, decryptMs: 0, hitCount: 0, indexSpan: 0 });

const flowSteps = computed(() => [
  { title: '输入明文 SQL', detail: sql.value || '等待输入查询语句' },
  { title: '解析范围边界', detail: store.range ? `${store.range.table}.${store.range.column}` : '提取表名、字段和区间条件' },
  { title: '映射索引区间', detail: store.range ? `${store.range.lowerIndex} ~ ${store.range.upperIndex}` : '映射为可比较密文索引' },
  { title: '返回密文结果', detail: store.rows.length ? `返回 ${store.rows.length} 条记录` : '等待执行查询' }
]);

const translate = async () => {
  const start = performance.now();
  const { data } = await translateSql(sql.value);
  metrics.translateMs = Math.round(performance.now() - start);
  store.setRange(data);
  metrics.indexSpan = Math.max(0, data.upperIndex - data.lowerIndex);
  activeStepIndex.value = 2;
};

const execute = async () => {
  showPlain.value = false;
  metrics.decryptMs = 0;
  if (!store.range) await translate();
  const start = performance.now();
  const { data } = await rangeQuery(store.range);
  metrics.queryMs = Math.round(performance.now() - start);
  metrics.hitCount = data.rows.length;
  store.setRows(data.rows);
  activeStepIndex.value = 3;
};

const parseBlob = (blob: unknown) => {
  if (!blob) return null;
  if (typeof blob === 'string') {
    try {
      return JSON.parse(blob);
    } catch {
      return null;
    }
  }
  return blob as any;
};

const decryptAll = async () => {
  const start = performance.now();
  showPlain.value = true;
  for (const row of store.rows) {
    const recordId = String(row.record_id ?? row.recordId ?? '');
    const blob = parseBlob(row.cipher_blob ?? row.cipherBlob);
    if (!blob || !Array.isArray(blob.fields)) continue;
    const out: Record<string, string> = {};
    for (const f of blob.fields) {
      if (!f?.ciphertextBase64 || !f?.nonceBase64) continue;
      try {
        out[f.column] = await decryptField(f.column, f.ciphertextBase64, f.nonceBase64);
      } catch {
        out[f.column] = '(decrypt failed)';
      }
    }
    store.setDecrypted(recordId, out);
    await recordDecryptAudit({ table: store.range?.table, recordId, fieldCount: Object.keys(out).length });
  }
  metrics.decryptMs = Math.round(performance.now() - start);
};

const shortText = (text: string, max = 80) => (text && text.length > max ? `${text.slice(0, max)}...` : text || '');
const openBlob = (text: string) => {
  modalText.value = text;
  modalOpen.value = true;
};

const columns = [
  { title: '记录 ID', key: 'recordId', minWidth: 160 },
  { title: 'RIndex', key: 'rindex', width: 100 },
  { title: '完整性', key: 'integrityStatus', width: 120 },
  { title: '前驱记录', key: 'prevRecordId', minWidth: 120 },
  { title: '后继记录', key: 'nextRecordId', minWidth: 120 },
  { title: '链计数器', key: 'chainCounter', width: 100 },
  { title: '链钥摘要', key: 'chainKeyShort', minWidth: 160 },
  { title: '密文摘要', key: 'cipherBlobShort', minWidth: 220 },
  {
    title: '查看',
    key: 'view',
    width: 110,
    render: (_: unknown, params: any) =>
      h('button', { class: 'table-link-btn', type: 'button', onClick: () => openBlob(params.row.cipherBlob) }, [
        h('span', { class: 'table-link-btn__icon' }, 'i'),
        h('span', { class: 'table-link-btn__text' }, '查看详情')
      ])
  },
  { title: '姓名', key: 'plainName', width: 120 },
  { title: '薪资', key: 'plainSalary', width: 100 }
];

const tableData = computed(() =>
  store.rows.map((row: any) => {
    const recordId = String(row.record_id ?? row.recordId ?? '');
    const plain = store.decrypted[recordId] ?? {};
    const cipherBlob = typeof row.cipher_blob === 'string' ? row.cipher_blob : JSON.stringify(row.cipher_blob ?? row.cipherBlob ?? {});
    return {
      recordId,
      rindex: row.rindex ?? '-',
      integrityStatus: row.integrityStatus ?? 'UNKNOWN',
      prevRecordId: row.prevRecordId ?? '-',
      nextRecordId: row.nextRecordId ?? '-',
      chainCounter: row.chainCounter ?? '-',
      chainKeyShort: shortText(String(row.chainKeyHex ?? ''), 24),
      cipherBlob,
      cipherBlobShort: shortText(cipherBlob, 52),
      plainName: showPlain.value ? (plain.name ?? '') : '',
      plainSalary: showPlain.value ? (plain.salary ?? '') : ''
    };
  })
);
</script>

<style scoped>
.query-page {
  display: grid;
  gap: 16px;
}
.stats,
.metric-grid,
.flow-pipeline {
  display: grid;
  gap: 12px;
}
.stats {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.metric-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.flow-pipeline {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.stat-card,
.query-card,
.flow-card,
.result-card,
.metric-card {
  border: 1px solid var(--border);
}
.stat-card,
.metric-item {
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff;
}
.stat-k,
.section-sub,
.metric-item span {
  font-size: 12px;
  color: var(--fg-2);
}
.stat-v,
.metric-item strong {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
}
.section-title {
  font-size: 14px;
  font-weight: 800;
}
.actions {
  display: flex;
  gap: 10px;
  margin: 12px 0;
}
.metric-item {
  display: grid;
  gap: 6px;
  border: 1px solid #e8eef7;
}
.flow-step {
  min-height: 120px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fbfcfe;
}
.flow-step--active {
  border-color: rgba(22, 93, 255, 0.35);
  box-shadow: 0 8px 20px rgba(22, 93, 255, 0.08);
}
.flow-step__head {
  display: flex;
  gap: 10px;
  align-items: center;
}
.flow-step__no {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: var(--primary-weak);
  color: var(--primary);
  font-weight: 800;
}
.flow-step__title {
  font-size: 13px;
  font-weight: 800;
}
.flow-step__body {
  margin-top: 12px;
  font-size: 12px;
  color: var(--fg-1);
}
.empty-state {
  padding: 28px 20px;
  border: 1px dashed #c9d8f2;
  border-radius: 8px;
  background: #f8fbff;
  text-align: center;
}
.table-scroll-shell {
  overflow-x: auto;
}
:deep(.wide-table) {
  min-width: 1560px;
}
.modal-pre {
  max-height: 540px;
  overflow: auto;
  padding: 12px;
  border-radius: 8px;
  background: #0f172a;
  color: #e5e7eb;
  font-size: 12px;
}
@media (max-width: 980px) {
  .stats,
  .metric-grid,
  .flow-pipeline {
    grid-template-columns: 1fr;
  }
}
</style>
