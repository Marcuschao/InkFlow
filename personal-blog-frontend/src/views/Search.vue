<template>
  <div class="search-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-lg">搜索</h1>
        <p class="ds-page-sub">在全站已发布文章中查找关键词</p>
      </header>
      <div class="search-bar-wrap">
        <input
          v-model.trim="keyword"
          type="search"
          class="ds-input search-input"
          placeholder="输入关键词…"
          autocomplete="off"
          @keyup.enter="runSearch"
        />
        <button type="button" class="ds-btn ds-btn--primary" @click="runSearch">搜索</button>
      </div>
      <p v-if="listErr" class="state-err">{{ listErr }}</p>
      <div v-if="loading" class="sk-grid">
        <div v-for="n in 5" :key="'s-' + n" class="ui-skeleton sk-card" />
      </div>
      <div v-else-if="!hits.length && searched" class="ds-empty-panel empty-box">
        <p>没有找到匹配的文章</p>
      </div>
      <div v-else class="hit-list">
        <article v-for="h in hits" :key="h.id" class="hit-card">
          <router-link :to="'/article/' + h.id" class="hit-link">
            <h2 class="hit-title" v-html="h.highlightTitle"></h2>
            <p class="hit-sum" v-html="h.highlightSummary || displaySummary(h)"></p>
            <time class="hit-time">{{ formatDate(h.createTime) }}</time>
          </router-link>
        </article>
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
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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
const listErr = ref('');

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
    hits.value = data?.records || [];
    total.value = Number(data?.total) || 0;
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
  gap: 0.65rem;
  margin-bottom: 1.75rem;
  justify-content: center;
}

.search-input {
  flex: 1 1 240px;
  max-width: 420px;
}

.sk-grid {
  display: grid;
  gap: 0.75rem;
}

.sk-card {
  height: 5.5rem;
  border-radius: var(--radius-lg);
}

.empty-box {
  margin-top: 1rem;
}

.hit-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.hit-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
}

.hit-link {
  display: block;
  padding: 1.15rem 1.25rem;
  text-decoration: none;
  color: inherit;
}

.hit-link:hover .hit-title {
  color: var(--color-primary);
}

.hit-title {
  margin: 0 0 0.45rem;
  font-size: 1.12rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--color-text);
}

.hit-title :deep(mark) {
  background: rgba(250, 204, 21, 0.35);
  padding: 0 0.15em;
  border-radius: 3px;
}

.hit-sum {
  margin: 0 0 0.65rem;
  font-size: 0.88rem;
  color: var(--color-text-muted);
  line-height: 1.55;
}

.hit-sum :deep(mark) {
  background: rgba(250, 204, 21, 0.35);
  padding: 0 0.15em;
  border-radius: 3px;
}

.hit-time {
  font-size: 0.78rem;
  color: var(--color-text-soft);
  font-variant-numeric: tabular-nums;
}

.state-err {
  color: var(--color-danger);
  text-align: center;
  margin-bottom: 1rem;
}
</style>
