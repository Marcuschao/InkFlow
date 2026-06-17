<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">性能监控</h1>
          <p class="ds-page-sub">缓存命中率、限流拒绝、慢接口与 JVM 指标</p>
        </div>
        <n-space :size="8">
          <router-link to="/admin"><n-button>文章管理</n-button></router-link>
          <router-link to="/admin/stream"><n-button>消息监控</n-button></router-link>
          <n-button type="primary" :loading="loading" @click="loadAll">刷新</n-button>
        </n-space>
      </header>

      <n-alert v-if="loadErr" type="error" class="state-msg">{{ loadErr }}</n-alert>

      <template v-else>
        <n-grid cols="2 m:3" :x-gap="12" :y-gap="12" responsive="screen" class="summary-grid">
          <n-gi>
            <n-card size="small"><n-statistic label="堆内存已用" :value="formatBytes(jvm?.heapUsed)" /></n-card>
          </n-gi>
          <n-gi>
            <n-card size="small"><n-statistic label="堆内存上限" :value="formatBytes(jvm?.heapMax)" /></n-card>
          </n-gi>
          <n-gi>
            <n-card size="small"><n-statistic label="线程数" :value="jvm?.threadCount ?? '—'" /></n-card>
          </n-gi>
          <n-gi>
            <n-card size="small"><n-statistic label="GC 次数" :value="jvm?.gcCount ?? '—'" /></n-card>
          </n-gi>
          <n-gi>
            <n-card size="small"><n-statistic label="限流拒绝" :value="rateLimit?.rejectedTotal ?? '—'" /></n-card>
          </n-gi>
          <n-gi>
            <n-card size="small"><n-statistic label="限流放行" :value="rateLimit?.allowedTotal ?? '—'" /></n-card>
          </n-gi>
        </n-grid>

        <n-grid cols="1 m:2" :x-gap="16" :y-gap="16" responsive="screen" style="margin-top: 16px;">
          <n-gi>
            <n-card title="缓存命中率">
              <div class="chart-wrap"><canvas ref="cacheCanvas" /></div>
            </n-card>
          </n-gi>
          <n-gi>
            <n-card title="系统负载">
              <n-descriptions :column="1" label-placement="left" bordered size="small">
                <n-descriptions-item label="CPU 核心">{{ system?.availableProcessors ?? '—' }}</n-descriptions-item>
                <n-descriptions-item label="系统负载">{{ formatLoad(system?.systemLoadAverage) }}</n-descriptions-item>
                <n-descriptions-item label="Caffeine 命中率">{{ formatRate(cache?.caffeineHitRate) }}</n-descriptions-item>
                <n-descriptions-item label="Redis 命中率">{{ formatRate(cache?.redisHitRate) }}</n-descriptions-item>
                <n-descriptions-item label="限流拒绝累计">{{ rateLimit?.rejectedTotal ?? '—' }}</n-descriptions-item>
              </n-descriptions>
            </n-card>
          </n-gi>
        </n-grid>

        <n-card title="慢接口 TOP10" style="margin-top: 16px;">
          <n-data-table
            :columns="slowColumns"
            :data="slowList"
            :bordered="false"
            :single-line="false"
            :scroll-x="640"
          />
        </n-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { Chart, registerables } from 'chart.js';
import {
  NAlert,
  NButton,
  NCard,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NGi,
  NGrid,
  NSpace,
  NStatistic,
} from 'naive-ui';
import {
  fetchMonitorCache,
  fetchMonitorJvm,
  fetchMonitorRateLimit,
  fetchMonitorSlow,
  fetchMonitorSystem,
} from '../../api/monitor';

Chart.register(...registerables);

const loading = ref(false);
const loadErr = ref('');
const cache = ref(null);
const jvm = ref(null);
const system = ref(null);
const rateLimit = ref(null);
const slowList = ref([]);
const cacheCanvas = ref(null);
let chartInst = null;

const slowColumns = [
  { title: '接口', key: 'endpoint', ellipsis: { tooltip: true } },
  { title: '耗时(ms)', key: 'costMs', width: 100 },
  {
    title: '时间',
    key: 'timestamp',
    width: 180,
    render(row) {
      return row.timestamp ? new Date(row.timestamp).toLocaleString() : '—';
    },
  },
];

function formatBytes(v) {
  if (v == null) return '—';
  const n = Number(v);
  if (Number.isNaN(n)) return '—';
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / 1024 / 1024).toFixed(1)} MB`;
}

function formatRate(v) {
  if (v == null) return '—';
  return `${Number(v).toFixed(1)}%`;
}

function formatLoad(v) {
  if (v == null || v < 0) return '—';
  return Number(v).toFixed(2);
}

function paintChart() {
  const canvas = cacheCanvas.value;
  if (!canvas || !cache.value) return;
  if (chartInst) {
    chartInst.destroy();
    chartInst = null;
  }
  chartInst = new Chart(canvas.getContext('2d'), {
    type: 'doughnut',
    data: {
      labels: ['Caffeine 命中', 'Caffeine 未命中', 'Redis 命中', 'Redis 未命中'],
      datasets: [{
        data: [
          cache.value.caffeineHit || 0,
          cache.value.caffeineMiss || 0,
          cache.value.redisHit || 0,
          cache.value.redisMiss || 0,
        ],
        backgroundColor: ['#FFD600', '#3B82F6', '#10B981', '#71717A'],
      }],
    },
    options: { responsive: true, maintainAspectRatio: false },
  });
}

async function loadAll() {
  loading.value = true;
  loadErr.value = '';
  try {
    const [c, j, s, rl, slow] = await Promise.all([
      fetchMonitorCache(),
      fetchMonitorJvm(),
      fetchMonitorSystem(),
      fetchMonitorRateLimit(),
      fetchMonitorSlow(),
    ]);
    cache.value = c;
    jvm.value = j;
    system.value = s;
    rateLimit.value = rl;
    slowList.value = slow || [];
    await nextTick();
    paintChart();
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadAll);
onUnmounted(() => {
  if (chartInst) chartInst.destroy();
});
</script>

<style scoped>
.summary-grid {
  margin-bottom: var(--space-4);
}

.chart-wrap {
  height: 220px;
}

.state-msg {
  margin-bottom: var(--space-4);
}
</style>
