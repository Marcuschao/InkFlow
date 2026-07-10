<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">AI 模型管理</h1>
          <p class="ds-page-sub">查看模型列表、健康状态与启停</p>
        </div>
        <n-space :size="8">
          <router-link to="/admin/ai-dashboard"><n-button>AI 监控</n-button></router-link>
          <router-link to="/admin"><n-button>返回管理</n-button></router-link>
        </n-space>
      </header>

      <n-alert v-if="err" type="error">{{ err }}</n-alert>

      <n-card title="模型列表">
        <n-data-table :columns="columns" :data="models" :bordered="false" />
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue';
import { NAlert, NButton, NCard, NDataTable, NSpace, NTag, NSwitch } from 'naive-ui';
import { fetchAiModels, fetchAiModelHealth, updateAiModelEnabled } from '../../api/aiGateway';

const models = ref([]);
const healthMap = ref({});
const err = ref('');

const columns = [
  { title: 'Provider', key: 'providerId' },
  { title: 'Model', key: 'model', render: (r) => r.model || (r.models ? r.models.join(', ') : '—') },
  { title: '优先级', key: 'priority', width: 80 },
  {
    title: '健康',
    key: 'health',
    width: 100,
    render: (r) => {
      const st = healthMap.value[r.providerId] || r.health || 'UNKNOWN';
      const type = st === 'HEALTHY' ? 'success' : st === 'HALF_OPEN' ? 'warning' : 'error';
      return h(NTag, { type, size: 'small' }, () => st);
    },
  },
  {
    title: '启用',
    key: 'enabled',
    width: 80,
    render: (r) => {
      if (!r.id) return '—';
      return h(NSwitch, {
        value: !!r.enabled,
        onUpdateValue: (v) => toggle(r.id, v),
      });
    },
  },
];

async function toggle(id, enabled) {
  try {
    await updateAiModelEnabled(id, enabled);
    await load();
  } catch (e) {
    err.value = e?.message || '更新失败';
  }
}

async function load() {
  const [m, h] = await Promise.all([fetchAiModels(), fetchAiModelHealth()]);
  models.value = m || [];
  const map = {};
  (h || []).forEach((item) => { map[item.providerId] = item.status; });
  healthMap.value = map;
}

onMounted(async () => {
  try {
    await load();
  } catch (e) {
    err.value = e?.message || '加载失败';
  }
});
</script>
