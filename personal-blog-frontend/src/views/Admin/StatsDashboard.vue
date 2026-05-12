<template>
  <div class="stats-dash-page">
    <div class="container">
      <header class="dash-header">
        <div>
          <h1 class="page-title">数据看板</h1>
          <p class="page-sub">PV、热门内容与 AI 调用概览</p>
        </div>
        <div class="dash-actions">
          <router-link to="/admin" class="link-btn">文章管理</router-link>
          <router-link to="/admin/logs" class="link-btn">操作日志</router-link>
          <button type="button" class="primary-btn" :disabled="weeklyLoading" @click="openWeekly">
            {{ weeklyLoading ? '生成中…' : '周报' }}
          </button>
        </div>
      </header>

      <div v-if="loadErr" class="state-msg">{{ loadErr }}</div>

      <section v-else class="summary-grid">
        <div class="stat-card">
          <div class="stat-label">已发布文章</div>
          <div class="stat-num">{{ summary?.articleCount ?? '—' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">PV 累计</div>
          <div class="stat-num">{{ summary?.pvTotal ?? '—' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">访客估算 (UV)</div>
          <div class="stat-num">{{ summary?.uvEstimate ?? '—' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">AI 调用累计</div>
          <div class="stat-num">{{ summary?.aiCallTotal ?? '—' }}</div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <h2>PV 趋势</h2>
          <div class="toggle">
            <button type="button" :class="{ on: trendDays === 7 }" @click="trendDays = 7">7 天</button>
            <button type="button" :class="{ on: trendDays === 30 }" @click="trendDays = 30">30 天</button>
          </div>
        </div>
        <div class="chart-wrap">
          <canvas ref="chartCanvas" height="120" />
        </div>
      </section>

      <div class="two-col">
        <section class="panel">
          <h2 class="panel-title">热门 Top {{ topArticles.length }}</h2>
          <table v-if="topArticles.length" class="mini-table">
            <thead>
              <tr>
                <th>标题</th>
                <th class="num">阅读</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in topArticles" :key="a.id">
                <td>
                  <router-link :to="`/article/${a.id}`" class="article-link">{{ a.title }}</router-link>
                </td>
                <td class="num">{{ a.viewCount }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty">暂无数据</p>
        </section>

        <section class="panel">
          <h2 class="panel-title">AI 调用（本周）</h2>
          <ul v-if="aiUsage.length" class="usage-list">
            <li v-for="row in aiUsage" :key="row.feature">
              <span class="feat">{{ row.feature }}</span>
              <span class="cnt">{{ row.count }}</span>
            </li>
          </ul>
          <div class="usage-foot">
            <button type="button" class="ghost-btn" @click="loadAi('yesterday')">昨日</button>
            <button type="button" class="ghost-btn" @click="loadAi('week')">近 7 日</button>
          </div>
          <p v-if="!aiUsage.length" class="empty">暂无数据</p>
        </section>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="weeklyOpen" class="modal-mask" @click.self="weeklyOpen = false">
        <div class="modal-box">
          <header class="modal-head">
            <span>站点周报</span>
            <button type="button" class="icon-close" aria-label="关闭" @click="weeklyOpen = false">×</button>
          </header>
          <div class="modal-body">
            <MarkdownRenderer v-if="weeklyMd" :markdown="weeklyMd" />
            <p v-else class="empty">暂无内容</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { Chart, registerables } from 'chart.js';
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
          borderColor: 'rgba(79, 70, 229, 0.85)',
          backgroundColor: 'rgba(79, 70, 229, 0.08)',
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
.stats-dash-page {
  padding: 2rem 0 3rem;
}

.dash-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.75rem;
}

.page-title {
  margin: 0;
  font-size: 1.85rem;
  font-weight: 700;
}

.page-sub {
  margin: 0.35rem 0 0;
  color: var(--color-text-muted);
}

.dash-actions {
  display: flex;
  gap: 0.6rem;
}

.link-btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  text-decoration: none;
  color: var(--color-text);
  font-weight: 600;
}

.primary-btn {
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  font-weight: 650;
  color: #fff;
  background: var(--gradient-cta);
}

.primary-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.summary-grid {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(11rem, 1fr));
  margin-bottom: 1.75rem;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1rem 1.15rem;
  box-shadow: var(--shadow-xs);
}

.stat-label {
  font-size: 0.8rem;
  color: var(--color-text-muted);
}

.stat-num {
  margin-top: 0.35rem;
  font-size: 1.45rem;
  font-weight: 800;
}

.panel {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 1.15rem 1.25rem;
  margin-bottom: 1.25rem;
  box-shadow: var(--shadow-xs);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.panel-head h2 {
  margin: 0;
  font-size: 1.05rem;
}

.toggle {
  display: flex;
  gap: 0.35rem;
}

.toggle button {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-pill);
  cursor: pointer;
  font-size: 0.78rem;
}

.toggle button.on {
  background: var(--color-primary-soft);
  border-color: var(--border-accent-strong);
  font-weight: 700;
}

.chart-wrap {
  height: 220px;
}

.two-col {
  display: grid;
  gap: 1.25rem;
}

@media (min-width: 900px) {
  .two-col {
    grid-template-columns: 1fr 1fr;
  }
}

.panel-title {
  margin: 0 0 0.85rem;
  font-size: 1.05rem;
}

.mini-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.mini-table th,
.mini-table td {
  padding: 0.45rem 0.35rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
}

.mini-table .num {
  text-align: right;
  width: 4rem;
}

.article-link {
  color: var(--color-primary);
  text-decoration: none;
}

.article-link:hover {
  text-decoration: underline;
}

.usage-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.usage-list li {
  display: flex;
  justify-content: space-between;
  padding: 0.4rem 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.88rem;
}

.usage-list .feat {
  font-family: ui-monospace, monospace;
  font-size: 0.82rem;
}

.usage-list .cnt {
  font-weight: 700;
}

.usage-foot {
  margin-top: 0.65rem;
  display: flex;
  gap: 0.5rem;
}

.ghost-btn {
  border: 1px solid var(--color-border);
  background: transparent;
  border-radius: var(--radius-md);
  padding: 0.3rem 0.65rem;
  cursor: pointer;
  font-size: 0.78rem;
}

.empty {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.state-msg {
  color: #b91c1c;
  margin-bottom: 1rem;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.modal-box {
  width: min(720px, 100%);
  max-height: min(85vh, 640px);
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.85rem;
  border-bottom: 1px solid var(--color-border);
  font-weight: 700;
}

.icon-close {
  border: none;
  background: transparent;
  font-size: 1.35rem;
  line-height: 1;
  cursor: pointer;
  color: var(--color-text-muted);
}

.modal-body {
  padding: 0.85rem 1rem;
  overflow: auto;
  flex: 1;
}
</style>
