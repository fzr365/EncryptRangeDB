<template>
  <div class="panel-page">
    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">当前密钥版本</div>
        <div class="summary-value">{{ status?.activeVersion || '-' }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">AES 指纹</div>
        <div class="summary-value mono">{{ status?.aesFingerprint || '-' }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">完整性密钥指纹</div>
        <div class="summary-value mono">{{ status?.integrityFingerprint || '-' }}</div>
      </div>
    </div>

    <Card class="key-card" dis-hover>
      <div class="section-head">
        <div>
          <div class="section-title">密钥管理</div>
          <div class="section-sub">展示密钥版本、配置来源和指纹信息，不展示密钥原文。</div>
        </div>
        <div class="head-actions">
          <Button @click="loadStatus">刷新状态</Button>
          <Button type="primary" @click="rotateVersion">更新密钥版本</Button>
          <Button :loading="repairing" @click="repairTags">修复完整性标签</Button>
        </div>
      </div>

      <Table class="key-table" :columns="columns" :data="tableData" border />
      <Alert type="info" show-icon class="tip">
        版本更新后，后续写入记录将使用新的 `keyVersion` 标识，用于跟踪密钥生命周期状态。
      </Alert>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Message } from 'view-ui-plus';
import { fetchKeyStatus, repairIntegrityTags, rotateDemoKeyVersion, type KeyStatus } from '../api/admin';

const status = ref<KeyStatus | null>(null);
const repairing = ref(false);

const columns = [
  { title: '密钥类型', key: 'type', width: 180 },
  { title: '指纹', key: 'fingerprint', width: 220 },
  { title: '来源', key: 'source', minWidth: 220 }
];

const tableData = computed(() => [
  { type: 'AES 数据密钥', fingerprint: status.value?.aesFingerprint || '-', source: status.value?.aesSource || '-' },
  { type: 'OPE 索引密钥', fingerprint: status.value?.opeFingerprint || '-', source: status.value?.opeSource || '-' },
  { type: 'HMAC 完整性密钥', fingerprint: status.value?.integrityFingerprint || '-', source: status.value?.integritySource || '-' }
]);

const loadStatus = async () => {
  const { data } = await fetchKeyStatus();
  status.value = data;
};

const rotateVersion = async () => {
  const { data } = await rotateDemoKeyVersion();
  status.value = data;
  Message.success(`密钥版本已更新为 ${data.activeVersion}`);
};

const repairTags = async () => {
  repairing.value = true;
  try {
    const { data } = await repairIntegrityTags();
    Message.success(`已修复 ${data.repairedRecords} 条完整性标签`);
  } finally {
    repairing.value = false;
  }
};

onMounted(() => {
  void loadStatus();
});
</script>

<style scoped>
.panel-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card,
.key-card {
  border: 1px solid var(--border);
  min-width: 0;
}

.summary-card {
  padding: 16px;
  border-radius: 8px;
  background: #fff;
}

.summary-label {
  font-size: 12px;
  color: var(--fg-2);
}

.summary-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 800;
  color: var(--fg-0);
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 18px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 800;
}

.section-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--fg-2);
}

.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tip {
  margin-top: 14px;
}

@media (max-width: 980px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .section-head {
    flex-direction: column;
  }
}
</style>
