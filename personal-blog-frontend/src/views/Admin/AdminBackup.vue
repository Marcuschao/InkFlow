<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">数据库备份</h1>
          <p class="ds-page-sub">mysqldump 归档至 MinIO blog-backups</p>
        </div>
        <router-link to="/admin"><n-button>返回管理</n-button></router-link>
      </header>

      <n-alert type="info" style="margin-bottom: 16px;">
        每日 03:00 自动备份；本地保留 30 天策略由后端执行。
      </n-alert>

      <n-card :bordered="true" style="max-width: 36rem;">
        <n-descriptions :column="1" label-placement="left">
          <n-descriptions-item label="上次备份">{{ status.lastTime || '—' }}</n-descriptions-item>
          <n-descriptions-item label="文件大小">{{ formatSize(status.lastSize) }}</n-descriptions-item>
          <n-descriptions-item label="对象键">{{ status.objectKey || '—' }}</n-descriptions-item>
          <n-descriptions-item label="说明">{{ status.message || '—' }}</n-descriptions-item>
        </n-descriptions>

        <n-alert v-if="err" type="error" style="margin-top: 16px;">{{ err }}</n-alert>
        <n-alert v-if="msg" type="success" style="margin-top: 16px;">{{ msg }}</n-alert>

        <n-button type="primary" :loading="busy" style="margin-top: 16px;" @click="runBackup">
          立即备份
        </n-button>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { NAlert, NButton, NCard, NDescriptions, NDescriptionsItem } from 'naive-ui';
import { fetchBackupStatus, triggerBackup } from '../../api/backup';

const status = ref({});
const busy = ref(false);
const err = ref('');
const msg = ref('');

function formatSize(n) {
  if (n == null) return '—';
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / 1024 / 1024).toFixed(2)} MB`;
}

async function loadStatus() {
  err.value = '';
  try {
    status.value = (await fetchBackupStatus()) || {};
  } catch (e) {
    err.value = e?.message || '加载失败';
  }
}

async function runBackup() {
  busy.value = true;
  err.value = '';
  msg.value = '';
  try {
    status.value = (await triggerBackup()) || {};
    msg.value = '备份已触发';
  } catch (e) {
    err.value = e?.message || '备份失败';
  } finally {
    busy.value = false;
  }
}

onMounted(loadStatus);
</script>
