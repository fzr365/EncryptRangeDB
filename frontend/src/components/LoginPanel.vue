<template>
  <div class="login-shell">
    <div class="login-visual" aria-hidden="true">
      <div class="visual-badge">OPE + EAFS</div>
      <div class="visual-title">Encrypted Range Query</div>
      <div class="visual-grid">
        <div v-for="item in visualNodes" :key="item" class="visual-node">{{ item }}</div>
      </div>
      <div class="visual-panel">
        <span>cipher_blob</span>
        <strong>HMAC-SHA256 verified</strong>
      </div>
    </div>

    <Card class="login-card" :bordered="false" dis-hover>
      <div class="login-title">EncryptRangeDB</div>
      <div class="login-sub">支持密文范围查询的安全数据库管理系统</div>

      <div class="account-picker">
        <button
          v-for="account in accounts"
          :key="account.username"
          type="button"
          class="account-chip"
          :class="{ 'account-chip--active': form.username === account.username }"
          @click="fill(account.username, account.password)"
        >
          <span>{{ account.label }}</span>
          <small>{{ account.role }}</small>
        </button>
      </div>

      <Form :model="form" :label-width="72" class="login-form">
        <FormItem label="用户名">
          <Input v-model="form.username" placeholder="admin / user / audit" @keyup.enter="submit" />
        </FormItem>
        <FormItem label="密码">
          <Input v-model="form.password" type="password" password placeholder="请输入密码" @keyup.enter="submit" />
        </FormItem>
        <FormItem label="验证码">
          <div class="captcha-row">
            <Input v-model="captchaInput" placeholder="输入图片中的结果" @keyup.enter="submit" />
            <button class="captcha-image" type="button" title="点击刷新验证码" @click="refreshCaptcha">
              <img :src="captchaSrc" alt="算术验证码" />
            </button>
          </div>
        </FormItem>
      </Form>

      <Button type="primary" long :loading="loading" @click="submit">登录系统</Button>
      <Alert v-if="err" type="error" show-icon class="login-alert">{{ err }}</Alert>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { login, type LoginResponse } from '../api/auth';

const emit = defineEmits<{ loggedIn: [LoginResponse] }>();

const accounts = [
  { label: '管理员', role: 'ADMIN', username: 'admin', password: 'admin123' },
  { label: '普通用户', role: 'USER', username: 'user', password: 'user123' },
  { label: '审计员', role: 'AUDITOR', username: 'audit', password: 'audit123' }
];

const visualNodes = ['AES-GCM', 'RIndex', 'Anchor', 'Chain', 'Audit', 'Key v1'];
const loading = ref(false);
const err = ref('');
const captchaInput = ref('');
const captcha = reactive({ expression: '', answer: 0, seed: 0 });
const form = reactive({ username: 'admin', password: 'admin123' });

const fill = (username: string, password: string) => {
  form.username = username;
  form.password = password;
  err.value = '';
};

const refreshCaptcha = () => {
  const a = 2 + Math.floor(Math.random() * 8);
  const b = 1 + Math.floor(Math.random() * 9);
  const op = ['+', '-', '×'][Math.floor(Math.random() * 3)];
  captcha.expression = `${a} ${op} ${b} = ?`;
  captcha.answer = op === '+' ? a + b : op === '-' ? a - b : a * b;
  captcha.seed = Math.random();
  captchaInput.value = '';
};

const captchaSrc = computed(() => {
  const lines = Array.from({ length: 5 }, (_, i) => {
    const x1 = Math.round(((captcha.seed * 997 + i * 31) % 120));
    const y1 = Math.round(((captcha.seed * 613 + i * 19) % 44));
    const x2 = Math.round(((captcha.seed * 431 + i * 47) % 120));
    const y2 = Math.round(((captcha.seed * 281 + i * 23) % 44));
    return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="rgba(22,93,255,.22)" stroke-width="1"/>`;
  }).join('');
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="124" height="46" viewBox="0 0 124 46">
      <defs>
        <linearGradient id="g" x1="0" x2="1">
          <stop offset="0" stop-color="#eef6ff"/>
          <stop offset="1" stop-color="#d8fff8"/>
        </linearGradient>
      </defs>
      <rect width="124" height="46" rx="8" fill="url(#g)"/>
      ${lines}
      <text x="18" y="30" font-family="Consolas, monospace" font-size="22" font-weight="700" fill="#16324f"
        transform="rotate(-2 62 23)">${captcha.expression}</text>
    </svg>`;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
});

const submit = async () => {
  err.value = '';
  if (Number(captchaInput.value.trim()) !== captcha.answer) {
    err.value = '验证码结果不正确';
    refreshCaptcha();
    return;
  }
  loading.value = true;
  try {
    const { data } = await login({ username: form.username.trim(), password: form.password });
    localStorage.setItem('erdb_token', data.token);
    localStorage.setItem('erdb_user', JSON.stringify({ username: data.username, role: data.role }));
    emit('loggedIn', data);
  } catch (e: any) {
    err.value = String(e?.response?.data?.message ?? e?.message ?? '登录失败');
    refreshCaptcha();
  } finally {
    loading.value = false;
  }
};

refreshCaptcha();
</script>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(420px, 520px);
  align-items: center;
  gap: 34px;
  padding: 48px clamp(28px, 7vw, 96px);
  background:
    linear-gradient(120deg, rgba(7, 28, 58, 0.68), rgba(7, 53, 82, 0.22)),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='1600' height='1000' viewBox='0 0 1600 1000'%3E%3Cdefs%3E%3ClinearGradient id='bg' x1='0' x2='1' y1='0' y2='1'%3E%3Cstop stop-color='%230d2340'/%3E%3Cstop offset='.55' stop-color='%23165dff'/%3E%3Cstop offset='1' stop-color='%2314c9c9'/%3E%3C/linearGradient%3E%3C/defs%3E%3Crect width='1600' height='1000' fill='url(%23bg)'/%3E%3Cg fill='none' stroke='rgba(255,255,255,.28)'%3E%3Cpath d='M110 780 C330 560 500 640 720 410 S1120 200 1490 310' stroke-width='3'/%3E%3Cpath d='M180 210 L420 310 L620 250 L860 390 L1110 330 L1390 470' stroke-width='2'/%3E%3Cpath d='M260 850 L520 720 L760 790 L1040 610 L1330 670' stroke-width='2'/%3E%3C/g%3E%3Cg fill='rgba(255,255,255,.18)'%3E%3Ccircle cx='420' cy='310' r='12'/%3E%3Ccircle cx='860' cy='390' r='16'/%3E%3Ccircle cx='1110' cy='330' r='11'/%3E%3Ccircle cx='720' cy='410' r='14'/%3E%3Ccircle cx='1040' cy='610' r='13'/%3E%3C/g%3E%3Cg opacity='.22'%3E%3Crect x='120' y='110' width='260' height='120' rx='16' fill='white'/%3E%3Crect x='1020' y='130' width='330' height='130' rx='18' fill='white'/%3E%3Crect x='1040' y='740' width='300' height='120' rx='18' fill='white'/%3E%3C/g%3E%3C/svg%3E") center/cover no-repeat;
}

.login-visual {
  color: #fff;
  max-width: 560px;
}

.visual-badge {
  display: inline-flex;
  padding: 8px 12px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-weight: 800;
}

.visual-title {
  margin-top: 18px;
  font-size: 48px;
  line-height: 1.05;
  font-weight: 850;
}

.visual-grid {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.visual-node,
.visual-panel {
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  border-radius: 8px;
}

.visual-node {
  padding: 14px;
  font-weight: 800;
}

.visual-panel {
  margin-top: 14px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.login-card {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.45);
  box-shadow: 0 24px 60px rgba(4, 19, 38, 0.24);
}

.login-title {
  font-size: 32px;
  font-weight: 850;
  color: var(--fg-0);
}

.login-sub {
  margin-top: 6px;
  color: var(--fg-1);
  font-size: 14px;
}

.account-picker {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.account-chip {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 8px;
  padding: 10px 8px;
  cursor: pointer;
  color: var(--fg-1);
  display: grid;
  gap: 3px;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.account-chip span {
  font-weight: 800;
}

.account-chip small {
  font-size: 11px;
}

.account-chip--active {
  border-color: var(--primary);
  background: var(--primary);
  color: #fff;
  transform: translateY(-1px);
}

.login-form {
  margin-top: 18px;
}

.captcha-row {
  display: grid;
  grid-template-columns: 1fr 124px;
  gap: 10px;
}

.captcha-image {
  height: 40px;
  padding: 0;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
}

.captcha-image img {
  width: 124px;
  height: 46px;
  display: block;
  transform: translateY(-3px);
}

.login-alert {
  margin-top: 12px;
}

@media (max-width: 960px) {
  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-visual {
    display: none;
  }
}
</style>
