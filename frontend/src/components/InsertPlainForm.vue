<template>
  <div class="panel">
    <div>
      <div class="panel-title">明文插入（服务端加密）</div>
      <div class="panel-sub">用于演示服务端接收明文后加密入库的流程。</div>
    </div>

    <Form :model="form" :label-width="88">
      <FormItem label="记录 ID">
        <Input v-model="form.recordId" placeholder="emp-1001，可留空自动生成" />
      </FormItem>
      <FormItem label="姓名">
        <Input v-model="form.name" placeholder="例如 Alice" />
      </FormItem>
      <FormItem label="薪资">
        <Input v-model.number="form.salary" type="number" placeholder="例如 8000" />
      </FormItem>
    </Form>

    <div class="meta">
      <div class="meta-item"><span>表名</span><strong>employees</strong></div>
      <div class="meta-item"><span>索引字段</span><strong>salary</strong></div>
      <div class="meta-item"><span>加密方式</span><strong>AES-GCM</strong></div>
    </div>

    <Button type="primary" @click="submit">插入</Button>
    <Alert v-if="msg" type="success" show-icon>{{ msg }}</Alert>
    <Alert v-if="err" type="error" show-icon>{{ err }}</Alert>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { insertPlain } from '../api/records';

const msg = ref('');
const err = ref('');
const form = reactive({ recordId: '', name: '', salary: 8000 });

const submit = async () => {
  msg.value = '';
  err.value = '';
  const rid = form.recordId?.trim() ? form.recordId.trim() : `emp-${Date.now()}`;
  try {
    await insertPlain({
      table: 'employees',
      recordId: rid,
      fields: [
        { column: 'name', value: form.name, indexed: false },
        { column: 'salary', value: form.salary, indexed: true }
      ]
    });
    form.recordId = rid;
    msg.value = '插入成功，服务端已完成 AES-GCM 加密、索引构建和完整性标签生成。';
  } catch (e: any) {
    err.value = String(e?.response?.data?.message ?? e?.message ?? '插入失败');
  }
};
</script>

<style scoped>
.panel {
  display: grid;
  gap: 14px;
}
.panel-title {
  font-size: 14px;
  font-weight: 800;
}
.panel-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--fg-2);
}
.meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.meta-item {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #f9fbff;
}
.meta-item span {
  display: block;
  font-size: 12px;
  color: var(--fg-2);
}
.meta-item strong {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}
</style>
