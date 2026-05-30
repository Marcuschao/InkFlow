<template>
  <div class="admin-page">
    <div class="container">
      <header class="ds-admin-header" style="margin-bottom: var(--space-6);">
        <div>
          <h1 class="ds-page-title">文章审核</h1>
          <p class="ds-page-sub">待审核投稿列表</p>
        </div>
        <router-link to="/admin">
          <n-button>返回管理</n-button>
        </router-link>
      </header>

      <n-card :bordered="true">
        <n-data-table
          v-if="rows.length"
          :columns="columns"
          :data="rows"
          :bordered="false"
          :scroll-x="900"
        />
        <n-empty v-else description="暂无待审核文章" />
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { NButton, NCard, NDataTable, NEmpty, NTag } from 'naive-ui';
import { getReviewArticles, approveArticle, rejectArticle } from '../../api/article';
import { formatShortDateTime } from '../../utils/format';
import { useToastStore } from '../../stores/toast';

const toast = useToastStore();
const rows = ref([]);

const columns = [
  { title: 'ID', key: 'id', width: 64 },
  { title: '标题', key: 'title', minWidth: 160, ellipsis: { tooltip: true } },
  {
    title: '质量分',
    key: 'reviewScore',
    width: 80,
    render(row) {
      return row.reviewScore != null ? row.reviewScore : '—';
    },
  },
  {
    title: '检测说明',
    key: 'reviewReason',
    minWidth: 160,
    ellipsis: { tooltip: true },
    render(row) {
      return row.reviewReason || '—';
    },
  },
  {
    title: '提交时间',
    key: 'submittedAt',
    width: 140,
    render(row) {
      return formatShortDateTime(row.submittedAt || row.updateTime);
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => onApprove(row.id),
        }, () => '通过'),
        h(NButton, {
          size: 'small',
          type: 'error',
          secondary: true,
          onClick: () => onReject(row.id),
        }, () => '驳回'),
      ]);
    },
  },
];

async function load() {
  const res = await getReviewArticles({ page: 1, size: 50 });
  rows.value = res?.data?.records || [];
}

async function onApprove(id) {
  try {
    await approveArticle(id);
    toast.push('已通过', 'success');
    await load();
  } catch (e) {
    toast.push(e?.message || '操作失败', 'error');
  }
}

async function onReject(id) {
  const reason = prompt('驳回原因（可选）');
  if (reason === null) return;
  try {
    await rejectArticle(id, reason || undefined);
    toast.push('已驳回', 'success');
    await load();
  } catch (e) {
    toast.push(e?.message || '操作失败', 'error');
  }
}

onMounted(load);
</script>
