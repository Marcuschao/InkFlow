<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">操作日志</h1>
          <p class="ds-page-sub">审计管理员关键操作</p>
        </div>
        <div class="dash-actions">
          <router-link to="/admin" class="admin-link-btn">文章管理</router-link>
          <router-link to="/admin/dashboard" class="admin-link-btn">数据看板</router-link>
        </div>
      </header>

      <div v-if="loadErr" class="state-msg">{{ loadErr }}</div>

      <div v-else class="logs-stack">
        <div class="ds-table-shell">
          <table>
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
                <td><code class="admin-type-badge">{{ row.action }}</code></td>
                <td class="td-detail">{{ row.detail || '—' }}</td>
                <td>{{ row.ip || '—' }}</td>
                <td>{{ formatTime(row.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="!rows.length && !loading" class="empty">暂无记录</div>
        </div>
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
.logs-stack {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.td-detail {
  max-width: 280px;
  word-break: break-word;
}

.empty {
  padding: 2rem;
  text-align: center;
  color: var(--color-text-muted);
}

.state-msg {
  color: var(--color-danger);
  margin-bottom: 1rem;
}

@media (max-width: 768px) {
  .logs-stack :deep(.ds-table-shell) {
    overflow-x: auto;
  }
}
</style>
