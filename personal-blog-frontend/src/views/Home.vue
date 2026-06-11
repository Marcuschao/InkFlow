<template>
  <div class="home-page ds-page">
    <div class="container home-layout">
      <div class="home-main">
        <header class="ds-page-hero">
          <h1 class="ds-page-title ds-page-title-lg">{{ homeTab === 'feed' ? '关注动态' : '最新文章' }}</h1>
          <p class="ds-page-sub">{{ homeTab === 'feed' ? '你关注的人最近 7 天发布的内容' : '探索想法、笔记与技术碎片' }}</p>
        </header>

        <n-tabs
          v-if="authStore.isLoggedIn"
          type="line"
          :value="homeTab"
          class="home-tabs"
          @update:value="switchHomeTab"
        >
          <n-tab name="latest" tab="最新" />
          <n-tab name="feed" tab="动态" />
        </n-tabs>

        <n-space v-if="homeTab === 'latest'" class="tag-row" :size="8">
          <n-tag
            v-for="tag in displayedHomeTags"
            :key="tag.id"
            :type="currentTagId === tag.id ? 'primary' : 'default'"
            :bordered="false"
            checkable
            :checked="currentTagId === tag.id"
            @click="filterByTag(tag.id)"
          >{{ tag.name }}</n-tag>
          <n-tag
            :type="currentTagId === null ? 'primary' : 'default'"
            :bordered="false"
            checkable
            :checked="currentTagId === null"
            @click="filterByTag(null)"
          >全部</n-tag>
        </n-space>

        <template v-if="homeTab === 'latest'">
          <ListSkeleton v-if="listingLoading" />
          <n-alert v-else-if="articleStore.listError" type="error" class="home-list-err">{{ articleStore.listError }}</n-alert>
          <n-empty v-else-if="!articleStore.articles.length" description="暂无文章" />
          <n-grid v-else :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
            <n-gi v-for="(article, idx) in articleStore.articles" :key="article.id" span="24 m:12 l:8">
              <ArticleCard class="card-enter" :article="article" :style="{ '--stagger': `${Math.min(idx, 8) * 45}ms` }" />
            </n-gi>
          </n-grid>
          <Pagination
            :total="articleStore.pagination.total"
            :page-size="articleStore.pagination.pageSize"
            :current-page="articleStore.pagination.currentPage"
            @changePage="handlePageChange"
          />
        </template>

        <template v-else>
          <n-grid v-if="feedLoading" :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
            <n-gi v-for="n in 6" :key="'fsk-' + n" span="24 m:12 l:8">
              <n-card><n-skeleton text :repeat="3" /></n-card>
            </n-gi>
          </n-grid>
          <n-empty v-else-if="!feedArticles.length" description="关注用户后，这里会显示他们的新文章" />
          <n-grid v-else :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
            <n-gi v-for="(article, idx) in feedArticles" :key="article.id" span="24 m:12 l:8">
              <ArticleCard
                class="card-enter"
                :article="article"
                :like-count="article.likeCount"
                :liked="article.liked"
                show-like
                :style="{ '--stagger': `${Math.min(idx, 8) * 45}ms` }"
              />
            </n-gi>
          </n-grid>
          <Pagination
            v-if="feedTotal > feedSize"
            :total="feedTotal"
            :page-size="feedSize"
            :current-page="feedPage"
            @changePage="handleFeedPageChange"
          />
        </template>

        <section v-if="homeTab === 'latest'" class="home-hot-section" aria-label="全网热搜">
          <div class="home-hot-head">
            <h2 class="home-hot-title">全网热搜</h2>
            <router-link to="/hot-search" class="home-hot-more">查看全部</router-link>
          </div>
          <div class="home-hot-scroll">
            <HotSearchCard
              v-for="list in hotLists"
              :key="list.source"
              class="home-hot-card"
              :list="list"
              :loading="hotLoading"
              :limit="5"
            />
            <template v-if="hotLoading && !hotLists.length">
              <n-card v-for="n in 2" :key="'hsk-' + n" class="home-hot-card">
                <n-skeleton text :repeat="5" />
              </n-card>
            </template>
          </div>
        </section>
      </div>

      <aside class="home-aside" aria-label="猜你喜欢">
        <n-card title="猜你喜欢">
          <template v-if="!authStore.isLoggedIn">
            <n-empty description="请先登录" size="small" />
          </template>
          <template v-else>
            <n-space v-if="recLoading" vertical :size="12">
              <n-skeleton v-for="n in 4" :key="'rsk-' + n" height="88px" :sharp="false" />
            </n-space>
            <n-empty v-else-if="!recItems.length" description="暂无推荐" size="small" />
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
        </n-card>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue';
import { NAlert, NCard, NEmpty, NGi, NGrid, NSkeleton, NSpace, NTab, NTabs, NTag } from 'naive-ui';
import { useArticleStore } from '../stores/article';
import ArticleCard from '../components/ArticleCard.vue';
import HotSearchCard from '../components/HotSearchCard.vue';
import ListSkeleton from '../components/ListSkeleton.vue';
import Pagination from '../components/Pagination.vue';
import { fetchAllHotSearch } from '../api/hotSearch';
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
const hotLists = ref([]);
const hotLoading = ref(false);

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

async function loadHotSearch() {
  hotLoading.value = true;
  try {
    const res = await fetchAllHotSearch({ limit: 5 });
    hotLists.value = res.data || [];
  } catch {
    hotLists.value = [];
  } finally {
    hotLoading.value = false;
  }
}

onMounted(() => {
  articleStore.fetchTags();
  loadHotSearch();
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

.home-hot-section {
  margin-top: var(--space-12);
  padding-top: var(--space-8);
  border-top: 1px solid var(--color-border);
}

.home-hot-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.home-hot-title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.home-hot-more {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  text-decoration: none;
}

.home-hot-more:hover {
  color: var(--color-primary);
}

.home-hot-scroll {
  display: flex;
  gap: var(--space-4);
  overflow-x: auto;
  padding-bottom: var(--space-2);
  scroll-snap-type: x mandatory;
}

.home-hot-card {
  flex: 0 0 min(320px, 85vw);
  scroll-snap-align: start;
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
