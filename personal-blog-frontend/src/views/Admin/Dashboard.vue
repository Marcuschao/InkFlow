<template>
  <div class="admin-dashboard-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">文章管理</h1>
          <p class="ds-page-sub">创建、编辑与发布内容</p>
        </div>
        <n-space class="dash-actions" :size="8" wrap>
          <router-link to="/admin/settings"><n-button size="small">站点设置</n-button></router-link>
          <router-link to="/admin/links"><n-button size="small">友链管理</n-button></router-link>
          <router-link to="/admin/comments"><n-button size="small">评论审核</n-button></router-link>
          <router-link to="/admin/translations"><n-button size="small">翻译</n-button></router-link>
          <router-link to="/admin/freshness"><n-button size="small">内容保鲜</n-button></router-link>
          <router-link to="/admin/diary"><n-button size="small">写日记</n-button></router-link>
          <router-link to="/admin/diary/list"><n-button size="small">日记列表</n-button></router-link>
          <router-link to="/admin/logs"><n-button size="small">操作日志</n-button></router-link>
          <router-link to="/admin/dashboard"><n-button size="small">数据看板</n-button></router-link>
          <router-link to="/admin/push"><n-button size="small">推送管理</n-button></router-link>
          <router-link to="/admin/stream"><n-button size="small">消息监控</n-button></router-link>
          <router-link to="/admin/monitor"><n-button size="small">性能监控</n-button></router-link>
          <router-link to="/admin/new"><n-button size="small" type="primary">＋ 新建文章</n-button></router-link>
          <router-link to="/admin/ai-weekly"><n-button size="small">AI 周报</n-button></router-link>
        </n-space>
      </header>

      <n-card :bordered="true">
        <n-data-table
          v-if="articles.length"
          :columns="columns"
          :data="articles"
          :bordered="false"
          :single-line="false"
          :scroll-x="640"
        />
        <n-empty v-else description="暂无文章">
          <template #extra>
            <router-link to="/admin/new">
              <n-button type="primary">立即新建一篇</n-button>
            </router-link>
          </template>
        </n-empty>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, h } from 'vue';
import { RouterLink } from 'vue-router';
import { NButton, NCard, NDataTable, NEmpty, NSpace } from 'naive-ui';
import { useArticleStore } from '../../stores/article';
import { deleteArticle } from '../../api/article';
import { formatShortDateTime } from '../../utils/format';

const articleStore = useArticleStore();
const articles = ref([]);

const columns = [
  { title: 'ID', key: 'id', width: 56 },
  {
    title: '标题',
    key: 'title',
    minWidth: 140,
    ellipsis: { tooltip: true },
  },
  {
    title: '发布日期',
    key: 'createTime',
    width: 120,
    render(row) {
      return formatShortDateTime(row.createTime || row.createdAt);
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right',
    render(row) {
      return h(NSpace, { size: 8 }, () => [
        h(
          RouterLink,
          { to: `/admin/edit/${row.id}` },
          () => h(NButton, { size: 'small', type: 'primary', secondary: true }, () => '编辑')
        ),
        h(
          NButton,
          {
            size: 'small',
            type: 'error',
            secondary: true,
            onClick: () => confirmDelete(row.id),
          },
          () => '删除'
        ),
      ]);
    },
  },
];

const fetchArticles = async () => {
  await articleStore.fetchArticles(1, 500, null);
  articles.value = articleStore.articles;
};

const confirmDelete = async (id) => {
  if (confirm('确定要删除这篇文章吗？')) {
    try {
      await deleteArticle(id);
      alert('文章删除成功！');
      fetchArticles();
    } catch (error) {
      console.error('Failed to delete article:', error);
      alert('删除失败，请稍后再试。');
    }
  }
};

onMounted(() => {
  fetchArticles();
});
</script>

<style scoped>
</style>
