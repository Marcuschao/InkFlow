<template>
  <div class="hot-search-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-lg">全网热搜</h1>
        <p class="ds-page-sub">聚合各平台实时热搜榜单</p>
      </header>

      <div class="site-search-bar">
        <n-input
          v-model:value="siteKeyword"
          type="text"
          placeholder="站内搜索文章…"
          clearable
          @keyup.enter="goSiteSearch"
        />
        <n-button type="primary" @click="goSiteSearch">站内搜索</n-button>
      </div>

      <n-tabs
        v-if="sources.length"
        type="line"
        :value="activeSource"
        class="hot-tabs"
        @update:value="onTabChange"
      >
        <n-tab-pane
          v-for="src in sources"
          :key="src.id"
          :name="src.id"
          :tab="src.name"
        />
      </n-tabs>

      <n-alert v-if="listErr" type="error" class="state-err">{{ listErr }}</n-alert>

      <n-skeleton v-if="loading" text :repeat="12" />
      <n-empty v-else-if="!items.length" description="暂无热搜数据" />
      <ol v-else class="hot-full-list">
        <li v-for="item in items" :key="item.rank + '-' + item.title" class="hot-full-item">
          <span class="hot-rank" :class="{ top: item.rank <= 3 }">{{ item.rank }}</span>
          <a :href="item.url" target="_blank" rel="noopener noreferrer" class="hot-title">{{ item.title }}</a>
          <span v-if="item.heat" class="hot-heat">{{ item.heat }}</span>
        </li>
      </ol>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NAlert, NButton, NEmpty, NInput, NSkeleton, NTabPane, NTabs } from 'naive-ui';
import { fetchHotSearchList, fetchHotSearchSources } from '../api/hotSearch';

const route = useRoute();
const router = useRouter();

const sources = ref([]);
const activeSource = ref('');
const items = ref([]);
const loading = ref(false);
const listErr = ref('');
const siteKeyword = ref('');

async function loadSources() {
  try {
    const res = await fetchHotSearchSources();
    sources.value = res.data || [];
    if (!activeSource.value && sources.value.length) {
      const q = route.query.source;
      activeSource.value = sources.value.some((s) => s.id === q) ? q : sources.value[0].id;
    }
  } catch {
    sources.value = [];
  }
}

async function loadList(sourceId) {
  if (!sourceId) return;
  loading.value = true;
  listErr.value = '';
  try {
    const res = await fetchHotSearchList({ source: sourceId });
    items.value = res.data?.items || [];
  } catch (e) {
    items.value = [];
    listErr.value = e?.message || '加载失败';
  } finally {
    loading.value = false;
  }
}

function onTabChange(sourceId) {
  activeSource.value = sourceId;
  router.replace({ path: '/hot-search', query: { source: sourceId } });
}

function goSiteSearch() {
  const q = siteKeyword.value.trim();
  if (!q) return;
  router.push({ path: '/search', query: { q } });
}

watch(
  () => route.query.source,
  (q) => {
    if (typeof q === 'string' && sources.value.some((s) => s.id === q)) {
      activeSource.value = q;
    }
  }
);

watch(activeSource, (id) => {
  if (id) loadList(id);
});

onMounted(async () => {
  await loadSources();
  if (route.query.source && sources.value.some((s) => s.id === route.query.source)) {
    activeSource.value = route.query.source;
  } else if (sources.value.length) {
    activeSource.value = sources.value[0].id;
  }
});
</script>

<style scoped>
.site-search-bar {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.site-search-bar :deep(.n-input) {
  flex: 1;
}

.hot-tabs {
  margin-bottom: var(--space-4);
}

.hot-full-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.hot-full-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.hot-rank {
  flex-shrink: 0;
  width: 24px;
  text-align: center;
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
}

.hot-rank.top {
  color: var(--color-primary);
}

.hot-title {
  flex: 1;
  min-width: 0;
  font-size: var(--text-base);
  color: var(--color-text);
  text-decoration: none;
}

.hot-title:hover {
  color: var(--color-primary);
}

.hot-heat {
  flex-shrink: 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.state-err {
  margin-bottom: var(--space-4);
}
</style>
