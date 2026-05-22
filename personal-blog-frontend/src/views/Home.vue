<template>
  <div class="home-page ds-page">
    <div class="container home-layout">
      <div class="home-main">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-lg">{{ homeTab === 'feed' ? '关注动态' : '最新文章' }}</h1>
        <p class="ds-page-sub">{{ homeTab === 'feed' ? '你关注的人最近 7 天发布的内容' : '探索想法、笔记与技术碎片' }}</p>
      </header>

      <nav v-if="authStore.isLoggedIn" class="home-tabs" aria-label="首页内容">
        <button
          type="button"
          class="home-tab-btn"
          :class="{ active: homeTab === 'latest' }"
          @click="switchHomeTab('latest')"
        >最新</button>
        <button
          type="button"
          class="home-tab-btn"
          :class="{ active: homeTab === 'feed' }"
          @click="switchHomeTab('feed')"
        >动态</button>
      </nav>

      <div v-if="homeTab === 'latest'" class="ds-tag-row">
        <span
          v-for="tag in displayedHomeTags"
          :key="tag.id"
          :class="['ds-tag-pill', { 'is-active': currentTagId === tag.id }]"
          @click="filterByTag(tag.id)"
        >
          {{ tag.name }}
        </span>
        <span
          class="ds-tag-pill tag-all"
          :class="{ 'is-active': currentTagId === null }"
          @click="filterByTag(null)"
        >
          全部
        </span>
      </div>

      <template v-if="homeTab === 'latest'">
      <div v-if="listingLoading" class="article-list skeleton-grid ds-grid-cards">
        <div v-for="n in 6" :key="'sk-' + n" class="card-skeleton">
          <div class="ui-skeleton sk-title" />
          <div class="ui-skeleton sk-line" />
          <div class="ui-skeleton sk-line short" />
          <div class="sk-meta">
            <div class="ui-skeleton sk-dot" />
            <div class="ui-skeleton sk-chip" />
          </div>
        </div>
      </div>
      <p v-else-if="articleStore.listError" class="home-list-err">{{ articleStore.listError }}</p>
      <div v-else-if="!articleStore.articles.length" class="ds-empty-panel home-empty">
        <p>暂无文章</p>
      </div>
      <div v-else class="article-list ds-grid-cards">
        <ArticleCard
          v-for="(article, idx) in articleStore.articles"
          :key="article.id"
          class="card-enter"
          :article="article"
          :style="{ '--stagger': `${Math.min(idx, 8) * 45}ms` }"
        />
      </div>

      <Pagination
        :total="articleStore.pagination.total"
        :page-size="articleStore.pagination.pageSize"
        :current-page="articleStore.pagination.currentPage"
        @changePage="handlePageChange"
      />
      </template>

      <template v-else>
        <div v-if="feedLoading" class="article-list skeleton-grid ds-grid-cards">
          <div v-for="n in 6" :key="'fsk-' + n" class="card-skeleton">
            <div class="ui-skeleton sk-title" />
            <div class="ui-skeleton sk-line" />
            <div class="ui-skeleton sk-line short" />
          </div>
        </div>
        <div v-else-if="!feedArticles.length" class="ds-empty-panel home-empty">
          <p>关注用户后，这里会显示他们的新文章</p>
        </div>
        <div v-else class="article-list ds-grid-cards">
          <ArticleCard
            v-for="(article, idx) in feedArticles"
            :key="article.id"
            class="card-enter"
            :article="article"
            :like-count="article.likeCount"
            :liked="article.liked"
            show-like
            :style="{ '--stagger': `${Math.min(idx, 8) * 45}ms` }"
          />
        </div>
        <Pagination
          v-if="feedTotal > feedSize"
          :total="feedTotal"
          :page-size="feedSize"
          :current-page="feedPage"
          @changePage="handleFeedPageChange"
        />
      </template>
      </div>

      <aside class="home-aside" aria-label="猜你喜欢">
        <div class="aside-card">
          <h2 class="aside-title">猜你喜欢</h2>
          <template v-if="!authStore.isLoggedIn">
            <p class="aside-login-hint">请先登录</p>
          </template>
          <template v-else>
            <div v-if="recLoading" class="rec-skel">
              <div v-for="n in 4" :key="'rsk-' + n" class="rec-skel-row ui-skeleton" />
            </div>
            <div v-else-if="!recItems.length" class="aside-empty">暂无推荐</div>
            <div v-else class="rec-list">
              <ArticleCard
                v-for="(item, ix) in recItems"
                :key="item.id + '-' + ix"
                class="rec-card"
                :article="normalizeRecArticle(item)"
                :reason="item.reason"
              />
            </div>
          </template>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { useArticleStore } from '../stores/article';
import ArticleCard from '../components/ArticleCard.vue';
import Pagination from '../components/Pagination.vue';
import { useRoute, useRouter } from 'vue-router';
import { usePageViewHome } from '../composables/usePageView';
import { useAuthStore } from '../stores/auth';
import { agentRecommendHome } from '../api/agent';
import { fetchFeed } from '../api/interaction';
import { useReadingHistory } from '../composables/useReadingHistory';

usePageViewHome();
const articleStore = useArticleStore();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { getRecentArticleIds } = useReadingHistory();

const currentTagId = ref(null);
const listingLoading = ref(false);
const homeTab = ref('latest');
const feedLoading = ref(false);
const feedArticles = ref([]);
const feedPage = ref(1);
const feedSize = ref(10);
const feedTotal = ref(0);
const recItems = ref([]);
const recLoading = ref(false);

function normalizeRecArticle(item) {
  return {
    ...item,
    tags: item.tags || [],
    createTime: item.createTime ?? item.createdAt,
  };
}

async function loadFeed(page = 1) {
  if (!authStore.isLoggedIn) {
    feedArticles.value = [];
    return;
  }
  feedLoading.value = true;
  feedPage.value = page;
  try {
    const res = await fetchFeed({ page, size: feedSize.value });
    const d = res.data;
    feedArticles.value = d?.records || [];
    feedTotal.value = d?.total || 0;
  } catch {
    feedArticles.value = [];
    feedTotal.value = 0;
  } finally {
    feedLoading.value = false;
  }
}

function switchHomeTab(tab) {
  homeTab.value = tab;
  const query = { ...route.query };
  if (tab === 'feed') {
    query.view = 'feed';
    delete query.page;
    delete query.tag;
  } else {
    delete query.view;
  }
  router.push({ query });
}

function handleFeedPageChange(newPage) {
  router.push({ query: { ...route.query, page: newPage, view: 'feed' } });
}

async function loadRecommendations() {
  if (!authStore.isLoggedIn) {
    recItems.value = [];
    return;
  }
  recLoading.value = true;
  try {
    const list = await agentRecommendHome({ recentArticleIds: getRecentArticleIds() });
    recItems.value = Array.isArray(list) ? list : [];
  } catch {
    recItems.value = [];
  } finally {
    recLoading.value = false;
  }
}

const displayedHomeTags = computed(() => {
  const all = articleStore.tags || [];
  const slice = all.slice(0, 10);
  const tid = currentTagId.value;
  if (tid == null) return slice;
  if (slice.some((t) => t.id === tid)) return slice;
  const sel = all.find((t) => t.id === tid);
  if (!sel) return slice;
  return [...slice.slice(0, 9), sel];
});

const fetchArticlesData = async (page, tagId) => {
  listingLoading.value = true;
  try {
    await articleStore.fetchArticles(page, articleStore.pagination.pageSize, tagId);
  } finally {
    listingLoading.value = false;
  }
};

const handlePageChange = (newPage) => {
  const query = { page: newPage };
  if (currentTagId.value != null) query.tag = currentTagId.value;
  router.push({ query });
};

const filterByTag = (tagId) => {
  currentTagId.value = tagId;
  const query = { page: 1 };
  if (tagId != null) query.tag = tagId;
  router.push({ query });
};

watch(
  () => route.query,
  async (newQuery) => {
    const view = newQuery.view === 'feed' && authStore.isLoggedIn ? 'feed' : 'latest';
    homeTab.value = view;

    if (view === 'feed') {
      const page = Math.max(1, parseInt(newQuery.page, 10) || 1);
      await loadFeed(page);
      return;
    }

    const page = Math.max(1, parseInt(newQuery.page, 10) || 1);
    let tag = null;
    const raw = newQuery.tag;
    if (raw !== undefined && raw !== null && raw !== '') {
      const t = parseInt(raw, 10);
      tag = Number.isNaN(t) ? null : t;
    }
    currentTagId.value = tag;
    await fetchArticlesData(page, tag);
  },
  { immediate: true }
);

onMounted(() => {
  articleStore.fetchTags();
});

watch(
  () => authStore.isLoggedIn,
  () => loadRecommendations(),
  { immediate: true }
);
</script>

<style scoped>
.home-layout {
  display: grid;
  gap: var(--space-8);
  align-items: start;
}

.home-main {
  min-width: 0;
}

.home-tabs {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-6);
}

.home-tab-btn {
  border: none;
  background: none;
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
  cursor: pointer;
  font-family: inherit;
  border-radius: var(--radius-sm);
}

.home-tab-btn.active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.home-aside {
  position: sticky;
  top: calc(var(--layout-navbar-bottom) + var(--space-4));
}

.aside-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 1.15rem 1.05rem 1.25rem;
}

.aside-title {
  margin: 0 0 1rem;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}

.rec-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.rec-list :deep(.article-card) {
  min-height: 0;
}

.rec-list :deep(.title) {
  font-size: 1.02rem;
}

.rec-list :deep(.excerpt) {
  font-size: 0.85rem;
  min-height: 2.6em;
}

.rec-skel {
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.rec-skel-row {
  height: 5.5rem;
  border-radius: var(--radius-lg);
}

.aside-empty {
  font-size: var(--text-88);
  color: var(--color-text-muted);
}

.aside-login-hint {
  margin: 0;
  font-size: var(--text-88);
  color: var(--color-text-muted);
}

.skeleton-grid .card-skeleton {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 1.35rem 1.5rem;
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.sk-title {
  height: 1.35rem;
  width: 72%;
  margin-bottom: 1rem;
}

.sk-line {
  height: 0.75rem;
  width: 100%;
  margin-bottom: 0.55rem;
}

.sk-line.short {
  width: 55%;
  margin-bottom: 1.1rem;
}

.sk-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.sk-dot {
  width: 3.5rem;
  height: 0.65rem;
  border-radius: var(--radius-pill);
}

.sk-chip {
  width: 3rem;
  height: 1.35rem;
  border-radius: var(--radius-pill);
}

.card-enter {
  animation: fade-in-soft 0.5s var(--ease-out-soft) both;
  animation-delay: var(--stagger, 0ms);
}

.home-list-err {
  text-align: center;
  color: var(--color-danger);
  margin-bottom: 1rem;
}

.home-empty {
  margin-bottom: 2rem;
}

@media (min-width: 1024px) {
  .home-layout {
    grid-template-columns: minmax(0, 1fr) minmax(240px, 280px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .card-enter {
    animation: none;
  }
}
</style>
