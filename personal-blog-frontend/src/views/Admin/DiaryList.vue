<template>
  <div class="diary-list-page admin-page">
    <div class="container">
      <header class="list-head ds-admin-header">
        <div>
          <h1 class="ds-page-title">日记列表</h1>
          <p class="ds-page-sub">按月份归档，支持筛选</p>
        </div>
        <div class="head-actions">
          <router-link to="/admin/diary" class="ds-btn ds-btn--primary ds-btn--pill">写日记</router-link>
          <router-link to="/admin" class="ds-btn ds-btn--secondary ds-btn--pill">返回管理</router-link>
        </div>
      </header>

      <div class="filters">
        <label class="fil">
          <span>月份</span>
          <input v-model="monthFilter" type="month" class="ds-input" @change="reload" />
        </label>
        <label class="fil">
          <span>标签</span>
          <input v-model="tagFilter" type="text" class="ds-input" placeholder="可选" @keyup.enter="reload" />
        </label>
        <button type="button" class="ds-btn ds-btn--ghost ds-btn--pill" @click="clearFilters">清空</button>
      </div>

      <p v-if="err" class="err">{{ err }}</p>
      <div v-if="loading" class="sk-list">
        <div v-for="n in 4" :key="n" class="ui-skeleton sk-row" />
      </div>
      <div v-else-if="!records.length" class="ds-empty-panel">暂无日记，去写一篇吧</div>
      <div v-else class="month-acc">
        <section v-for="block in grouped" :key="block.key" class="month-block">
          <button type="button" class="month-head" @click="toggle(block.key)">
            <span>{{ block.label }}</span>
            <span class="cnt">{{ block.rows.length }} 条</span>
            <span class="chev">{{ expanded.has(block.key) ? '▼' : '▶' }}</span>
          </button>
          <ul v-show="expanded.has(block.key)" class="entry-list">
            <li v-for="row in block.rows" :key="row.id" class="entry">
              <router-link :to="'/admin/diary/' + row.id" class="entry-main">
                <time class="d">{{ row.diaryDate }}</time>
                <span class="t">{{ row.title || '未命名' }}</span>
                <span v-if="row.tags" class="tg">{{ row.tags }}</span>
                <span v-if="Number(row.isPublic) === 1" class="pub">公开</span>
              </router-link>
              <div class="entry-act">
                <router-link :to="'/admin/diary/edit/' + row.id" class="ds-btn ds-btn--secondary ds-btn--pill sm">编辑</router-link>
                <button type="button" class="ds-btn ds-btn--danger ds-btn--pill sm" @click="remove(row.id)">删除</button>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <Pagination
        v-if="total > 0"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @changePage="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import Pagination from '../../components/Pagination.vue';
import { listDiaries, deleteDiary } from '../../api/diary';

const records = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const loading = ref(false);
const err = ref('');
const monthFilter = ref('');
const tagFilter = ref('');
const expanded = ref(new Set());

const grouped = computed(() => {
  const map = new Map();
  for (const row of records.value) {
    const d = (row.diaryDate || '').toString().slice(0, 10);
    const key = d.slice(0, 7);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(row);
  }
  const keys = [...map.keys()].sort((a, b) => b.localeCompare(a));
  return keys.map((key) => ({
    key,
    label: key,
    rows: map.get(key),
  }));
});

function toggle(key) {
  const next = new Set(expanded.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  expanded.value = next;
}

async function load() {
  loading.value = true;
  err.value = '';
  try {
    const params = { page: page.value, size: pageSize.value };
    if (monthFilter.value) params.month = monthFilter.value;
    if (tagFilter.value.trim()) params.tag = tagFilter.value.trim();
    const res = await listDiaries(params);
    const data = res.data;
    records.value = data?.records || [];
    total.value = Number(data?.total) || 0;
    const keys = [...new Set(records.value.map((r) => (r.diaryDate || '').toString().slice(0, 7)))].sort((a, b) =>
      b.localeCompare(a)
    );
    expanded.value = new Set(keys);
  } catch (e) {
    err.value = e?.message || '加载失败';
    records.value = [];
  } finally {
    loading.value = false;
  }
}

function reload() {
  page.value = 1;
  load();
}

function clearFilters() {
  monthFilter.value = '';
  tagFilter.value = '';
  reload();
}

function onPage(p) {
  page.value = p;
  load();
}

async function remove(id) {
  if (!confirm('确定删除这篇日记？')) return;
  try {
    await deleteDiary(id);
    await load();
  } catch {
    /* toast */
  }
}

onMounted(load);
</script>

<style scoped>
.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  align-items: flex-end;
  margin-bottom: var(--space-6);
}

.fil {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.err {
  color: var(--color-danger);
  margin-bottom: var(--space-3);
}

.sk-row {
  height: 3rem;
  margin-bottom: var(--space-3);
}

.month-head {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--admin-panel-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  cursor: pointer;
  font-weight: var(--weight-semibold);
}

.month-head .cnt {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  font-weight: 500;
}

.month-head .chev {
  margin-left: auto;
  color: var(--color-text-soft);
}

.month-block + .month-block {
  margin-top: var(--space-3);
}

.entry-list {
  list-style: none;
  margin: 0;
  padding: var(--space-2) 0 0 var(--space-2);
}

.entry {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--color-border);
}

.entry-main {
  flex: 1;
  min-width: 200px;
  text-decoration: none;
  color: inherit;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-3);
}

.entry-main .d {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
}

.entry-main .t {
  font-weight: var(--weight-semibold);
}

.entry-main .tg {
  font-size: var(--text-xs);
  color: var(--color-text-soft);
}

.pub {
  font-size: var(--text-xs);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-pill);
  background: var(--surface-primary-tint);
  color: var(--color-primary);
}

.entry-act {
  display: flex;
  gap: var(--space-2);
}

.sm {
  padding: 0.35rem 0.65rem;
  font-size: var(--text-xs);
}
</style>
