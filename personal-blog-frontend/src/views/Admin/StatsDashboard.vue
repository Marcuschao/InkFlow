<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">数据看板</h1>
          <p class="ds-page-sub">PV、热门内容与模型调用概览</p>
        </div>
        <n-space class="dash-actions" :size="12">
          <router-link to="/admin">
            <n-button>文章管理</n-button>
          </router-link>
          <router-link to="/admin/logs">
            <n-button>操作日志</n-button>
          </router-link>
          <n-button type="primary" :loading="weeklyLoading" @click="openWeekly">
            周报
          </n-button>
        </n-space>
      </header>

      <n-alert v-if="loadErr" type="error" class="state-msg">{{ loadErr }}</n-alert>

      <template v-else>
        <n-grid cols="2 m:4" :x-gap="12" :y-gap="12" responsive="screen" class="summary-grid">
          <n-gi v-for="(stat, idx) in statItems" :key="idx">
            <div class="admin-stat-card">
              <div class="stat-label">{{ stat.label }}</div>
              <div class="stat-value">{{ stat.value }}</div>
            </div>
          </n-gi>
        </n-grid>

        <n-card class="panel admin-panel-gap">
          <template #header>
            <n-space justify="space-between" align="center">
              <span>PV 趋势</span>
              <n-radio-group v-model:value="trendDays" size="small">
                <n-radio-button :value="7">7 天</n-radio-button>
                <n-radio-button :value="30">30 天</n-radio-button>
              </n-radio-group>
            </n-space>
          </template>
          <div class="chart-wrap">
            <canvas ref="chartCanvas" height="120" />
          </div>
        </n-card>

        <n-grid :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive class="two-col admin-panel-gap">
          <n-gi span="24 m:12">
            <n-card :title="'热门 Top ' + topArticles.length">
              <n-list v-if="topArticles.length" hoverable clickable>
                <n-list-item v-for="a in topArticles" :key="a.id">
                  <n-space justify="space-between" style="width: 100%">
                    <router-link :to="`/article/${a.id}`" class="article-link">{{ a.title }}</router-link>
                    <n-tag :bordered="false" size="small" type="info">{{ a.viewCount }} 次阅读</n-tag>
                  </n-space>
                </n-list-item>
              </n-list>
              <n-empty v-else description="暂无数据" />
            </n-card>
          </n-gi>

          <n-gi span="24 m:12">
            <n-card title="模型调用（本周）">
              <n-list v-if="aiUsage.length">
                <n-list-item v-for="row in aiUsage" :key="row.feature">
                  <n-space justify="space-between" style="width: 100%">
                    <code class="feat">{{ row.feature }}</code>
                    <strong>{{ row.count }} 次</strong>
                  </n-space>
                </n-list-item>
              </n-list>
              <n-empty v-else description="暂无数据" />
              <template #action>
                <n-space :size="8">
                  <n-button size="small" @click="loadAi('yesterday')">昨日</n-button>
                  <n-button size="small" @click="loadAi('week')">近 7 日</n-button>
                </n-space>
              </template>
            </n-card>
          </n-gi>
        </n-grid>
      </template>
    </div>

    <n-modal v-model:show="weeklyOpen" preset="card" style="width: min(720px, 100%)" title="站点周报">
      <div class="modal-body">
        <MarkdownRenderer v-if="weeklyMd" :markdown="weeklyMd" />
        <n-empty v-else description="暂无内容" />
      </div>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue';
import { Chart, registerables } from 'chart.js';
import {
  NAlert,
  NButton,
  NCard,
  NEmpty,
  NGi,
  NGrid,
  NList,
  NListItem,
  NModal,
  NRadioGroup,
  NRadioButton,
  NSpace,
  NTag,
} from 'naive-ui';
import {
  fetchStatsSummary,
  fetchPvTrend,
  fetchTopArticles,
  fetchAiUsage,
} from '../../api/stats';
import { agentWeeklyReport } from '../../api/agent';
import MarkdownRenderer from '../../components/MarkdownRenderer.vue';

Chart.register(...registerables);

const summary = ref(null);
const trendDays = ref(7);
const pvTrend = ref({ labels: [], values: [] });
const topArticles = ref([]);
const aiUsage = ref([]);
const loadErr = ref('');
const chartCanvas = ref(null);
let chartInst = null;

const weeklyOpen = ref(false);
const weeklyMd = ref('');
const weeklyLoading = ref(false);

const statItems = computed(() => [
  { label: '已发布文章', value: summary.value?.articleCount ?? '—' },
  { label: 'PV 累计', value: summary.value?.pvTotal ?? '—' },
  { label: 'UV 估算', value: summary.value?.uvEstimate ?? '—' },
  { label: 'AI 调用', value: summary.value?.aiCallTotal ?? '—' },
]);

async function refreshTrend() {
  pvTrend.value = await fetchPvTrend(trendDays.value);
  await nextTick();
  paintChart();
}

function paintChart() {
  const canvas = chartCanvas.value;
  if (!canvas) return;
  const labels = pvTrend.value?.labels || [];
  const values = (pvTrend.value?.values || []).map((v) => Number(v) || 0);
  if (chartInst) {
    chartInst.destroy();
    chartInst = null;
  }
  chartInst = new Chart(canvas.getContext('2d'), {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: 'PV',
          data: values,
          fill: true,
          tension: 0.25,
          borderColor: '#FFD600',
          backgroundColor: 'rgba(255, 214, 0, 0.15)',
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { grid: { display: false } },
        y: { beginAtZero: true, ticks: { precision: 0 } },
      },
    },
  });
}

watch(trendDays, refreshTrend);

async function loadAi(period) {
  aiUsage.value = await fetchAiUsage(period);
}

onMounted(async () => {
  try {
    const [s, t, top, ai] = await Promise.all([
      fetchStatsSummary(),
      fetchPvTrend(trendDays.value),
      fetchTopArticles(5),
      fetchAiUsage('week'),
    ]);
    summary.value = s;
    pvTrend.value = t;
    topArticles.value = top || [];
    aiUsage.value = ai || [];
    await nextTick();
    paintChart();
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
  }
});

onUnmounted(() => {
  if (chartInst) chartInst.destroy();
});

async function openWeekly() {
  weeklyLoading.value = true;
  weeklyMd.value = '';
  try {
    weeklyMd.value = await agentWeeklyReport({});
    weeklyOpen.value = true;
  } catch (e) {
    weeklyMd.value = `生成失败：${e?.message || '错误'}`;
    weeklyOpen.value = true;
  } finally {
    weeklyLoading.value = false;
  }
}
</script>

<style scoped>
.summary-grid {
  margin-bottom: var(--space-4);
}

.admin-panel-gap {
  margin-top: var(--space-4);
}

.panel :deep(.n-card) {
  box-shadow: var(--shadow-card);
  background: var(--color-surface) !important;
  border: 1px solid var(--color-border) !important;
}

.chart-wrap {
  height: 220px;
  background: var(--color-surface);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
}

.article-link {
  color: var(--color-primary);
  text-decoration: none;
}

.article-link:hover {
  text-decoration: underline;
}

.feat {
  font-family: ui-monospace, monospace;
}

.modal-body {
  max-height: 60vh;
  overflow-y: auto;
}
</style>
