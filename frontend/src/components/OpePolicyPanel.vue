<template>
  <div class="panel-page">
    <div class="summary-grid">
      <div class="summary-card"><div class="summary-label">当前策略</div><div class="summary-value">{{ form.policyName }}</div></div>
      <div class="summary-card"><div class="summary-label">噪声灵敏度</div><div class="summary-value">{{ form.sensitivity }}</div></div>
      <div class="summary-card"><div class="summary-label">分段数量</div><div class="summary-value">{{ form.segments.length }}</div></div>
    </div>

    <Card class="policy-card" dis-hover>
      <div class="section-head">
        <div>
          <div class="section-title">OPE 策略配置</div>
          <div class="section-sub">配置分段线性参数和噪声灵敏度，用于展示 Programmable OPE 的可调能力。</div>
        </div>
        <div class="head-actions">
          <Button @click="loadPolicy">读取当前策略</Button>
          <Button @click="resetDefault">恢复默认</Button>
          <Button type="primary" @click="submitPolicy">保存并生效</Button>
        </div>
      </div>

      <Form :label-width="96" class="policy-form">
        <FormItem label="策略名称"><Input v-model="form.policyName" placeholder="例如 salary-policy-v2" /></FormItem>
        <FormItem label="灵敏度"><InputNumber v-model="form.sensitivity" :min="0" :step="1" /></FormItem>
      </Form>

      <div class="segment-head">
        <div class="segment-title">分段参数</div>
        <Button @click="addSegment">新增分段</Button>
      </div>

      <div class="segment-list">
        <div v-for="(segment, index) in form.segments" :key="index" class="segment-row">
          <div class="segment-index">#{{ index + 1 }}</div>
          <InputNumber v-model="segment.minValue" :step="1" placeholder="起始值" />
          <InputNumber v-model="segment.a" :min="1" :step="1" placeholder="a" />
          <InputNumber v-model="segment.b" :step="1" placeholder="b" />
          <Input v-model="segment.label" placeholder="标签，可选" />
          <Button type="error" ghost :disabled="form.segments.length === 1" @click="removeSegment(index)">删除</Button>
        </div>
      </div>
    </Card>

    <div class="preview-grid">
      <Card class="formula-card" dis-hover>
        <div class="section-title">当前索引公式</div>
        <div class="section-sub">index = a * value + b + random(0, a * sensitivity)</div>
        <div class="formula-list">
          <div v-for="(segment, index) in sortedSegments" :key="index" class="formula-item">
            <strong>{{ segment.label || `Segment ${index + 1}` }}</strong>
            <span>minValue ≥ {{ segment.minValue }}</span>
            <span>a = {{ segment.a }}</span>
            <span>b = {{ segment.b }}</span>
          </div>
        </div>
      </Card>

      <Card class="preview-card" dis-hover>
        <div class="section-title">示例值预览</div>
        <div class="preview-toolbar">
          <Input v-model="sampleInput" placeholder="例如：60, 75, 90, 120, 500" />
          <Button type="primary" @click="refreshPreview">计算预览</Button>
        </div>
        <div class="preview-list">
          <div v-for="item in previewRows" :key="item.value" class="preview-item">
            <div>明文 {{ item.value }}</div>
            <strong>RIndex ≈ {{ item.index }}</strong>
          </div>
        </div>
      </Card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Message } from 'view-ui-plus';
import { fetchOpePolicy, saveOpePolicy, type OpePolicySegment } from '../api/admin';

const defaultSegments = (): OpePolicySegment[] => [
  { minValue: 0, a: 3, b: 10, label: 'low-range' },
  { minValue: 100, a: 5, b: 20, label: 'mid-range' },
  { minValue: 500, a: 10, b: 30, label: 'high-range' }
];

const form = reactive({ policyName: 'default-policy', sensitivity: 1, segments: defaultSegments() });
const sampleInput = ref('60, 75, 90, 120, 500');

const sortedSegments = computed(() => [...form.segments].sort((a, b) => Number(a.minValue) - Number(b.minValue)));
const previewRows = computed(() => {
  const values = sampleInput.value.split(',').map((item) => Number(item.trim())).filter(Number.isFinite);
  return values.map((value) => {
    const segment = [...sortedSegments.value].reverse().find((candidate) => value >= Number(candidate.minValue)) ?? sortedSegments.value[0];
    return { value, index: Number(segment.a) * value + Number(segment.b) + Number(segment.a) * Number(form.sensitivity) };
  });
});

const loadPolicy = async () => {
  const { data } = await fetchOpePolicy();
  form.policyName = data.policyName || 'default-policy';
  form.sensitivity = data.sensitivity ?? 1;
  form.segments = (data.segments?.length ? data.segments : defaultSegments()).map((segment) => ({
    minValue: Number(segment.minValue ?? 0),
    a: Number(segment.a ?? 1),
    b: Number(segment.b ?? 0),
    label: segment.label || ''
  }));
};

const submitPolicy = async () => {
  await saveOpePolicy({
    policyName: form.policyName,
    sensitivity: Number(form.sensitivity),
    segments: form.segments.map((segment) => ({
      minValue: Number(segment.minValue ?? 0),
      a: Number(segment.a ?? 1),
      b: Number(segment.b ?? 0),
      label: segment.label || ''
    }))
  });
  Message.success('OPE 策略已更新');
  await loadPolicy();
};

const resetDefault = () => {
  form.policyName = 'default-policy';
  form.sensitivity = 1;
  form.segments = defaultSegments();
  Message.info('已恢复默认参数，可继续保存生效');
};

const addSegment = () => {
  form.segments.push({ minValue: form.segments.length * 100, a: 10, b: 0, label: `segment-${form.segments.length + 1}` });
};

const removeSegment = (index: number) => {
  form.segments.splice(index, 1);
};

const refreshPreview = () => {
  sampleInput.value = sampleInput.value.split(',').map((item) => item.trim()).filter(Boolean).join(', ');
};

onMounted(() => {
  void loadPolicy();
});
</script>

<style scoped>
.panel-page {
  display: grid;
  gap: 16px;
}
.summary-grid,
.preview-grid {
  display: grid;
  gap: 12px;
}
.summary-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.preview-grid {
  grid-template-columns: 1.1fr 0.9fr;
}
.summary-card,
.policy-card,
.formula-card,
.preview-card {
  border: 1px solid var(--border);
}
.summary-card {
  padding: 16px;
  border-radius: 8px;
  background: #fff;
}
.summary-label,
.section-sub {
  font-size: 12px;
  color: var(--fg-2);
}
.summary-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 800;
}
.section-head,
.segment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.section-title,
.segment-title {
  font-size: 15px;
  font-weight: 800;
}
.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.policy-form {
  margin-top: 16px;
}
.segment-head {
  margin: 8px 0 12px;
}
.segment-list,
.formula-list,
.preview-list {
  display: grid;
  gap: 10px;
}
.segment-row {
  display: grid;
  grid-template-columns: 64px repeat(4, minmax(0, 1fr)) 88px;
  gap: 10px;
  align-items: center;
}
.formula-item,
.preview-item {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  background: #f8fbff;
}
.preview-toolbar {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  margin: 14px 0;
}
@media (max-width: 980px) {
  .summary-grid,
  .preview-grid,
  .segment-row,
  .preview-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
