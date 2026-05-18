<template>
  <div class="import-page">
    <div class="stats">
      <div class="stat-card"><div class="stat-k">导入模式</div><div class="stat-v">批量 SQL</div></div>
      <div class="stat-card"><div class="stat-k">支持语句</div><div class="stat-v">INSERT</div></div>
      <div class="stat-card"><div class="stat-k">自动处理</div><div class="stat-v">加密 + 建索引</div></div>
    </div>

    <Card class="import-card" dis-hover>
      <div class="section-title">SQL 文件导入</div>
      <div class="section-sub">上传包含 INSERT 的 .sql 文件，系统会自动解析、加密存储、构建索引并记录审计日志。</div>
      <Upload :before-upload="beforeUpload" action="/">
        <Button icon="ios-cloud-upload-outline">选择 .sql 文件</Button>
      </Upload>
      <Alert v-if="msg" type="success" show-icon class="alert">{{ msg }}</Alert>
      <Alert v-if="err" type="error" show-icon class="alert">{{ err }}</Alert>
    </Card>

    <Card class="guide-card" dis-hover>
      <div class="section-title">导入流程</div>
      <div class="guide-grid">
        <div class="guide-item">1. 解析 SQL 文件中的 INSERT 语句</div>
        <div class="guide-item">2. 对普通字段执行 AES-GCM 加密</div>
        <div class="guide-item">3. 对数值字段生成可比较 rindex</div>
        <div class="guide-item">4. 写入 EAFS 链式索引和完整性标签</div>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { importSqlFile } from '../api/sqlImport';

const msg = ref('');
const err = ref('');

const beforeUpload = async (file: File) => {
  msg.value = '';
  err.value = '';
  try {
    const { data } = await importSqlFile(file);
    const errorCount = Array.isArray(data?.errors) ? data.errors.length : 0;
    msg.value = `导入完成：插入 ${data?.insertedRows ?? 0} 行，处理 INSERT ${data?.handledInsertStatements ?? 0} 条，错误 ${errorCount} 条。`;
  } catch (e: any) {
    err.value = String(e?.response?.data?.message ?? e?.message ?? '导入失败');
  }
  return false;
};
</script>

<style scoped>
.import-page {
  display: grid;
  gap: 16px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.stat-card,
.import-card,
.guide-card {
  border: 1px solid var(--border);
}
.stat-card {
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff;
}
.stat-k,
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
.section-sub {
  margin: 6px 0 12px;
}
.guide-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.guide-item {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fbfcfe;
  font-size: 13px;
}
.alert {
  margin-top: 12px;
}
@media (max-width: 980px) {
  .stats,
  .guide-grid {
    grid-template-columns: 1fr;
  }
}
</style>
