<template>
  <LoginPanel v-if="!currentUser" @logged-in="onLoggedIn" />

  <div v-else class="app-shell">
    <div class="app-header">
      <div class="header-left">
        <div class="brand-badge">E</div>
        <div>
          <div class="brand-kicker">Secure Encrypted Database System</div>
          <div class="brand-title">EncryptRangeDB</div>
          <div class="brand-sub">支持密文范围查询的安全数据库管理系统</div>
        </div>
      </div>

      <div class="header-right">
        <div class="status-chip">
          <span class="status-label">当前用户</span>
          <span class="status-value">{{ currentUser.username }} / {{ roleName }}</span>
        </div>
        <div class="status-chip">
          <span class="status-label">查询能力</span>
          <span class="status-value">OPE + EAFS</span>
        </div>
        <Button @click="handleLogout">退出</Button>
      </div>
    </div>

    <Layout class="app-layout">
      <Layout>
        <Sider class="app-sider" hide-trigger :width="240">
          <div class="sider-section">
            <div class="sider-title">功能导航</div>
            <Menu class="sider-menu" :active-name="tab" @on-select="onSelect">
              <MenuItem v-for="item in visibleTabs" :key="item.name" :name="item.name">
                <Icon :type="item.icon" />
                {{ item.label }}
              </MenuItem>
            </Menu>
          </div>
        </Sider>

        <Content class="app-content">
          <div class="content-inner">
            <Card class="page-card" :bordered="false" dis-hover>
              <div class="page-head">
                <div>
                  <div class="page-title">{{ currentPage.title }}</div>
                  <div class="page-sub">{{ currentPage.subtitle }}</div>
                </div>
                <div class="page-tags">
                  <Tag color="blue">Spring Boot 3</Tag>
                  <Tag color="cyan">Vue 3</Tag>
                  <Tag color="green">MySQL 8</Tag>
                </div>
              </div>

              <div class="page-body">
                <Card v-if="tab === 'insert'" class="sub-card insert-single" dis-hover>
                  <div class="sub-title">客户端加密录入</div>
                  <div class="sub-sub">浏览器端先完成字段加密，再上传密文、保序索引和链式索引数据。</div>
                  <div class="sub-body">
                    <InsertClientForm />
                  </div>
                </Card>
                <SqlImportPanel v-else-if="tab === 'import'" />
                <SqlConsole v-else-if="tab === 'query'" />
                <ResultsTable v-else-if="tab === 'results'" />
                <ExperimentStatsPanel v-else-if="tab === 'stats'" />
                <OpePolicyPanel v-else-if="tab === 'policy'" />
                <KeyManagementPanel v-else-if="tab === 'keys'" />
                <AuditLogPanel v-else />
              </div>

              <div class="page-foot">
                <div class="foot-title">安全机制概览</div>
                <div class="foot-grid">
                  <div class="foot-item">
                    <div class="foot-k">身份认证</div>
                    <div class="foot-v">登录后签发令牌，后端根据角色控制接口访问。</div>
                  </div>
                  <div class="foot-item">
                    <div class="foot-k">密文存储</div>
                    <div class="foot-v">业务字段使用 AES-GCM 保存为密文载荷。</div>
                  </div>
                  <div class="foot-item">
                    <div class="foot-k">范围查询</div>
                    <div class="foot-v">数值字段映射为可比较索引，并通过 EAFS 链式索引扫描。</div>
                  </div>
                  <div class="foot-item">
                    <div class="foot-k">完整性校验</div>
                    <div class="foot-v">记录写入时生成 HMAC-SHA256 标签，读取时校验密文是否被篡改。</div>
                  </div>
                  <div class="foot-item">
                    <div class="foot-k">审计日志</div>
                    <div class="foot-v">记录登录、录入、查询、策略变更、密钥版本变更和解密查看行为。</div>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </Content>
      </Layout>
    </Layout>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { logout, type CurrentUser, type LoginResponse } from './api/auth';
import LoginPanel from './components/LoginPanel.vue';
import SqlConsole from './components/SqlConsole.vue';
import ResultsTable from './components/ResultsTable.vue';
import InsertClientForm from './components/InsertClientForm.vue';
import SqlImportPanel from './components/SqlImportPanel.vue';
import ExperimentStatsPanel from './components/ExperimentStatsPanel.vue';
import OpePolicyPanel from './components/OpePolicyPanel.vue';
import AuditLogPanel from './components/AuditLogPanel.vue';
import KeyManagementPanel from './components/KeyManagementPanel.vue';

type AppTab = 'insert' | 'query' | 'results' | 'stats' | 'policy' | 'audit' | 'import' | 'keys';

const storedUser = localStorage.getItem('erdb_user');
const currentUser = ref<CurrentUser | null>(storedUser ? JSON.parse(storedUser) : null);
const tab = ref<AppTab>(currentUser.value?.role === 'AUDITOR' ? 'audit' : 'insert');

const allTabs: Array<{ name: AppTab; label: string; icon: string; roles: string[] }> = [
  { name: 'insert', label: '数据录入', icon: 'md-create', roles: ['ADMIN', 'USER'] },
  { name: 'import', label: 'SQL 文件导入', icon: 'md-cloud-upload', roles: ['ADMIN', 'USER'] },
  { name: 'query', label: '范围查询', icon: 'md-search', roles: ['ADMIN', 'USER'] },
  { name: 'results', label: '加密存储预览', icon: 'md-list-box', roles: ['ADMIN', 'USER'] },
  { name: 'stats', label: '实验统计面板', icon: 'md-podium', roles: ['ADMIN'] },
  { name: 'policy', label: 'OPE 策略配置', icon: 'md-options', roles: ['ADMIN'] },
  { name: 'keys', label: '密钥管理', icon: 'md-key', roles: ['ADMIN'] },
  { name: 'audit', label: '审计日志', icon: 'md-document', roles: ['ADMIN', 'AUDITOR'] }
];

const pageMap: Record<AppTab, { title: string; subtitle: string }> = {
  insert: { title: '数据录入', subtitle: '客户端先完成加密与索引构建，再写入密文记录。' },
  import: { title: 'SQL 文件导入', subtitle: '批量解析 INSERT 语句，自动完成加密存储和索引构建。' },
  query: { title: '范围查询', subtitle: '解析 SQL 条件并重写为密文索引区间查询。' },
  results: { title: '加密存储预览', subtitle: '查看密文记录、链式索引、完整性校验状态和本地解密结果。' },
  stats: { title: '实验统计面板', subtitle: '汇总记录规模、查询效率、链式索引覆盖情况和审计规模。' },
  policy: { title: 'OPE 策略配置', subtitle: '配置分段线性参数与噪声灵敏度，展示可编程 OPE 机制。' },
  keys: { title: '密钥管理', subtitle: '展示密钥版本、密钥来源和密钥指纹，支持演示轮换。' },
  audit: { title: '审计日志', subtitle: '追踪登录、录入、查询、密钥、策略和解密查看行为。' }
};

const visibleTabs = computed(() => allTabs.filter((item) => currentUser.value && item.roles.includes(currentUser.value.role)));
const currentPage = computed(() => pageMap[tab.value]);
const roleName = computed(() => ({ ADMIN: '管理员', USER: '普通用户', AUDITOR: '审计员' }[currentUser.value?.role || 'USER']));

const onLoggedIn = (payload: LoginResponse) => {
  currentUser.value = { username: payload.username, role: payload.role };
  tab.value = payload.role === 'AUDITOR' ? 'audit' : 'insert';
};

const onSelect = (name: string) => {
  if (name in pageMap) {
    tab.value = name as AppTab;
  }
};

const handleLogout = async () => {
  try {
    await logout();
  } finally {
    localStorage.removeItem('erdb_token');
    localStorage.removeItem('erdb_user');
    currentUser.value = null;
  }
};
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-layout {
  min-height: 100vh;
  background: var(--bg-1);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  min-height: 78px;
  padding: 0 22px;
  background: linear-gradient(92deg, #165dff 0%, #2f6fff 48%, #14c9c9 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.14);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-badge {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 18px;
}

.brand-kicker {
  font-size: 10px;
  letter-spacing: 1.1px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.brand-title {
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
}

.brand-sub {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.86);
}

.status-chip {
  min-width: 120px;
  padding: 9px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  line-height: 1.1;
}

.status-label {
  display: block;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.72);
}

.status-value {
  display: block;
  margin-top: 5px;
  font-size: 13px;
  font-weight: 800;
}

.app-sider {
  background: #fff;
  border-right: 1px solid var(--border);
  min-height: calc(100vh - 78px);
}

.sider-section {
  padding: 14px 10px;
}

.sider-title {
  padding: 6px 10px 10px;
  font-size: 12px;
  font-weight: 800;
  color: var(--fg-2);
}

.sider-menu {
  width: 100%;
}

.app-content {
  padding: 18px;
  min-width: 0;
  overflow-x: hidden;
  max-width: calc(100vw - 240px);
}

.content-inner {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  min-width: 0;
}

.page-card {
  border-radius: 8px;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.page-body {
  min-width: 0;
  max-width: 100%;
}

.page-head {
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 800;
}

.page-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--fg-1);
}

.page-tags {
  display: flex;
  gap: 8px;
}

.insert-single {
  max-width: 720px;
  border: 1px solid var(--border);
}

.sub-title {
  font-size: 16px;
  font-weight: 800;
}

.sub-sub {
  margin-top: 6px;
  font-size: 12px;
  color: var(--fg-2);
}

.sub-body {
  margin-top: 12px;
}

.page-foot {
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px solid var(--border);
}

.foot-title {
  font-size: 12px;
  font-weight: 800;
  color: var(--fg-2);
  margin-bottom: 10px;
}

.foot-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.foot-item {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  background: var(--bg-0);
}

.foot-k {
  font-size: 12px;
  font-weight: 800;
}

.foot-v {
  margin-top: 6px;
  font-size: 12px;
  color: var(--fg-1);
  line-height: 1.45;
}

@media (max-width: 980px) {
  .header-right,
  .page-tags {
    display: none;
  }

  .brand-title {
    font-size: 22px;
  }

  .foot-grid {
    grid-template-columns: 1fr;
  }

  .app-content {
    max-width: 100vw;
  }
}
</style>
