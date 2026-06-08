<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';

type AmlRule = {
  id: string;
  code: string;
  name: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  threshold: string;
  enabled: boolean;
  version: number;
  createdAt?: string;
  updatedAt?: string;
};

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

const rules = ref<AmlRule[]>([
  {
    id: 'demo-1',
    code: 'BLACKLIST_EXPOSURE',
    name: 'Blacklist Exposure',
    severity: 'CRITICAL',
    threshold: '{"counterpartyTag":"blacklist"}',
    enabled: true,
    version: 1,
  },
  {
    id: 'demo-2',
    code: 'HIGH_FREQUENCY_TRANSFER',
    name: 'High Frequency Transfer',
    severity: 'HIGH',
    threshold: '{"count":20,"windowMinutes":30}',
    enabled: true,
    version: 1,
  },
  {
    id: 'demo-3',
    code: 'NEW_ADDRESS_LARGE_WITHDRAWAL',
    name: 'New Address Large Withdrawal',
    severity: 'HIGH',
    threshold: '{"addressAgeDays":7,"amountUsd":10000}',
    enabled: true,
    version: 1,
  },
  {
    id: 'demo-4',
    code: 'MULTI_HOP_OBFUSCATION',
    name: 'Multi-hop Obfuscation',
    severity: 'MEDIUM',
    threshold: '{"hops":4,"windowMinutes":60}',
    enabled: false,
    version: 1,
  },
]);
const token = ref<string | null>(null);
const loading = ref(false);
const authStatus = computed(() => token.value ? 'Admin authenticated' : 'Offline demo data');
const enabledCount = computed(() => rules.value.filter((rule) => rule.enabled).length);
const criticalCount = computed(() => rules.value.filter((rule) => rule.severity === 'CRITICAL').length);
const maxVersion = computed(() => Math.max(...rules.value.map((rule) => rule.version), 1));

function severityTagType(severity: AmlRule['severity']) {
  if (severity === 'CRITICAL') return 'danger';
  if (severity === 'HIGH') return 'warning';
  return 'info';
}

async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  if (token.value) {
    headers.set('Authorization', `Bearer ${token.value}`);
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`API ${path} failed with status ${response.status}`);
  }

  return response.json();
}

async function login() {
  const response = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin@chainguard.demo', password: 'demo-password' }),
  });

  if (!response.ok) {
    throw new Error('Login failed');
  }

  const body = await response.json();
  token.value = body.accessToken;
}

async function loadRules() {
  loading.value = true;
  try {
    if (!token.value) {
      await login();
    }
    rules.value = await apiRequest<AmlRule[]>('/api/rules');
    ElMessage.success('AML rules loaded from backend');
  } catch {
    ElMessage.warning('Backend unavailable. Showing built-in demo rules.');
  } finally {
    loading.value = false;
  }
}

async function toggleRule(rule: AmlRule) {
  if (!token.value || rule.id.startsWith('demo-')) {
    ElMessage.info('Offline demo mode: switch changed locally only.');
    return;
  }

  try {
    const updated = await apiRequest<AmlRule>(`/api/rules/${rule.id}/enabled`, {
      method: 'PATCH',
      body: JSON.stringify({ enabled: rule.enabled }),
    });
    const index = rules.value.findIndex((item) => item.id === updated.id);
    if (index >= 0) {
      rules.value[index] = updated;
    }
    ElMessage.success(`${updated.code} updated to v${updated.version}`);
  } catch {
    rule.enabled = !rule.enabled;
    ElMessage.error('Failed to update rule. Change reverted.');
  }
}

onMounted(loadRules);
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <h2>ChainGuard Admin Console</h2>
    </el-header>
    <el-main>
      <el-alert
        :title="authStatus"
        :description="`API Base: ${API_BASE}`"
        :type="token ? 'success' : 'warning'"
        show-icon
        class="status-alert"
      />

      <el-row :gutter="16">
        <el-col :span="8">
          <el-card>
            <template #header>Enabled Rules</template>
            <div class="metric">{{ enabledCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header>Critical Rules</template>
            <div class="metric critical">{{ criticalCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header>Max Rule Version</template>
            <div class="metric">v{{ maxVersion }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>AML Rule Management</span>
            <el-button type="primary" :loading="loading" @click="loadRules">Refresh Rules</el-button>
          </div>
        </template>
        <el-table :data="rules" style="width: 100%" v-loading="loading">
          <el-table-column prop="code" label="Code" width="260" />
          <el-table-column prop="name" label="Name" />
          <el-table-column prop="severity" label="Severity" width="140">
            <template #default="scope">
              <el-tag :type="severityTagType(scope.row.severity)">
                {{ scope.row.severity }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="threshold" label="Threshold" />
          <el-table-column prop="version" label="Version" width="100">
            <template #default="scope">v{{ scope.row.version }}</template>
          </el-table-column>
          <el-table-column prop="enabled" label="Enabled" width="120">
            <template #default="scope">
              <el-switch v-model="scope.row.enabled" @change="toggleRule(scope.row)" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>
  </el-container>
</template>
