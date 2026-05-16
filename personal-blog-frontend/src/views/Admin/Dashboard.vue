<template>
  <div class="admin-dashboard-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">文章管理</h1>
          <p class="ds-page-sub">创建、编辑与发布内容</p>
        </div>
        <div class="dash-actions">
          <router-link to="/admin/settings" class="ds-btn ds-btn--secondary ds-btn--pill">站点设置</router-link>
          <router-link to="/admin/links" class="ds-btn ds-btn--secondary ds-btn--pill">友链管理</router-link>
          <router-link to="/admin/comments" class="ds-btn ds-btn--secondary ds-btn--pill">评论审核</router-link>
          <router-link to="/admin/translations" class="ds-btn ds-btn--secondary ds-btn--pill">翻译</router-link>
          <router-link to="/admin/freshness" class="ds-btn ds-btn--secondary ds-btn--pill">内容保鲜</router-link>
          <router-link to="/admin/diary" class="ds-btn ds-btn--secondary ds-btn--pill">写日记</router-link>
          <router-link to="/admin/diary/list" class="ds-btn ds-btn--secondary ds-btn--pill">日记列表</router-link>
          <router-link to="/admin/logs" class="ds-btn ds-btn--secondary ds-btn--pill">操作日志</router-link>
          <router-link to="/admin/dashboard" class="ds-btn ds-btn--secondary ds-btn--pill">数据看板</router-link>
          <router-link to="/admin/new" class="ds-btn ds-btn--green ds-btn--pill">＋ 新建文章</router-link>
          <router-link to="/admin/ai-weekly" class="ds-btn ds-btn--secondary ds-btn--pill">AI 周报</router-link>
        </div>
      </header>

      <div v-if="articles.length" class="article-management-list ds-table-shell">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>发布日期</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="article in articles" :key="article.id">
              <td>{{ article.id }}</td>
              <td class="td-title">{{ article.title }}</td>
              <td class="td-date">{{ formatDate(article.createTime || article.createdAt) }}</td>
              <td class="actions-column">
                <router-link
                  :to="`/admin/edit/${article.id}`"
                  class="edit-button ds-btn ds-btn--primary"
                >编辑</router-link>
                <button
                  type="button"
                  class="delete-button ds-btn ds-btn--danger"
                  @click="confirmDelete(article.id)"
                >
                  删除
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="no-articles ds-empty-panel">
        <p>暂无文章，<router-link to="/admin/new" class="ds-link-inline">立即新建一篇</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useArticleStore } from '../../stores/article';
import { deleteArticle } from '../../api/article';

const articleStore = useArticleStore();
const articles = ref([]);

const fetchArticles = async () => {
  await articleStore.fetchArticles(1, 500, null);
  articles.value = articleStore.articles;
};

const formatDate = (dateString) => {
  const options = { year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
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
.dash-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
}

.edit-button {
  padding: 0.45rem 0.85rem;
  margin: 0 var(--space-2);
  font-size: var(--text-sm);
  text-decoration: none;
}

.delete-button {
  padding: 0.45rem 0.85rem;
  margin: 0 var(--space-2);
  font-size: var(--text-sm);
}

.td-title {
  font-weight: var(--weight-semibold);
  color: var(--color-text);
  max-width: 320px;
}

.td-date {
  font-size: var(--text-88);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.actions-column {
  width: 160px;
  text-align: right;
  white-space: nowrap;
}

.no-articles p {
  margin: 0;
}

@media (max-width: 768px) {
  .article-management-list {
    overflow-x: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .dash-actions :deep(.ds-btn:hover) {
    transform: none;
  }
}
</style>
