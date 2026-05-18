<template>
  <div class="panel">
    <div>
      <div class="panel-title">密文插入（客户端加密）</div>
      <div class="panel-sub">浏览器端完成字段加密和索引生成，后端只接收密文载荷。</div>
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
      <div class="meta-item"><span>执行位置</span><strong>浏览器端</strong></div>
      <div class="meta-item"><span>索引字段</span><strong>salary</strong></div>
      <div class="meta-item"><span>完整性</span><strong>后端 HMAC</strong></div>
    </div>

    <Button type="primary" @click="submit">加密并插入</Button>
    <Alert v-if="msg" type="success" show-icon>{{ msg }}</Alert>
    <Alert v-if="err" type="error" show-icon>{{ err }}</Alert>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { insertEncrypted } from '../api/recordsEncrypted';
import { encryptFieldClient } from '../crypto/encryptClient';

const msg = ref('');
const err = ref('');
const form = reactive({ recordId: '', name: '', salary: 8000 });

const submit = async () => {
  msg.value = '';
  err.value = '';
  const rid = form.recordId?.trim() ? form.recordId.trim() : `emp-${Date.now()}`;
  try {
    const fields = [
      await encryptFieldClient(rid, 'name', form.name, false),
      await encryptFieldClient(rid, 'salary', form.salary, true)
    ];
    await insertEncrypted({ table: 'employees', recordId: rid, fields });
    form.recordId = rid;
    msg.value = '插入成功，客户端已完成字段加密并提交密文索引。';
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
