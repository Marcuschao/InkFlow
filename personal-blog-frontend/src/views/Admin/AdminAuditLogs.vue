<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">操作日志</h1>
          <p class="ds-page-sub">审计管理员关键操作</p>
        </div>
        <n-space class="dash-actions" :size="12">
          <router-link to="/admin">
            <n-button>文章管理</n-button>
          </router-link>
          <router-link to="/admin/dashboard">
            <n-button>数据看板</n-button>
          </router-link>
        </n-space>
      </header>

      <n-alert type="info" style="margin-bottom: 16px;">
        超过 90 天的操作日志每周日 02:00 归档至 MinIO blog-logs 后从库中删除；慢接口快照同期导出。
      </n-alert>

      <n-alert v-if="loadErr" type="error" class="state-msg" style="margin-bottom: 16px;">{{ loadErr }}</n-alert>

      <div v-else class="logs-stack">
        <n-card :bordered="true">
          <n-data-table
            :columns="columns"
            :data="rows"
            :bordered="false"
            :single-line="false"
            :scroll-x="760"
          />
          <n-empty v-if="!rows.length && !loading" description="暂无记录" />
        </n-card>
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
import { ref, onMounted, h } from 'vue';
import { NAlert, NCard, NDataTable, NEmpty, NSpace, NButton } from 'naive-ui';
import Pagination from '../../components/Pagination.vue';
import { fetchAdminLogs } from '../../api/logs';
import { formatShortDateTime } from '../../utils/format';

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loadErr = ref('');
const loading = ref(false);

const columns = [
  { title: '操作人', key: 'username', width: 88, ellipsis: { tooltip: true } },
  {
    title: '类型',
    key: 'action',
    width: 120,
    ellipsis: { tooltip: true },
    render(row) {
      return h('code', { class: 'admin-type-badge' }, row.action);
    },
  },
  { title: '详情', key: 'detail', minWidth: 160, ellipsis: { tooltip: true } },
  { title: 'IP', key: 'ip', width: 110, render: row => row.ip || '—' },
  {
    title: '时间',
    key: 'createdAt',
    width: 120,
    render: row => formatShortDateTime(row.createdAt),
  },
];

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
</style>
