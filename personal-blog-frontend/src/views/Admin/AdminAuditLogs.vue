<template>
  <div class="admin-logs-page">
    <div class="container">
      <header class="dash-header">
        <div>
          <h1 class="page-title">操作日志</h1>
          <p class="page-sub">审计管理员关键操作</p>
        </div>
        <div class="dash-actions">
          <router-link to="/admin" class="link-btn">文章管理</router-link>
          <router-link to="/admin/dashboard" class="link-btn">数据看板</router-link>
        </div>
      </header>

      <div v-if="loadErr" class="state-msg">{{ loadErr }}</div>

      <div v-else class="logs-panel">
        <table class="logs-table">
          <thead>
            <tr>
              <th>操作人</th>
              <th>类型</th>
              <th>详情</th>
              <th>IP</th>
              <th>时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.username }}</td>
              <td><code class="act">{{ row.action }}</code></td>
              <td class="td-detail">{{ row.detail || '—' }}</td>
              <td>{{ row.ip || '—' }}</td>
              <td>{{ formatTime(row.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="!rows.length && !loading" class="empty">暂无记录</div>
        <Pagination
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @changePage="onPage"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import Pagination from '../../components/Pagination.vue';
import { fetchAdminLogs } from '../../api/logs';

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loadErr = ref('');
const loading = ref(false);

function formatTime(t) {
  if (!t) return '—';
  return new Date(t).toLocaleString();
}

async function load() {
  loading.value = true;
  loadErr.value = '';
  try {
    const pr = await fetchAdminLogs(page.value, pageSize.value);
    rows.value = pr?.records || [];
    total.value = Number(pr?.total) || 0;
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
    rows.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function onPage(p) {
  page.value = p;
  load();
}

onMounted(load);
</script>

<style scoped>
.admin-logs-page {
  padding: 2.25rem 0 3.5rem;
}

.dash-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.page-title {
  margin: 0;
  font-size: clamp(1.85rem, 4vw, 2.2rem);
  font-weight: 760;
}

.page-sub {
  margin: 0.35rem 0 0;
  font-size: 0.92rem;
  color: var(--color-text-muted);
}

.dash-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  padding: 0.65rem 1.1rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  text-decoration: none;
  font-weight: 650;
  font-size: 0.88rem;
  color: var(--color-text);
}

.logs-panel {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.logs-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.logs-table th,
.logs-table td {
  padding: 0.75rem 1rem;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.logs-table th {
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-soft);
  background: rgba(248, 250, 252, 0.95);
}

.td-detail {
  max-width: 280px;
  word-break: break-word;
}

.act {
  font-size: 0.78rem;
  padding: 0.15rem 0.4rem;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.empty {
  padding: 2rem;
  text-align: center;
  color: var(--color-text-muted);
}

.state-msg {
  color: #b91c1c;
  margin-bottom: 1rem;
}

@media (max-width: 768px) {
  .logs-panel {
    overflow-x: auto;
  }
}
</style>
