<template>
  <div class="search-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-lg">搜索</h1>
        <p class="ds-page-sub">在全站已发布文章中查找关键词</p>
      </header>
      <div class="search-bar-wrap">
        <n-input
          v-model:value="keyword"
          type="text"
          placeholder="输入关键词…"
          clearable
          @keyup.enter="runSearch"
        />
        <n-button type="primary" @click="runSearch">搜索</n-button>
      </div>
      <n-space v-if="relatedTags.length" class="related-tags" :size="8">
        <span class="related-label">知识星系：</span>
        <router-link
          v-for="t in relatedTags"
          :key="t.tagId"
          :to="{ path: '/tags', query: { focus: t.tagId } }"
        >
          <n-tag size="small" type="primary" :bordered="false">{{ t.name }}</n-tag>
        </router-link>
      </n-space>
      <n-alert v-if="listErr" type="error" class="state-err">{{ listErr }}</n-alert>
      <n-grid v-if="loading" :cols="1" :y-gap="12">
        <n-gi v-for="n in 5" :key="'s-' + n">
          <n-skeleton height="88px" :sharp="false" />
        </n-gi>
      </n-grid>
      <n-empty v-else-if="!hits.length && searched" description="没有找到匹配的文章" />
      <n-grid v-else :cols="1" :y-gap="16">
        <n-gi v-for="h in hits" :key="h.id">
          <n-card hoverable>
            <router-link :to="'/article/' + h.id" class="hit-link">
              <h2 class="hit-title" v-html="h.highlightTitle"></h2>
              <p class="hit-sum" v-html="h.highlightSummary || displaySummary(h)"></p>
              <time class="hit-time">{{ formatDate(h.createTime) }}</time>
            </router-link>
          </n-card>
        </n-gi>
      </n-grid>
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
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NAlert, NButton, NCard, NEmpty, NGi, NGrid, NInput, NSkeleton, NSpace, NTag } from 'naive-ui';
import Pagination from '../components/Pagination.vue';
import { searchArticles } from '../api/search';

const route = useRoute();
const router = useRouter();

const keyword = ref((route.query.q || '').toString());
const hits = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const searched = ref(false);
const relatedTags = ref([]);

function formatDate(t) {
  if (!t) return '';
  return new Date(t).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function displaySummary(h) {
  const s = (h.summary || '').trim();
  return s.length > 180 ? `${s.slice(0, 180)}…` : s || '—';
}

async function load() {
  const q = keyword.value.trim();
  listErr.value = '';
  if (!q) {
    hits.value = [];
    total.value = 0;
    searched.value = false;
    return;
  }
  loading.value = true;
  searched.value = true;
  try {
    const res = await searchArticles({ keyword: q, page: page.value, size: pageSize.value });
    const data = res.data;
    const pageData = data?.page || data;
    hits.value = pageData?.records || [];
    total.value = Number(pageData?.total) || 0;
    relatedTags.value = data?.relatedTags || [];
  } catch (e) {
    listErr.value = e?.message || '加载失败';
    hits.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function runSearch() {
  page.value = 1;
  router.replace({ query: { q: keyword.value.trim() || undefined } });
}

function onPage(p) {
  page.value = p;
  router.replace({ query: { ...route.query, q: keyword.value.trim(), page: p > 1 ? p : undefined } });
}

watch(
  () => route.query,
  (q) => {
    keyword.value = (q.q || '').toString();
    const pg = Math.max(1, parseInt(q.page, 10) || 1);
    page.value = pg;
    load();
  },
  { immediate: true }
);
</script>

<style scoped>
.search-bar-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
  justify-content: center;
}

.search-bar-wrap :deep(.n-input) {
  flex: 1 1 240px;
  max-width: 420px;
}

.related-tags {
  margin-bottom: var(--space-6);
  flex-wrap: wrap;
}

.related-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.state-err {
  margin-bottom: var(--space-4);
}

.hit-link {
  display: block;
  text-decoration: none;
  color: inherit;
}

.hit-link:hover .hit-title {
  color: var(--color-primary);
}

.hit-title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.hit-title :deep(mark) {
  background: rgba(250, 204, 21, 0.35);
  padding: 0 0.15em;
  border-radius: 3px;
}

.hit-sum {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  line-height: 1.55;
}

.hit-sum :deep(mark) {
  background: rgba(250, 204, 21, 0.35);
  padding: 0 0.15em;
  border-radius: 3px;
}

.hit-time {
  font-size: var(--text-xs);
  color: var(--color-text-soft);
}
</style>
