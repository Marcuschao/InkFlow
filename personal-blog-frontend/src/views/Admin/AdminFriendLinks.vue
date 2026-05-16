<template>
  <div class="admin-links-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header">
        <div>
          <h1 class="ds-page-title">友情链接</h1>
          <p class="ds-page-sub">前台 /links 页面数据源</p>
        </div>
        <router-link to="/admin" class="ds-btn ds-btn--secondary ds-btn--pill">返回管理</router-link>
      </header>
      <div class="form-grid ds-table-shell">
        <h2 class="subh">{{ editingId ? '编辑' : '新增' }}</h2>
        <div class="row">
          <label class="ds-form-label">名称</label>
          <input v-model="form.name" class="ds-input" type="text" />
        </div>
        <div class="row">
          <label class="ds-form-label">URL</label>
          <input v-model="form.url" class="ds-input" type="url" />
        </div>
        <div class="row">
          <label class="ds-form-label">Logo URL</label>
          <input v-model="form.logo" class="ds-input" type="url" placeholder="可选" />
        </div>
        <div class="row two">
          <div>
            <label class="ds-form-label">排序</label>
            <input v-model.number="form.sortOrder" class="ds-input" type="number" />
          </div>
          <div>
            <label class="ds-form-label">状态 1启用 0停用</label>
            <input v-model.number="form.status" class="ds-input" type="number" />
          </div>
        </div>
        <div class="actions">
          <button type="button" class="ds-btn ds-btn--primary" @click="save">{{ editingId ? '更新' : '创建' }}</button>
          <button v-if="editingId" type="button" class="ds-btn ds-btn--ghost" @click="resetForm">取消编辑</button>
        </div>
      </div>
      <div class="table-wrap ds-table-shell">
        <table>
          <thead>
            <tr>
              <th>排序</th>
              <th>名称</th>
              <th>URL</th>
              <th>状态</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>{{ row.sortOrder }}</td>
              <td>{{ row.name }}</td>
              <td class="td-url">{{ row.url }}</td>
              <td>{{ row.status }}</td>
              <td class="actions-col">
                <button type="button" class="ds-btn ds-btn--secondary" @click="edit(row)">编辑</button>
                <button type="button" class="ds-btn ds-btn--danger" @click="remove(row.id)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import {
  fetchAdminFriendLinks,
  createFriendLink,
  updateFriendLink,
  deleteFriendLink,
} from '../../api/links';

const rows = ref([]);
const editingId = ref(null);
const form = ref({
  name: '',
  url: '',
  logo: '',
  sortOrder: 0,
  status: 1,
});

async function load() {
  const res = await fetchAdminFriendLinks();
  rows.value = Array.isArray(res.data) ? res.data : [];
}

function resetForm() {
  editingId.value = null;
  form.value = { name: '', url: '', logo: '', sortOrder: 0, status: 1 };
}

function edit(row) {
  editingId.value = row.id;
  form.value = {
    name: row.name,
    url: row.url,
    logo: row.logo || '',
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
  };
}

async function save() {
  const payload = { ...form.value };
  if (editingId.value) {
    await updateFriendLink(editingId.value, payload);
  } else {
    await createFriendLink(payload);
  }
  resetForm();
  await load();
}

async function remove(id) {
  if (!confirm('删除该友链？')) return;
  await deleteFriendLink(id);
  await load();
}

onMounted(load);
</script>

<style scoped>
.form-grid {
  padding: 1.25rem;
  margin-bottom: 1.5rem;
  max-width: 42rem;
}

.subh {
  margin: 0 0 1rem;
  font-size: 1rem;
}

.row {
  margin-bottom: 0.85rem;
}

.row.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.actions {
  display: flex;
  gap: 0.65rem;
  margin-top: 0.5rem;
}

.table-wrap {
  overflow-x: auto;
}

.td-url {
  max-width: 280px;
  word-break: break-all;
  font-size: 0.82rem;
}

.actions-col {
  white-space: nowrap;
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}
</style>
