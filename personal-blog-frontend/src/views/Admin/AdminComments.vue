<template>
  <div class="admin-comments-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">评论审核</h1>
          <p class="ds-page-sub">待审核与已通过</p>
        </div>
        <router-link to="/admin" class="ds-btn ds-btn--secondary ds-btn--pill">返回管理</router-link>
      </header>
      <div class="filters">
        <label>
          状态
          <select v-model.number="statusFilter" class="ds-input" @change="reload">
            <option :value="null">全部</option>
            <option :value="0">待审核</option>
            <option :value="1">已通过</option>
          </select>
        </label>
      </div>
      <div class="ds-table-shell">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>文章</th>
              <th>作者</th>
              <th>内容</th>
              <th>状态</th>
              <th>时间</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in rows" :key="c.id">
              <td>{{ c.id }}</td>
              <td>{{ c.articleId }}</td>
              <td>{{ c.author }}</td>
              <td class="td-content">{{ c.content }}</td>
              <td>{{ c.status }}</td>
              <td>{{ formatTime(c.createTime) }}</td>
              <td class="act">
                <button
                  v-if="c.status === 0"
                  type="button"
                  class="ds-btn ds-btn--green ds-btn--pill"
                  @click="approve(c.id)"
                >
                  通过
                </button>
                <button type="button" class="ds-btn ds-btn--danger ds-btn--pill" @click="remove(c.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <Pagination
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @changePage="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import Pagination from '../../components/Pagination.vue';
import { fetchAdminComments, approveComment, deleteAdminComment } from '../../api/comments';

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const statusFilter = ref(null);

function formatTime(t) {
  if (!t) return '';
  return new Date(t).toLocaleString();
}

async function reload() {
  const params = { page: page.value, size: pageSize.value };
  if (statusFilter.value !== null) params.status = statusFilter.value;
  const res = await fetchAdminComments(params);
  const data = res.data;
  rows.value = data?.records || [];
  total.value = Number(data?.total) || 0;
}

function onPage(p) {
  page.value = p;
  reload();
}

async function approve(id) {
  await approveComment(id);
  await reload();
}

async function remove(id) {
  if (!confirm('删除该评论？')) return;
  await deleteAdminComment(id);
  await reload();
}

onMounted(reload);
</script>

<style scoped>
.filters {
  margin-bottom: 1rem;
}

.filters label {
  font-size: 0.88rem;
  color: var(--color-text-muted);
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.td-content {
  max-width: 280px;
  word-break: break-word;
  font-size: 0.82rem;
}

.act {
  white-space: nowrap;
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}
</style>
