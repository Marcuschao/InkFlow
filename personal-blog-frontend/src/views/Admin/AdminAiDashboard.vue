<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">AI 监控</h1>
          <p class="ds-page-sub">调用量、成功率、模型分布与健康状态</p>
        </div>
        <n-space :size="8">
          <router-link to="/admin/ai/models"><n-button>模型管理</n-button></router-link>
          <router-link to="/admin/ai/quota"><n-button>配额管理</n-button></router-link>
          <router-link to="/admin"><n-button>返回管理</n-button></router-link>
        </n-space>
      </header>

      <n-alert v-if="loadErr" type="error">{{ loadErr }}</n-alert>

      <template v-else>
        <n-grid cols="2 m:4" :x-gap="12" :y-gap="12" responsive="screen" class="summary-grid">
          <n-gi v-for="(stat, idx) in statItems" :key="idx">
            <div class="admin-stat-card">
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-value">{{ stat.value }}</div>
            </div>
          </n-gi>
        </n-grid>

        <n-card class="panel admin-panel-gap" title="近 7 天调用趋势">
          <div class="chart-wrap">
            <canvas ref="lineCanvas" height="120" />
          </div>
        </n-card>

        <n-grid :cols="1" :x-gap="16" :y-gap="16" item-responsive class="admin-panel-gap">
          <n-gi span="24 m:12">
            <n-card title="模型使用分布">
              <div class="chart-wrap pie-wrap">
                <canvas ref="pieCanvas" height="160" />
              </div>
            </n-card>
          </n-gi>
          <n-gi span="24 m:12">
            <n-card title="模型健康状态">
              <n-list v-if="healthList.length">
                <n-list-item v-for="h in healthList" :key="h.providerId">
                  <n-space justify="space-between" style="width: 100%">
                    <span>{{ h.providerId }}</span>
                    <n-tag :type="healthTag(h.status)" size="small">{{ h.status }}</n-tag>
                  </n-space>
                </n-list-item>
              </n-list>
              <n-empty v-else description="暂无数据" />
            </n-card>
          </n-gi>
        </n-grid>

        <n-card class="admin-panel-gap" title="最近调用记录">
          <n-data-table :columns="logColumns" :data="logs" :bordered="false" size="small" :loading="logsLoading" />
          <n-empty v-if="!logs.length && !logsLoading" description="暂无记录" />
          <Pagination
            :total="logTotal"
            :page-size="pageSize"
            :current-page="page"
            @changePage="onLogPage"
          />
        </n-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { Chart, registerables } from 'chart.js';
import {
  NAlert, NButton, NCard, NDataTable, NEmpty, NGi, NGrid, NList, NListItem, NSpace, NTag,
} from 'naive-ui';
import {
  fetchAiOverview, fetchAiTrend, fetchAiByModel, fetchAiLogs, fetchAiModelHealth,
} from '../../api/aiGateway';
import Pagination from '../../components/Pagination.vue';
import { formatShortDateTime } from '../../utils/format';

Chart.register(...registerables);

const overview = ref(null);
const trend = ref({ labels: [], calls: [], costs: [], successRates: [] });
const byModel = ref([]);
const healthList = ref([]);
const logs = ref([]);
const logTotal = ref(0);
const page = ref(1);
const pageSize = 20;
const logsLoading = ref(false);
const loadErr = ref('');
const lineCanvas = ref(null);
const pieCanvas = ref(null);
let lineChart = null;
let pieChart = null;

const statItems = computed(() => [
  { label: '今日调用', value: overview.value?.todayCalls ?? '—' },
  { label: '成功率', value: overview.value ? `${overview.value.successRate.toFixed(1)}%` : '—' },
  { label: '平均延迟', value: overview.value ? `${Math.round(overview.value.avgLatencyMs)} ms` : '—' },
  { label: '总花费', value: overview.value ? overview.value.totalCost.toFixed(4) : '—' },
]);

const logColumns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '任务', key: 'taskType', width: 80 },
  { title: '模型', key: 'model', width: 120 },
  { title: 'Token', key: 'tokens', width: 80, render: (r) => (r.inputTokens || 0) + (r.outputTokens || 0) },
  { title: '延迟', key: 'latencyMs', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '时间', key: 'createdAt', width: 160, render: (r) => formatShortDateTime(r.createdAt) },
];

function healthTag(status) {
  if (status === 'HEALTHY') return 'success';
  if (status === 'HALF_OPEN') return 'warning';
  return 'error';
}

function paintLine() {
  const canvas = lineCanvas.value;
  if (!canvas) return;
  if (lineChart) lineChart.destroy();
  lineChart = new Chart(canvas.getContext('2d'), {
    type: 'line',
    data: {
      labels: trend.value.labels || [],
      datasets: [{
        label: '调用量',
        data: (trend.value.calls || []).map(Number),
        borderColor: '#111827',
        backgroundColor: 'rgba(17, 24, 39, 0.08)',
        fill: true,
        tension: 0.25,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } },
    },
  });
}

function paintPie() {
  const canvas = pieCanvas.value;
  if (!canvas) return;
  if (pieChart) pieChart.destroy();
  const labels = byModel.value.map((m) => m.model);
  const data = byModel.value.map((m) => m.count);
  pieChart = new Chart(canvas.getContext('2d'), {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{ data, backgroundColor: ['#111827', '#6b7280', '#9ca3af', '#d1d5db'] }],
    },
    options: { responsive: true, maintainAspectRatio: false },
  });
}

async function loadLogs(p = page.value) {
  logsLoading.value = true;
  page.value = p;
  try {
    const res = await fetchAiLogs(p, pageSize);
    logs.value = res?.records || [];
    logTotal.value = Number(res?.total) || 0;
  } finally {
    logsLoading.value = false;
  }
}

function onLogPage(p) {
  loadLogs(p);
}

onMounted(async () => {
  try {
    const [ov, tr, bm, hl] = await Promise.all([
      fetchAiOverview(),
      fetchAiTrend(7),
      fetchAiByModel(),
      fetchAiModelHealth(),
    ]);
    overview.value = ov;
    trend.value = tr || trend.value;
    byModel.value = bm || [];
    healthList.value = hl || [];
    await loadLogs(1);
    await nextTick();
    paintLine();
    paintPie();
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
  }
});

onUnmounted(() => {
  if (lineChart) lineChart.destroy();
  if (pieChart) pieChart.destroy();
});
</script>

<style scoped>
.summary-grid { margin-bottom: var(--space-4); }
.admin-panel-gap { margin-top: var(--space-4); }
.chart-wrap { height: 220px; padding: var(--space-3); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.pie-wrap { height: 200px; }
</style>
