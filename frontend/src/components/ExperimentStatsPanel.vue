<template>
  <div class="panel-page">
    <div class="summary-grid">
      <div class="summary-card"><div class="summary-label">密文记录总数</div><div class="summary-value">{{ stats?.totalRecords ?? 0 }}</div></div>
      <div class="summary-card"><div class="summary-label">索引记录总数</div><div class="summary-value">{{ stats?.totalIndexedRows ?? 0 }}</div></div>
      <div class="summary-card"><div class="summary-label">链节点总数</div><div class="summary-value">{{ stats?.totalChainNodes ?? 0 }}</div></div>
      <div class="summary-card"><div class="summary-label">审计日志总数</div><div class="summary-value">{{ stats?.totalAuditLogs ?? 0 }}</div></div>
    </div>

    <div class="metrics-grid">
      <Card class="metric-card" dis-hover>
        <div class="metric-title">查询效率指标</div>
        <div class="metric-list">
          <div class="metric-item"><span>平均范围查询耗时</span><strong>{{ stats?.avgQueryLatencyMs ?? 0 }} ms</strong></div>
          <div class="metric-item"><span>最近命中条数</span><strong>{{ stats?.latestQueryHitCount ?? 0 }}</strong></div>
          <div class="metric-item"><span>最近索引跨度</span><strong>{{ stats?.latestRangeSpan ?? 0 }}</strong></div>
        </div>
      </Card>

      <Card class="metric-card" dis-hover>
        <div class="metric-title">当前 OPE 策略摘要</div>
        <div class="metric-list">
          <div class="metric-item"><span>策略名称</span><strong>{{ stats?.activePolicyName || 'default-policy' }}</strong></div>
          <div class="metric-item"><span>噪声灵敏度</span><strong>{{ stats?.activePolicySensitivity ?? 0 }}</strong></div>
          <div class="metric-item"><span>分段数量</span><strong>{{ stats?.activePolicySegments ?? 0 }}</strong></div>
        </div>
      </Card>

      <Card class="metric-card" dis-hover>
        <div class="metric-title">Ordered EAFS 维护</div>
        <div class="metric-list">
          <div class="metric-item metric-item--stack"><span>默认桶</span><strong>{{ defaultBucket.table }}:{{ defaultBucket.column }}</strong></div>
          <div class="metric-item metric-item--stack"><span>最近重建</span><strong>{{ rebuildSummary }}</strong></div>
        </div>
        <div class="rebuild-actions">
          <Button :loading="rebuildingSingle" @click="rebuildSingleBucket">重建默认桶</Button>
          <Button type="primary" :loading="rebuildingAll" @click="rebuildAllBuckets">重建全部桶</Button>
        </div>
      </Card>
    </div>

    <Card class="chart-card" dis-hover>
      <div class="section-head">
        <div>
          <div class="section-title">实验统计面板</div>
          <div class="section-sub">用于观察系统规模、查询行为和 ordered EAFS 当前维护状态。</div>
        </div>
        <Button @click="loadStats">刷新统计</Button>
      </div>
      <div class="bar-list">
        <div class="bar-item" v-for="item in bars" :key="item.label">
          <div class="bar-row"><div>{{ item.label }}</div><div>{{ item.value }} / {{ item.total }}</div></div>
          <div class="bar-track"><div class="bar-fill" :style="{ width: percent(item.value, item.total) }"></div></div>
        </div>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { Message } from 'view-ui-plus';
import { fetchExperimentStats, rebuildEafs, type ExperimentStats } from '../api/admin';

const stats = ref<ExperimentStats | null>(null);
const rebuildingSingle = ref(false);
const rebuildingAll = ref(false);
const rebuildSummary = ref('尚未执行');
const defaultBucket = { table: 'employees', column: 'salary' };

const loadStats = async () => {
  const { data } = await fetchExperimentStats();
  stats.value = data;
};

const rebuildSingleBucket = async () => {
  rebuildingSingle.value = true;
  try {
    const { data } = await rebuildEafs({ table: defaultBucket.table, column: defaultBucket.column, rebuildAll: false });
    rebuildSummary.value = `${data.rebuiltBuckets} 个桶：${data.buckets.join(', ')}`;
    Message.success('默认 EAFS 桶已重建');
    await loadStats();
  } finally {
    rebuildingSingle.value = false;
  }
};

const rebuildAllBuckets = async () => {
  rebuildingAll.value = true;
  try {
    const { data } = await rebuildEafs({ rebuildAll: true });
    rebuildSummary.value = `${data.rebuiltBuckets} 个桶：${data.buckets.join(', ') || '无'}`;
    Message.success('EAFS 全量重建完成');
    await loadStats();
  } finally {
    rebuildingAll.value = false;
  }
};

const bars = computed(() => [
  { label: '密文记录索引覆盖率', value: stats.value?.totalIndexedRows ?? 0, total: stats.value?.totalRecords ?? 0 },
  { label: '链节点覆盖率', value: stats.value?.totalChainNodes ?? 0, total: stats.value?.totalRecords ?? 0 },
  { label: '审计覆盖规模', value: stats.value?.totalAuditLogs ?? 0, total: Math.max(stats.value?.totalRecords ?? 0, 1) }
]);

const percent = (value?: number, total?: number) => {
  if (!value || !total) return '0%';
  return `${Math.min(100, Math.max(8, Math.round((value / total) * 100)))}%`;
};

onMounted(() => {
  void loadStats();
});
</script>

<style scoped>
.panel-page {
  display: grid;
  gap: 16px;
}
.summary-grid,
.metrics-grid {
  display: grid;
  gap: 12px;
}
.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
.metrics-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.summary-card,
.metric-card,
.chart-card {
  border: 1px solid var(--border);
}
.summary-card {
  padding: 16px;
  border-radius: 8px;
  background: #fff;
}
.summary-label,
.section-sub,
.metric-item span {
  font-size: 12px;
  color: var(--fg-2);
}
.summary-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
}
.metric-title,
.section-title {
  font-size: 15px;
  font-weight: 800;
}
.metric-list,
.bar-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}
.metric-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fbff;
}
.metric-item--stack {
  flex-direction: column;
}
.rebuild-actions,
.section-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
.rebuild-actions {
  margin-top: 14px;
}
.bar-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.bar-track {
  height: 12px;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #165dff 0%, #14c9c9 100%);
}
@media (max-width: 980px) {
  .summary-grid,
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
