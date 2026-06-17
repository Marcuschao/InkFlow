<template>
  <div class="hot-search-page ds-page">
    <div class="container">
      <header class="ds-page-hero hot-page-hero">
        <h1 class="ds-page-title ds-page-title-lg">全网热搜</h1>
        <p class="ds-page-sub">聚合各平台实时热搜榜单</p>
      </header>

      <section class="hot-panel ds-brutal-surface">
        <div class="platform-search-bar">
          <n-input
            v-model:value="platformKeyword"
            class="platform-input"
            type="text"
            :placeholder="searchPlaceholder"
            clearable
            @keyup.enter="goPlatformSearch"
          />
          <button type="button" class="platform-search-btn" aria-label="搜索" @click="goPlatformSearch">
            <n-icon :component="SearchOutline" :size="20" />
          </button>
        </div>

        <div v-if="sources.length" class="platform-tabs" role="tablist" aria-label="热搜平台">
          <button
            v-for="src in sources"
            :key="src.id"
            type="button"
            role="tab"
            class="platform-tab"
            :class="{ active: activeSource === src.id }"
            :aria-selected="activeSource === src.id"
            @click="onTabChange(src.id)"
          >
            <span class="platform-icon" :style="{ background: getPlatformMeta(src.id).color }">
              {{ getPlatformMeta(src.id).short }}
            </span>
            <span class="platform-tab-name">{{ src.name }}</span>
          </button>
        </div>

        <n-alert v-if="listErr" type="error" class="state-err">{{ listErr }}</n-alert>

        <div v-if="loading" class="hot-grid hot-grid--loading">
          <n-skeleton v-for="n in 14" :key="n" height="52px" :sharp="false" />
        </div>
        <n-empty v-else-if="!items.length" description="暂无热搜数据" />
        <ol v-else class="hot-grid">
          <li v-for="item in items" :key="item.rank + '-' + item.title" class="hot-item ds-brutal-surface">
            <span class="hot-rank" :class="rankClass(item.rank)">{{ item.rank }}</span>
            <a :href="item.url" target="_blank" rel="noopener noreferrer" class="hot-title">{{ item.title }}</a>
            <span v-if="item.heat" class="hot-heat">
              <n-icon :component="FlameOutline" :size="14" />
              {{ item.heat }}
            </span>
          </li>
        </ol>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NAlert, NEmpty, NIcon, NInput, NSkeleton } from 'naive-ui';
import { FlameOutline, SearchOutline } from '@vicons/ionicons5';
import { fetchHotSearchList, fetchHotSearchSources } from '../api/hotSearch';
import { buildPlatformSearchUrl, getPlatformMeta } from '../constants/hotSearchPlatforms';

const route = useRoute();
const router = useRouter();

const sources = ref([]);
const activeSource = ref('');
const items = ref([]);
const loading = ref(false);
const listErr = ref('');
const platformKeyword = ref('');

const activeSourceName = computed(() => {
  const src = sources.value.find((s) => s.id === activeSource.value);
  return src?.name || '平台';
});

const searchPlaceholder = computed(() => `在 ${activeSourceName.value} 中搜索…`);

function rankClass(rank) {
  if (rank === 1) return 'hot-rank--1';
  if (rank === 2) return 'hot-rank--2';
  if (rank === 3) return 'hot-rank--3';
  return 'hot-rank--default';
}

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

function goPlatformSearch() {
  const url = buildPlatformSearchUrl(activeSource.value, platformKeyword.value);
  if (!url) return;
  window.open(url, '_blank', 'noopener,noreferrer');
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
.hot-page-hero {
  margin-bottom: var(--space-6);
}

.hot-panel {
  padding: var(--space-6);
}

.platform-search-bar {
  display: flex;
  align-items: stretch;
  gap: 0;
  border: var(--border-brutal);
  border-radius: var(--radius-brutal-btn);
  box-shadow: var(--shadow-brutal-sm);
  overflow: hidden;
  background: var(--color-surface);
}

.platform-input {
  flex: 1;
  min-width: 0;
}

.platform-input :deep(.n-input) {
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
}

.platform-input :deep(.n-input-wrapper) {
  min-height: 48px;
}

.platform-search-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  flex-shrink: 0;
  border: none;
  border-left: var(--border-brutal);
  background: var(--color-accent);
  color: var(--color-on-primary);
  cursor: pointer;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.platform-search-btn:hover {
  transform: translate(1px, 1px);
}

.platform-search-btn:active {
  transform: translate(2px, 2px);
  box-shadow: none;
}

.platform-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-5);
  margin-bottom: var(--space-5);
}

.platform-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border: var(--border-brutal);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  box-shadow: var(--shadow-brutal-sm);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast),
    background var(--transition-fast),
    color var(--transition-fast);
}

.platform-tab:hover {
  transform: translate(1px, 1px);
  box-shadow: none;
  color: var(--color-text);
}

.platform-tab.active {
  background: var(--surface-primary-tint);
  color: var(--color-text);
  box-shadow: var(--shadow-brutal-sm);
}

.platform-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  color: #ffffff;
  flex-shrink: 0;
}

.platform-tab-name {
  white-space: nowrap;
}

.hot-grid {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  align-content: start;
}

.hot-grid--loading {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.hot-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  min-width: 0;
  min-height: 52px;
  box-shadow: var(--shadow-brutal-sm);
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.hot-item:hover {
  transform: translate(2px, 2px);
  box-shadow: none;
}

.hot-rank {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  border: var(--border-brutal);
}

.hot-rank--1 {
  background: #ef4444;
  color: #ffffff;
}

.hot-rank--2 {
  background: #f97316;
  color: #ffffff;
}

.hot-rank--3 {
  background: var(--color-accent);
  color: var(--color-on-primary);
}

.hot-rank--default {
  background: var(--surface-muted);
  color: var(--color-text-muted);
}

.hot-title {
  flex: 1;
  min-width: 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-title:hover {
  color: var(--color-text);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.hot-heat {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--color-accent-orange);
  white-space: nowrap;
}

.state-err {
  margin-bottom: var(--space-4);
}

@media (max-width: 767px) {
  .hot-panel {
    padding: var(--space-4);
  }

  .hot-grid,
  .hot-grid--loading {
    grid-template-columns: 1fr;
  }

  .platform-tabs {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: var(--space-1);
    -webkit-overflow-scrolling: touch;
  }
}
</style>
