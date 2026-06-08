<script setup lang="ts">
import { ref } from 'vue';

type AmlRule = {
  code: string;
  name: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  threshold: string;
  enabled: boolean;
};

const rules = ref<AmlRule[]>([
  {
    code: 'BLACKLIST_EXPOSURE',
    name: 'Blacklist Exposure',
    severity: 'CRITICAL',
    threshold: 'counterparty tag = blacklist',
    enabled: true,
  },
  {
    code: 'HIGH_FREQUENCY_TRANSFER',
    name: 'High Frequency Transfer',
    severity: 'HIGH',
    threshold: '>= 20 transfers / 30 min',
    enabled: true,
  },
  {
    code: 'NEW_ADDRESS_LARGE_WITHDRAWAL',
    name: 'New Address Large Withdrawal',
    severity: 'HIGH',
    threshold: 'address age < 7d and amount > 10,000 USD',
    enabled: true,
  },
  {
    code: 'MULTI_HOP_OBFUSCATION',
    name: 'Multi-hop Obfuscation',
    severity: 'MEDIUM',
    threshold: '>= 4 hops / 1 hour',
    enabled: false,
  },
]);

function severityTagType(severity: AmlRule['severity']) {
  if (severity === 'CRITICAL') return 'danger';
  if (severity === 'HIGH') return 'warning';
  return 'info';
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <h2>ChainGuard Admin Console</h2>
    </el-header>
    <el-main>
      <el-row :gutter="16">
        <el-col :span="8">
          <el-card>
            <template #header>Enabled Rules</template>
            <div class="metric">{{ rules.filter((rule) => rule.enabled).length }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header>Critical Rules</template>
            <div class="metric critical">{{ rules.filter((rule) => rule.severity === 'CRITICAL').length }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card>
            <template #header>Rule Versions</template>
            <div class="metric">v1</div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span>AML Rule Management</span>
            <el-button type="primary">Create Rule</el-button>
          </div>
        </template>
        <el-table :data="rules" style="width: 100%">
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
          <el-table-column prop="enabled" label="Enabled" width="120">
            <template #default="scope">
              <el-switch v-model="scope.row.enabled" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-main>
  </el-container>
</template>
