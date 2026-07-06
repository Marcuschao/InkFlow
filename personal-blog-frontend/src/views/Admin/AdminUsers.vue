<template>
  <div class="admin-users-page admin-page">
    <div class="container">
      <header class="dash-header ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">用户管理</h1>
          <p class="ds-page-sub">注册用户列表</p>
        </div>
        <router-link to="/admin">
          <n-button>返回管理</n-button>
        </router-link>
      </header>

      <n-card :bordered="true" style="margin-bottom: 24px;">
        <n-space class="filters" align="center" :size="16">
          <div class="fil" style="display: flex; align-items: center;">
            <span style="margin-right: 8px;">关键词</span>
            <n-input
              v-model:value="keyword"
              placeholder="用户名 / 邮箱 / 昵称"
              style="width: 200px;"
              @keyup.enter="reload"
            />
          </div>
          <div class="fil" style="display: flex; align-items: center;">
            <span style="margin-right: 8px;">角色</span>
            <n-select v-model:value="roleFilter" style="width: 120px;" :options="roleOptions" @update:value="reload" />
          </div>
          <n-button type="primary" @click="reload">筛选</n-button>
        </n-space>
      </n-card>

      <n-card :bordered="true">
        <n-data-table
          :columns="columns"
          :data="rows"
          :bordered="false"
          :single-line="false"
          :scroll-x="1100"
        />
      </n-card>

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
import { ref, onMounted, h } from 'vue';
import { NButton, NCard, NDataTable, NInput, NSelect, NSpace, NTag } from 'naive-ui';
import Pagination from '../../components/Pagination.vue';
import { fetchAdminUsers } from '../../api/adminUser';
import { formatShortDateTime } from '../../utils/format';

const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const keyword = ref('');
const roleFilter = ref(null);

const roleOptions = [
  { label: '全部', value: null },
  { label: '管理员', value: 'ADMIN' },
  { label: '普通用户', value: 'USER' },
];

const columns = [
  { title: 'ID', key: 'id', width: 64 },
  { title: '用户名', key: 'username', width: 120, ellipsis: { tooltip: true } },
  { title: '昵称', key: 'nickname', width: 100, ellipsis: { tooltip: true }, render: row => row.nickname || '—' },
  { title: '邮箱', key: 'email', width: 180, ellipsis: { tooltip: true } },
  {
    title: '角色',
    key: 'role',
    width: 90,
    render(row) {
      if (row.role === 'ADMIN') return h(NTag, { type: 'warning', bordered: false }, () => '管理员');
      return h(NTag, { type: 'default', bordered: false }, () => '用户');
    },
  },
  {
    title: '注册时间',
    key: 'createTime',
    width: 130,
    render: row => formatShortDateTime(row.createTime),
  },
  { title: '注册地区', key: 'registerRegion', width: 100, ellipsis: { tooltip: true }, render: row => row.registerRegion || '—' },
  {
    title: '最近登录',
    key: 'lastLoginTime',
    width: 130,
    render: row => formatShortDateTime(row.lastLoginTime) || '—',
  },
  { title: '登录地区', key: 'lastLoginRegion', width: 100, ellipsis: { tooltip: true }, render: row => row.lastLoginRegion || '—' },
];

async function reload() {
  const res = await fetchAdminUsers({
    page: page.value,
    size: pageSize.value,
    keyword: keyword.value.trim() || undefined,
    role: roleFilter.value || undefined,
  });
  rows.value = res.data?.records || [];
  total.value = Number(res.data?.total) || 0;
}

function onPage(p) {
  page.value = p;
  reload();
}

onMounted(reload);
</script>
