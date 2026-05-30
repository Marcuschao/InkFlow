<template>
  <div class="admin-page">
    <div class="container">
      <header class="ds-admin-header" style="margin-bottom: var(--space-6);">
        <div>
          <h1 class="ds-page-title">内容举报</h1>
          <p class="ds-page-sub">用户举报的文章</p>
        </div>
        <router-link to="/admin">
          <n-button>返回管理</n-button>
        </router-link>
      </header>

      <n-tabs v-model:value="statusFilter" type="line" @update:value="load">
        <n-tab name="all" tab="全部" />
        <n-tab name="0" tab="待处理" />
        <n-tab name="1" tab="已处理" />
        <n-tab name="2" tab="已忽略" />
      </n-tabs>

      <n-card :bordered="true" style="margin-top: var(--space-4);">
        <n-data-table
          v-if="rows.length"
          :columns="columns"
          :data="rows"
          :bordered="false"
          :scroll-x="800"
        />
        <n-empty v-else description="暂无举报" />
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { NButton, NCard, NDataTable, NEmpty, NTab, NTabs } from 'naive-ui';
import { getContentReports, handleContentReport } from '../../api/article';
import { formatShortDateTime } from '../../utils/format';
import { useToastStore } from '../../stores/toast';

const toast = useToastStore();
const rows = ref([]);
const statusFilter = ref('0');

const statusLabel = (s) => ({ 0: '待处理', 1: '已处理', 2: '已忽略' }[s] || '未知');

const columns = [
  { title: '文章ID', key: 'targetId', width: 88 },
  { title: '原因', key: 'reason', minWidth: 200, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 88,
    render(row) {
      return statusLabel(row.status);
    },
  },
  {
    title: '时间',
    key: 'createTime',
    width: 140,
    render(row) {
      return formatShortDateTime(row.createTime);
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      if (row.status !== 0) return '—';
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => onHandle(row.id, 1),
        }, () => '处理'),
        h(NButton, {
          size: 'small',
          secondary: true,
          onClick: () => onHandle(row.id, 2),
        }, () => '忽略'),
      ]);
    },
  },
];

async function load() {
  const status = statusFilter.value === 'all' ? undefined : Number(statusFilter.value);
  const res = await getContentReports({ page: 1, size: 50, status });
  rows.value = res?.data?.records || [];
}

async function onHandle(id, status) {
  const note = prompt(status === 1 ? '处理备注（可选）' : '忽略备注（可选）');
  if (note === null) return;
  try {
    await handleContentReport(id, status, note || undefined);
    toast.push('已更新', 'success');
    await load();
  } catch (e) {
    toast.push(e?.message || '操作失败', 'error');
  }
}

onMounted(load);
</script>
