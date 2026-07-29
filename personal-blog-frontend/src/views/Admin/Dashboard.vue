<template>
  <div class="admin-dashboard-page admin-page">
    <div class="container">
      <header class="dash-header">
        <div class="dash-heading">
          <p class="dash-eyebrow">CONTENT / ARTICLES</p>
          <h1>文章管理</h1>
          <p class="dash-subtitle">创建、编辑与发布内容</p>
        </div>
        <div class="dash-actions">
          <div class="dash-count"><strong>{{ articles.length }}</strong><span>篇已发布</span></div>
          <el-button type="primary" :icon="Plus" @click="router.push('/admin/new')">新建文章</el-button>
        </div>
      </header>

      <el-card class="article-table-card" shadow="never">
        <template #header>
          <div class="table-heading">
            <div><strong>全部文章</strong><span>按发布时间倒序排列</span></div>
            <span class="table-meta">共 {{ articles.length }} 篇</span>
          </div>
        </template>
        <el-table v-loading="loading" :data="articles" row-key="id" empty-text="暂无文章">
          <el-table-column prop="id" label="ID" width="72" />
          <el-table-column prop="title" label="标题" min-width="260" show-overflow-tooltip>
            <template #default="{ row }"><span class="article-title">{{ row.title }}</span></template>
          </el-table-column>
          <el-table-column label="发布日期" width="160">
            <template #default="{ row }">{{ formatShortDateTime(row.createTime || row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button text type="primary" :icon="Edit" @click="router.push(`/admin/edit/${row.id}`)">编辑</el-button>
                <el-button text type="danger" :icon="Delete" @click="confirmDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无文章">
              <el-button type="primary" :icon="Plus" @click="router.push('/admin/new')">立即新建一篇</el-button>
            </el-empty>
          </template>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Delete, Edit, Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useArticleStore } from '../../stores/article';
import { deleteArticle } from '../../api/article';
import { formatShortDateTime } from '../../utils/format';

const router = useRouter();
const articleStore = useArticleStore();
const articles = ref([]);
const loading = ref(false);

async function fetchArticles() {
  loading.value = true;
  try {
    await articleStore.fetchArticles(1, 500, null);
    articles.value = articleStore.articles;
  } finally {
    loading.value = false;
  }
}

async function confirmDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除《${row.title || '未命名文章'}》吗？此操作不可撤销。`, '删除文章', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    });
    await deleteArticle(row.id);
    ElMessage.success('文章已删除');
    await fetchArticles();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('Failed to delete article:', error);
      ElMessage.error('删除失败，请稍后重试');
    }
  }
}

onMounted(fetchArticles);
</script>

<style scoped>
.admin-dashboard-page { padding: 32px 0 48px; }
.dash-header { position:relative; display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:24px; padding:28px 32px; border:1px solid var(--color-border); border-radius:14px; background:linear-gradient(135deg, var(--color-surface) 0%, color-mix(in srgb, var(--color-surface) 82%, var(--surface-primary-tint)) 100%); box-shadow:var(--shadow-sm); overflow:hidden; }
.dash-header::after { content:''; position:absolute; right:-70px; top:-90px; width:220px; height:220px; border:1px solid color-mix(in srgb, var(--color-primary) 24%, transparent); border-radius:50%; pointer-events:none; }
.dash-heading { position:relative; z-index:1; }
.dash-eyebrow { margin:0 0 8px !important; color:var(--color-primary) !important; font:600 12px/1.4 var(--font-mono); letter-spacing:.08em; }
.dash-header h1 { margin:0; color:var(--color-text); font-size:clamp(28px,4vw,42px); font-weight:600; line-height:1.15; }
.dash-subtitle { margin:10px 0 0 !important; color:var(--color-text-muted); font-size:15px; }
.dash-actions { position:relative; z-index:1; display:flex; align-items:center; gap:20px; }
.dash-count { display:flex; align-items:baseline; gap:7px; color:var(--color-text-muted); }
.dash-count strong { color:var(--color-text); font-size:28px; font-weight:600; }
.dash-count span { font-size:13px; }
.article-table-card { overflow:hidden; border-color:var(--color-border); box-shadow:var(--shadow-sm); }
.article-table-card :deep(.el-card__header) { padding:18px 24px; border-bottom:1px solid var(--color-border); background:var(--color-surface); }
.article-table-card :deep(.el-card__body) { padding:0; }
.table-heading { display:flex; align-items:center; justify-content:space-between; gap:16px; }
.table-heading div { display:flex; align-items:baseline; gap:12px; }
.table-heading strong { color:var(--color-text); font-size:16px; font-weight:600; }
.table-heading span { color:var(--color-text-soft); font-size:13px; }
.table-meta { color:var(--color-primary) !important; font:600 12px var(--font-mono); }
.article-table-card :deep(.el-table) { background:transparent; }
.article-table-card :deep(.el-table__header th) { height:50px; color:var(--color-text-muted); background:var(--surface-pages-well); font-size:12px; font-weight:600; }
.article-table-card :deep(.el-table__row td) { height:64px; background:var(--color-surface); border-bottom-color:var(--color-border); }
.article-table-card :deep(.el-table__row:hover td) { background:var(--surface-primary-tint) !important; }
.article-title { color:var(--color-text); font-weight:500; }
.row-actions { display:flex; align-items:center; gap:4px; }
.row-actions :deep(.el-button) { min-height:32px; padding:4px 8px; }
@media(max-width:767px){.dash-header{align-items:stretch;flex-direction:column;padding:22px 20px}.dash-actions{justify-content:space-between}.dash-actions .el-button{flex:1}.admin-dashboard-page{padding-top:16px}.article-table-card :deep(.el-card__header){padding:16px}.table-heading div{display:block}.table-heading div span{display:block;margin-top:4px}}
</style>
