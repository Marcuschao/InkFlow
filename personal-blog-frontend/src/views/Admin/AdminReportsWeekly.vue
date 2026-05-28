<template>
  <div class="admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">周报归档</h1>
          <p class="ds-page-sub">AI 周报 MinIO 持久化记录</p>
        </div>
        <n-space class="dash-actions" :size="12">
          <router-link to="/admin/ai-weekly"><n-button>生成周报</n-button></router-link>
          <router-link to="/admin"><n-button>返回管理</n-button></router-link>
        </n-space>
      </header>

      <n-alert v-if="loadErr" type="error" style="margin-bottom: 16px;">{{ loadErr }}</n-alert>

      <n-card :bordered="true">
        <n-data-table
          :columns="columns"
          :data="rows"
          :bordered="false"
          :single-line="false"
          :scroll-x="640"
        />
        <n-empty v-if="!rows.length && !loading" description="暂无归档" />
      </n-card>
      <Pagination
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @changePage="onPage"
      />

      <n-modal v-model:show="showModal" preset="card" style="width: min(720px, 100%)" :title="modalTitle">
        <div class="markdown-renderer markdown-prose">
          <MarkdownRenderer :markdown="modalBody" />
        </div>
      </n-modal>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { NAlert, NButton, NCard, NDataTable, NEmpty, NModal, NSpace } from 'naive-ui';
import Pagination from '../../components/Pagination.vue';
import MarkdownRenderer from '../../components/MarkdownRenderer.vue';
import { fetchWeeklyReports, fetchReportContent } from '../../api/reports';
import { formatShortDateTime } from '../../utils/format';

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loadErr = ref('');
const loading = ref(false);
const showModal = ref(false);
const modalTitle = ref('');
const modalBody = ref('');

function formatSize(n) {
  if (n == null) return '—';
  if (n < 1024) return `${n} B`;
  return `${(n / 1024).toFixed(1)} KB`;
}

async function openReport(row) {
  try {
    modalTitle.value = row.title || '周报';
    modalBody.value = await fetchReportContent(row.id);
    showModal.value = true;
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
  }
}

const columns = [
  { title: 'ID', key: 'id', width: 56 },
  { title: '标题', key: 'title', minWidth: 160, ellipsis: { tooltip: true } },
  {
    title: '大小',
    key: 'fileSize',
    width: 88,
    render: (row) => formatSize(row.fileSize),
  },
  {
    title: '生成时间',
    key: 'createdAt',
    width: 120,
    render: (row) => formatShortDateTime(row.createdAt),
  },
  {
    title: '操作',
    key: 'actions',
    width: 88,
    render(row) {
      return h(NButton, { size: 'small', onClick: () => openReport(row) }, () => '查看');
    },
  },
];

async function load() {
  loading.value = true;
  loadErr.value = '';
  try {
    const data = await fetchWeeklyReports(page.value, pageSize.value);
    rows.value = data?.records || [];
    total.value = data?.total || 0;
  } catch (e) {
    loadErr.value = e?.message || '加载失败';
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
