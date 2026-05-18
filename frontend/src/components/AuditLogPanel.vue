<template>
  <div class="panel-page">
    <div class="summary-grid">
      <div class="summary-card">
        <div class="summary-label">日志条数</div>
        <div class="summary-value">{{ filteredLogs.length }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最近动作</div>
        <div class="summary-value">{{ filteredLogs[0]?.actionType || '暂无' }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最近状态</div>
        <div class="summary-value">{{ filteredLogs[0]?.status || '暂无' }}</div>
      </div>
    </div>

    <Card class="audit-card" dis-hover>
      <div class="section-head">
        <div>
          <div class="section-title">审计日志</div>
          <div class="section-sub">记录登录、录入、查询、策略变更、密钥版本变更和解密查看行为。</div>
        </div>
        <div class="head-actions">
          <Select v-model="actionFilter" class="filter-select">
            <Option value="ALL">全部动作</Option>
            <Option v-for="action in actionTypes" :key="action" :value="action">{{ action }}</Option>
          </Select>
          <InputNumber v-model="limit" :min="5" :max="100" :step="5" />
          <Button @click="loadLogs">刷新日志</Button>
        </div>
      </div>

      <div class="table-scroll-shell">
        <Table class="wide-table" :columns="columns" :data="tableData" border />
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchAuditLogs, type AuditLogItem } from '../api/admin';

const logs = ref<AuditLogItem[]>([]);
const limit = ref(20);
const actionFilter = ref('ALL');

const actionTypes = [
  'LOGIN',
  'INSERT_ENCRYPTED',
  'INSERT_PLAIN',
  'SQL_TRANSLATE',
  'RANGE_QUERY',
  'SQL_IMPORT',
  'DECRYPT_VIEW',
  'OPE_POLICY_UPDATE',
  'EAFS_REBUILD',
  'KEY_ROTATE_DEMO'
];

const loadLogs = async () => {
  const { data } = await fetchAuditLogs(limit.value);
  logs.value = data;
};

const shortText = (text?: string | null, max = 84) => {
  if (!text) return '-';
  return text.length > max ? `${text.slice(0, max)}...` : text;
};

const filteredLogs = computed(() =>
  actionFilter.value === 'ALL' ? logs.value : logs.value.filter((log) => log.actionType === actionFilter.value)
);

const columns = [
  { title: '时间', key: 'createdAt', minWidth: 170 },
  { title: '动作类型', key: 'actionType', minWidth: 150 },
  { title: '状态', key: 'status', width: 90 },
  { title: '表名', key: 'tableName', width: 110 },
  { title: '字段', key: 'columnName', width: 110 },
  { title: '索引区间', key: 'indexRange', minWidth: 180 },
  { title: '数量', key: 'hitCount', width: 90 },
  { title: '耗时(ms)', key: 'elapsedMs', width: 110 },
  { title: 'SQL / 说明', key: 'detail', minWidth: 360 }
];

const tableData = computed(() =>
  filteredLogs.value.map((log) => ({
    createdAt: log.createdAt?.replace('T', ' ').slice(0, 19) ?? '-',
    actionType: log.actionType,
    status: log.status,
    tableName: log.tableName || '-',
    columnName: log.columnName || '-',
    indexRange: log.lowerIndex != null && log.upperIndex != null ? `${log.lowerIndex} ~ ${log.upperIndex}` : '-',
    hitCount: log.hitCount ?? '-',
    elapsedMs: log.elapsedMs ?? '-',
    detail: shortText(log.sqlText || log.detailText)
  }))
);

onMounted(() => {
  void loadLogs();
});
</script>

<style scoped>
.panel-page {
  display: grid;
  gap: 16px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}
.summary-card,
.audit-card {
  border: 1px solid var(--border);
  min-width: 0;
  max-width: 100%;
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
}
.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
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
  justify-content: flex-end;
}
.filter-select {
  width: 180px;
}
.table-scroll-shell {
  margin-top: 16px;
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
}
:deep(.wide-table) {
  min-width: 1260px;
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
