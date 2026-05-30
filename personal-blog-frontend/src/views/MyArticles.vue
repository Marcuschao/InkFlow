<template>
  <div class="my-articles-page">
    <div class="container">
      <header class="page-head">
        <div>
          <h1 class="ds-page-title">我的文章</h1>
          <p class="ds-page-sub">管理草稿、待审核与已发布内容</p>
        </div>
        <router-link to="/write">
          <n-button type="primary">写文章</n-button>
        </router-link>
      </header>

      <n-tabs v-model:value="statusFilter" type="line" @update:value="load">
        <n-tab name="all" tab="全部" />
        <n-tab :name="String(ARTICLE_STATUS.DRAFT)" tab="草稿" />
        <n-tab :name="String(ARTICLE_STATUS.PENDING)" tab="待审核" />
        <n-tab :name="String(ARTICLE_STATUS.PUBLISHED)" tab="已发布" />
        <n-tab :name="String(ARTICLE_STATUS.REJECTED)" tab="已驳回" />
      </n-tabs>

      <n-card :bordered="true" style="margin-top: var(--space-4);">
        <n-data-table
          v-if="rows.length"
          :columns="columns"
          :data="rows"
          :bordered="false"
          :scroll-x="720"
        />
        <n-empty v-else description="暂无文章">
          <template #extra>
            <router-link to="/write">
              <n-button type="primary">去写一篇</n-button>
            </router-link>
          </template>
        </n-empty>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NCard, NDataTable, NEmpty, NTab, NTabs, NTag } from 'naive-ui';
import { getMyArticles, deleteArticle, ARTICLE_STATUS, articleStatusLabel } from '../api/article';
import { formatShortDateTime } from '../utils/format';
import { useToastStore } from '../stores/toast';

const router = useRouter();
const toast = useToastStore();
const rows = ref([]);
const statusFilter = ref('all');

const columns = [
  { title: '标题', key: 'title', minWidth: 180, ellipsis: { tooltip: true } },
  {
    title: '状态',
    key: 'status',
    width: 96,
    render(row) {
      const type = row.status === ARTICLE_STATUS.PUBLISHED ? 'success'
        : row.status === ARTICLE_STATUS.REJECTED ? 'error'
        : row.status === ARTICLE_STATUS.PENDING ? 'warning' : 'default';
      return h(NTag, { type, bordered: false, size: 'small' }, () => articleStatusLabel(row.status));
    },
  },
  {
    title: '更新时间',
    key: 'updateTime',
    width: 140,
    render(row) {
      return formatShortDateTime(row.updateTime || row.createTime);
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render(row) {
      return h('div', { style: 'display:flex;gap:8px' }, [
        h(NButton, {
          size: 'small',
          type: 'primary',
          secondary: true,
          onClick: () => router.push(`/write/edit/${row.id}`),
        }, () => '编辑'),
        h(NButton, {
          size: 'small',
          type: 'error',
          secondary: true,
          onClick: () => onDelete(row.id),
        }, () => '删除'),
      ]);
    },
  },
];

async function load() {
  const status = statusFilter.value === 'all' ? undefined : Number(statusFilter.value);
  const res = await getMyArticles({ page: 1, size: 100, status });
  rows.value = res?.data?.records || res?.data || [];
}

async function onDelete(id) {
  if (!confirm('确定删除这篇文章？')) return;
  try {
    await deleteArticle(id);
    toast.push('已删除', 'success');
    await load();
  } catch (e) {
    toast.push(e?.message || '删除失败', 'error');
  }
}

onMounted(load);
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
</style>
