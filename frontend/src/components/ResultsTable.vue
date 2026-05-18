<template>
  <div class="results-page">
    <div class="stats">
      <div class="stat-card">
        <div class="stat-k">记录总数</div>
        <div class="stat-v">{{ store.latestRows.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-k">解密状态</div>
        <div class="stat-v">{{ showPlain ? '已解密展示' : '仅显示密文' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-k">完整性状态</div>
        <div class="stat-v">{{ integritySummary }}</div>
      </div>
    </div>

    <Card class="chain-card" dis-hover>
      <div class="section-title">链式索引结构图</div>
      <div class="section-sub">展示记录顺序、前驱后继关系、RIndex 和链路摘要。</div>
      <div v-if="!chainNodes.length" class="empty-state">暂无链式结构数据</div>
      <div v-else class="chain-strip">
        <div v-for="(node, index) in chainNodes" :key="node.recordId" class="chain-node">
          <div class="chain-node__counter">#{{ node.chainCounter || index + 1 }}</div>
          <div class="chain-node__id">{{ node.recordId }}</div>
          <div class="chain-node__index">RIndex {{ node.rindex }}</div>
          <div class="chain-node__meta">
            <span>Prev: {{ node.prevRecordId || '-' }}</span>
            <span>Next: {{ node.nextRecordId || '-' }}</span>
          </div>
          <div v-if="index < chainNodes.length - 1" class="chain-node__arrow"></div>
        </div>
      </div>
    </Card>

    <div class="header">
      <div>
        <h3>最新加密存储记录</h3>
        <div class="hint">展示数据库中的密文、索引、完整性校验状态和本地解密结果。</div>
      </div>
      <div class="action-bar">
        <Button @click="loadLatest">刷新</Button>
        <Button type="primary" @click="decryptAll">{{ showPlain ? '重新解密' : '解密' }}</Button>
      </div>
    </div>

    <div v-if="!store.latestRows.length" class="empty-state">暂无可展示数据</div>
    <div v-else class="table-scroll-shell">
      <Table class="wide-table" :columns="columns" :data="tableData" border />
    </div>

    <Modal v-model="modalOpen" title="密文载荷详情" :footer-hide="true" width="860">
      <pre class="modal-pre">{{ modalText }}</pre>
    </Modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { useResultsStore } from '../store/useResults';
import { decryptField } from '../crypto/aesgcm';
import { latestRecords, recordDecryptAudit } from '../api/records';

const store = useResultsStore();
const showPlain = ref(false);
const modalOpen = ref(false);
const modalText = ref('');

onMounted(() => {
  void loadLatest();
});

const loadLatest = async () => {
  const { data } = await latestRecords({ table: 'employees', column: 'salary', limit: 20 });
  store.setLatestRows(data.rows);
  showPlain.value = false;
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
  showPlain.value = true;
  for (const row of store.latestRows) {
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
    await recordDecryptAudit({ table: 'employees', recordId, fieldCount: Object.keys(out).length });
  }
};

const shortText = (text: string, max = 64) => (!text ? '' : text.length <= max ? text : `${text.slice(0, max)}...`);

const openBlob = (text: string) => {
  modalText.value = text;
  modalOpen.value = true;
};

const columns = [
  { title: '记录 ID', key: 'recordId', minWidth: 160 },
  { title: 'RIndex', key: 'rindex', width: 100 },
  { title: '完整性', key: 'integrityStatus', width: 120 },
  { title: '密钥版本', key: 'keyVersion', width: 100 },
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
      h(
        'button',
        { class: 'table-link-btn', type: 'button', onClick: () => openBlob(params.row.cipherBlob) },
        [h('span', { class: 'table-link-btn__icon' }, 'i'), h('span', { class: 'table-link-btn__text' }, '查看详情')]
      )
  },
  { title: '姓名', key: 'plainName', width: 120 },
  { title: '薪资', key: 'plainSalary', width: 100 }
];

const tableData = computed(() =>
  store.latestRows.map((row: any) => {
    const recordId = String(row.record_id ?? row.recordId ?? '');
    const plain = store.decrypted[recordId] ?? {};
    const cipherBlob =
      typeof row.cipher_blob === 'string' ? row.cipher_blob : JSON.stringify(row.cipher_blob ?? row.cipherBlob ?? {});
    return {
      recordId,
      rindex: row.rindex ?? '-',
      integrityStatus: row.integrityStatus ?? 'UNKNOWN',
      keyVersion: row.keyVersion ?? row.key_version ?? '-',
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

const chainNodes = computed(() =>
  [...tableData.value].sort((a, b) => Number(a.chainCounter || 0) - Number(b.chainCounter || 0)).slice(0, 8)
);

const integritySummary = computed(() => {
  if (!tableData.value.length) return '暂无';
  const failed = tableData.value.filter((row) => row.integrityStatus === 'FAILED').length;
  return failed ? `${failed} 条异常` : '通过';
});
</script>

<style scoped>
.results-page {
  display: grid;
  gap: 16px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}
.stat-card,
.chain-card {
  border: 1px solid var(--border);
  min-width: 0;
  max-width: 100%;
}
.stat-card {
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff;
}
.stat-k,
.hint,
.section-sub {
  font-size: 12px;
  color: var(--fg-2);
}
.stat-v {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
}
.section-title {
  font-size: 14px;
  font-weight: 800;
}
.chain-strip {
  display: flex;
  gap: 18px;
  overflow-x: auto;
  overflow-y: hidden;
  max-width: 100%;
  padding: 16px 0 4px;
}
.chain-node {
  position: relative;
  flex: 0 0 220px;
  max-width: 220px;
  padding: 14px;
  border: 1px solid #dce7fb;
  border-radius: 8px;
  background: #f8fbff;
}
.chain-node__counter {
  width: fit-content;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(22, 93, 255, 0.1);
  color: var(--primary);
  font-size: 11px;
  font-weight: 800;
}
.chain-node__id {
  margin-top: 10px;
  font-weight: 800;
  word-break: break-all;
}
.chain-node__index,
.chain-node__meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--fg-1);
}
.chain-node__meta {
  display: grid;
  gap: 4px;
}
.chain-node__arrow {
  position: absolute;
  top: 50%;
  right: -18px;
  width: 18px;
  height: 2px;
  background: #9db8ff;
}
.header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.header h3 {
  margin: 0;
  font-size: 14px;
}
.action-bar {
  display: flex;
  gap: 10px;
}
.empty-state {
  padding: 28px 20px;
  border: 1px dashed #c9d8f2;
  border-radius: 8px;
  background: #f8fbff;
  text-align: center;
}
.table-scroll-shell {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
}
:deep(.wide-table) {
  min-width: 1420px;
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
  .stats {
    grid-template-columns: 1fr;
  }
  .header {
    flex-direction: column;
  }
}
</style>
