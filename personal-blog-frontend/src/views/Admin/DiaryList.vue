<template>
  <div class="diary-list-page">
    <div class="container">
      <header class="list-head" style="margin-bottom: 24px; display: flex; align-items: flex-start; justify-content: space-between; gap: 16px;">
        <div>
          <h1 class="ds-page-title">我的分享</h1>
          <p class="ds-page-sub">按月份归档，支持筛选</p>
        </div>
        <router-link to="/share-write">
          <n-button type="primary">写分享</n-button>
        </router-link>
      </header>

      <n-card :bordered="true" class="filter-card">
        <n-grid cols="1 s:2" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
          <n-gi span="24 s:12">
            <n-form-item label="月份" label-placement="top" :show-feedback="false">
              <n-date-picker
                v-model:formatted-value="monthFilter"
                value-format="yyyy-MM"
                type="month"
                clearable
                class="filter-control"
                @update:formatted-value="reload"
              />
            </n-form-item>
          </n-gi>
          <n-gi span="24 s:12">
            <n-form-item label="标签" label-placement="top" :show-feedback="false">
              <n-input
                v-model:value="tagFilter"
                placeholder="可选"
                clearable
                class="filter-control"
                @keyup.enter="reload"
              />
            </n-form-item>
          </n-gi>
        </n-grid>
        <div class="filter-actions">
          <n-button secondary @click="clearFilters">清空</n-button>
        </div>
      </n-card>

      <n-alert v-if="err" type="error" class="err" style="margin-bottom: 16px;">{{ err }}</n-alert>
      
      <n-skeleton v-if="loading" height="120px" :sharp="false" style="margin-bottom: 16px;" />
      
      <n-empty v-else-if="!records.length" description="暂无分享，去写一篇吧">
        <template #extra>
          <router-link to="/share-write">
            <n-button type="primary">去写分享</n-button>
          </router-link>
        </template>
      </n-empty>

      <div v-else class="month-acc">
        <n-collapse :default-expanded-names="Array.from(expanded)" @item-header-click="handleCollapseClick">
          <n-collapse-item v-for="block in grouped" :key="block.key" :title="block.label + ' (' + block.rows.length + ' 条)'" :name="block.key">
            <n-list hoverable clickable>
              <n-list-item v-for="row in block.rows" :key="row.id">
                <template #prefix>
                  <time class="d" style="font-family: monospace; color: var(--color-text-muted);">{{ row.diaryDate }}</time>
                </template>
                <div class="entry-main" style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
                  <router-link :to="'/my-shares/' + row.id" class="t" style="text-decoration: none; color: var(--color-text); font-weight: 600;">
                    {{ row.title || '未命名' }}
                  </router-link>
                  <n-space :size="8">
                    <n-tag v-if="row.tags" size="small" :bordered="false">{{ row.tags }}</n-tag>
                    <n-tag v-if="Number(row.isPublic) === 1" size="small" type="success" :bordered="false">公开</n-tag>
                  </n-space>
                </div>
                <template #suffix>
                  <n-space :size="8">
                    <router-link :to="'/share-write/edit/' + row.id">
                      <n-button size="small" secondary>编辑</n-button>
                    </router-link>
                    <n-button size="small" type="error" secondary @click="remove(row.id)">删除</n-button>
                  </n-space>
                </template>
              </n-list-item>
            </n-list>
          </n-collapse-item>
        </n-collapse>
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
import {
  NAlert,
  NButton,
  NCard,
  NCollapse,
  NCollapseItem,
  NDatePicker,
  NEmpty,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NList,
  NListItem,
  NSkeleton,
  NSpace,
  NTag,
} from 'naive-ui';
import Pagination from '../../components/Pagination.vue';
import { listDiaries, deleteDiary } from '../../api/diary';

const records = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);
const loading = ref(false);
const err = ref('');
const monthFilter = ref(null);
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

function handleCollapseClick({ name, expanded: isExpanded }) {
  const next = new Set(expanded.value);
  if (isExpanded) next.add(name);
  else next.delete(name);
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
  monthFilter.value = null;
  tagFilter.value = '';
  reload();
}

function onPage(p) {
  page.value = p;
  load();
}

async function remove(id) {
  if (!confirm('确定删除这篇分享？')) return;
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
.filter-card {
  margin-bottom: var(--space-6);
}

.filter-control {
  width: 100%;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-2);
}

@media (max-width: 767px) {
  .filter-actions {
    justify-content: stretch;
  }

  .filter-actions :deep(.n-button) {
    width: 100%;
  }
}
</style>
