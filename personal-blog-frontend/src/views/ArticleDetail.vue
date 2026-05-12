<template>
  <div class="article-detail-page ds-page">
    <div class="container article-grid">
      <div v-if="loading" class="detail-skeleton">
        <div class="ui-skeleton sk-head" />
        <div class="ui-skeleton sk-meta" />
        <div class="ui-skeleton sk-line" />
        <div class="ui-skeleton sk-line" />
        <div class="ui-skeleton sk-line short" />
      </div>
      <template v-else>
        <div class="article-main-stack">
          <article v-if="articleStore.currentArticle" class="article-content">
            <nav class="lang-bar" aria-label="语言切换">
              <router-link
                class="lang-pill"
                :class="{ 'lang-on': !route.query.lang }"
                :to="{ name: 'ArticleDetail', params: { id: route.params.id } }"
              >原文</router-link>
              <router-link
                class="lang-pill"
                :class="{ 'lang-on': route.query.lang === 'en' }"
                :to="{ name: 'ArticleDetail', params: { id: route.params.id }, query: { lang: 'en' } }"
              >EN</router-link>
              <router-link
                class="lang-pill"
                :class="{ 'lang-on': route.query.lang === 'ja' }"
                :to="{ name: 'ArticleDetail', params: { id: route.params.id }, query: { lang: 'ja' } }"
              >JA</router-link>
              <router-link
                class="lang-pill"
                :class="{ 'lang-on': route.query.lang === 'ko' }"
                :to="{ name: 'ArticleDetail', params: { id: route.params.id }, query: { lang: 'ko' } }"
              >KO</router-link>
            </nav>
            <p v-if="articleStore.currentArticle.translationActive" class="trans-hint">
              当前为译文 · {{ (articleStore.currentArticle.viewingLocale || '').toUpperCase() }}
            </p>
            <h1 class="article-title">{{ articleStore.currentArticle.title }}</h1>
            <div class="article-meta">
              <span class="meta-date">
                {{ formatDate(articleStore.currentArticle.createTime || articleStore.currentArticle.createdAt) }}
              </span>
              <span
                v-if="articleStore.currentArticle.tags && articleStore.currentArticle.tags.length"
                class="meta-tags"
              >
                <span
                  v-for="tag in articleStore.currentArticle.tags"
                  :key="tag.id"
                  class="article-tag"
                >{{ tag.name }}</span>
              </span>
            </div>
            <div class="prose-shell">
              <MarkdownRenderer
                :markdown="articleStore.currentArticle.content || ''"
                @headings-extracted="handleHeadings"
              />
            </div>
          </article>
          <div v-else class="state-msg state-fail">
            <p>文章不存在或加载失败</p>
          </div>

          <section
            v-if="articleStore.currentArticle"
            class="ai-recommend-section"
            aria-label="AI 推荐阅读"
          >
            <h2 class="ai-recommend-title">AI 推荐阅读</h2>
            <div v-if="recommendLoading" class="ai-recommend-skel">
              <div class="ui-skeleton sk-rec" />
              <div class="ui-skeleton sk-rec" />
              <div class="ui-skeleton sk-rec" />
            </div>
            <p
              v-else-if="recommendError"
              :class="recommendError === loginHintText ? 'ai-recommend-login' : 'ai-recommend-fail'"
            >{{ recommendError }}</p>
            <div v-else-if="recommendArticles.length" class="ai-recommend-grid">
              <ArticleCard
                v-for="item in recommendArticles"
                :key="item.id"
                :article="item"
                :reason="item.reason"
              />
            </div>
            <p v-else class="ai-recommend-empty">暂无推荐</p>
          </section>
        </div>
        <aside v-if="articleStore.currentArticle && headings.length" class="sidebar">
          <div class="table-of-contents">
            <h3 class="toc-title">目录</h3>
            <ul>
              <li
                v-for="heading in headings"
                :key="heading.id"
                :class="`toc-item toc-item-${heading.level}`"
                :data-id="heading.id"
              >
                <a
                  :class="{ 'is-active': activeTocId === heading.id }"
                  :href="`#${heading.id}`"
                  @click.prevent="scrollToHeading(heading.id)"
                >
                  {{ heading.text }}
                </a>
              </li>
            </ul>
          </div>
        </aside>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import { useArticleStore } from '../stores/article';
import MarkdownRenderer from '../components/MarkdownRenderer.vue';
import ArticleCard from '../components/ArticleCard.vue';
import { agentRecommendContext } from '../api/agent';
import { usePageViewArticle } from '../composables/usePageView';
import { useReadingHistory } from '../composables/useReadingHistory';

const route = useRoute();
const articleStore = useArticleStore();
const { recordVisit, updateProgress, getRecentArticleIds } = useReadingHistory();
usePageViewArticle(() => articleStore.currentArticle?.id);

useHead(() => {
  const a = articleStore.currentArticle;
  const title = a ? `${a.seoTitle || a.title || '文章'} · 博客` : '博客';
  const desc = (a?.seoDescription || a?.summary || '').slice(0, 160);
  return {
    title,
    meta: [
      { name: 'description', content: desc },
      { property: 'og:title', content: (a?.seoTitle || a?.title || '').slice(0, 120) },
      {
        property: 'og:description',
        content: (a?.seoDescription || a?.summary || '').slice(0, 220),
      },
    ],
  };
});

let scrollTimer = null;
function onReadingScroll() {
  const id = Number(route.params.id);
  if (!Number.isFinite(id) || !articleStore.currentArticle) return;
  const doc = document.documentElement;
  const max = doc.scrollHeight - doc.clientHeight;
  const st = window.scrollY || doc.scrollTop || 0;
  const pct = max <= 0 ? 100 : Math.round((st / max) * 100);
  if (scrollTimer) clearTimeout(scrollTimer);
  scrollTimer = setTimeout(() => updateProgress(id, pct), 450);
}

const headings = ref([]);
const loading = ref(false);
const activeTocId = ref('');
const recommendArticles = ref([]);
const recommendLoading = ref(false);
const recommendError = ref('');

const loginHintText = '请先登录';

let headingObserver = null;

const teardownObserver = () => {
  if (headingObserver) {
    headingObserver.disconnect();
    headingObserver = null;
  }
};

const setupHeadingObserver = () => {
  teardownObserver();
  if (!headings.value.length) return;

  headingObserver = new IntersectionObserver(
    (entries) => {
      const fromTop = entries
        .filter((e) => e.isIntersecting && e.target.id)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
      if (fromTop.length) {
        activeTocId.value = fromTop[0].target.id;
      }
    },
    {
      root: null,
      rootMargin: '-12% 0px -62% 0px',
      threshold: [0, 0.05, 0.1, 0.25, 0.5, 1],
    }
  );

  headings.value.forEach((h) => {
    const el = document.getElementById(h.id);
    if (el) headingObserver.observe(el);
  });
};

const handleHeadings = (extractedHeadings) => {
  headings.value = extractedHeadings;
  nextTick(() => {
    setupHeadingObserver();
    if (route.hash) {
      scrollToHeading(route.hash.substring(1));
    }
  });
};

const scrollToHeading = (id) => {
  activeTocId.value = id;
  const element = document.getElementById(id);
  if (element) {
    element.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    });
  }
};

const formatDate = (dateString) => {
  const options = { year: 'numeric', month: 'long', day: 'numeric' };
  return new Date(dateString).toLocaleDateString(undefined, options);
};

function normalizeRecommendItem(raw, idx) {
  if (!raw || typeof raw !== 'object') return null;
  const id = raw.id ?? raw.articleId ?? idx;
  return {
    id,
    title: raw.title || '',
    summary: raw.summary || '',
    content: raw.content || '',
    createTime: raw.createTime || raw.createdAt,
    createdAt: raw.createdAt || raw.createTime,
    tags: Array.isArray(raw.tags) ? raw.tags : [],
    reason: raw.reason || '',
  };
}

watch(
  () => ({ id: route.params.id, lang: route.query.lang }),
  async ({ id: newId, lang }) => {
    headings.value = [];
    activeTocId.value = '';
    recommendArticles.value = [];
    recommendError.value = '';
    teardownObserver();
    if (!newId) return;
    const langParam =
      typeof lang === 'string' && lang.trim() ? String(lang).trim().toLowerCase() : undefined;
    loading.value = true;
    await articleStore.fetchArticleDetail(String(newId), langParam);
    loading.value = false;
    if (articleStore.currentArticle) {
      recordVisit(articleStore.currentArticle);
      recommendLoading.value = true;
      try {
        const rid = Number(newId);
        const recent = getRecentArticleIds(24).filter((x) => x !== rid);
        const list = await agentRecommendContext({ articleId: rid, recentArticleIds: recent });
        recommendArticles.value = list
          .slice(0, 3)
          .map(normalizeRecommendItem)
          .filter(Boolean);
      } catch (e) {
        const status = e?.response?.status;
        recommendError.value =
          status === 401 || status === 403 ? loginHintText : e?.message || '推荐加载失败';
      } finally {
        recommendLoading.value = false;
      }
    }
  },
  { immediate: true }
);

onMounted(() => {
  window.addEventListener('scroll', onReadingScroll, { passive: true });
});

onUnmounted(() => {
  teardownObserver();
  window.removeEventListener('scroll', onReadingScroll);
  if (scrollTimer) clearTimeout(scrollTimer);
});
</script>

<style scoped>
.detail-skeleton {
  max-width: 44rem;
  margin: 0 auto;
  padding: 2rem;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.sk-head {
  height: 2.4rem;
  width: 88%;
  margin-bottom: 1.25rem;
}

.sk-meta {
  height: 1rem;
  width: 40%;
  margin-bottom: 2rem;
}

.sk-line {
  height: 0.85rem;
  width: 100%;
  margin-bottom: 0.65rem;
}

.sk-line.short {
  width: 62%;
}

.state-msg {
  text-align: center;
  padding: 4rem 1.5rem;
  color: var(--color-text-muted);
  font-size: 1.05rem;
}

.state-fail p {
  padding: 1.5rem 2rem;
  display: inline-block;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.article-main-stack {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.article-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 2rem;
  align-items: start;
}

@media (min-width: 1024px) {
  .article-grid {
    grid-template-columns: minmax(0, 1fr) 15.5rem;
  }

  .sidebar {
    order: 2;
  }
}

.article-content {
  background: var(--color-surface);
  padding: clamp(1.5rem, 3vw, 2.5rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.lang-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-bottom: 0.75rem;
}

.lang-pill {
  font-size: 0.78rem;
  font-weight: 600;
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  text-decoration: none;
  background: rgba(248, 250, 252, 0.85);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    background var(--transition-fast);
}

.lang-pill:hover {
  border-color: var(--border-accent-strong);
  color: var(--color-primary);
}

.lang-pill.lang-on {
  border-color: transparent;
  color: #fff;
  background: var(--gradient-cta);
}

.trans-hint {
  margin: 0 0 0.85rem;
  font-size: 0.78rem;
  color: var(--color-primary);
  font-weight: 600;
}

.article-title {
  font-family: var(--font-ui);
  font-size: clamp(1.85rem, 4.5vw, 2.65rem);
  font-weight: 700;
  letter-spacing: -0.035em;
  color: var(--color-text);
  margin: 0 0 1.25rem;
  word-break: break-word;
  line-height: 1.2;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 2rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.meta-date {
  font-variant-numeric: tabular-nums;
}

.meta-date::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 0.5rem;
  border-radius: var(--radius-pill);
  background: var(--gradient-cta);
  vertical-align: middle;
}

.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.article-tag {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  padding: 0.28em 0.65em;
  border-radius: var(--radius-pill);
  font-size: 0.78rem;
  font-weight: 600;
}

.prose-shell {
  max-width: 42rem;
  margin: 0 auto;
}

.prose-shell :deep(.markdown-prose) {
  font-family: var(--font-prose);
}

.table-of-contents {
  background: var(--color-surface-glass);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  padding: 1.35rem 1.25rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: calc(var(--nav-height) + 1rem);
}

.toc-title {
  margin: 0 0 1rem;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--color-text-soft);
}

.table-of-contents ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.table-of-contents li {
  margin-bottom: 0.15rem;
}

.table-of-contents a {
  position: relative;
  color: var(--color-text-muted);
  text-decoration: none;
  display: block;
  padding: 0.42rem 0.5rem 0.42rem 0.85rem;
  border-radius: var(--radius-sm);
  font-size: 0.86rem;
  line-height: 1.35;
  transition: color var(--transition-fast), background var(--transition-fast);
}

.table-of-contents a::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  border-radius: var(--radius-pill);
  background: var(--gradient-cta);
  transition: height var(--transition-fast);
}

.table-of-contents a:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.table-of-contents a.is-active {
  color: var(--color-primary);
  font-weight: 600;
}

.table-of-contents a.is-active::before {
  height: 65%;
}

.toc-item-2 {
  padding-left: 0.65rem;
}

.toc-item-3 {
  padding-left: 1.15rem;
}

.toc-item-4 {
  padding-left: 1.65rem;
}

.ai-recommend-section {
  background: var(--color-surface);
  padding: clamp(1.25rem, 3vw, 1.85rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.ai-recommend-title {
  margin: 0 0 1.15rem;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text);
}

.ai-recommend-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 1rem;
}

@media (min-width: 640px) {
  .ai-recommend-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

.ai-recommend-skel {
  display: grid;
  gap: 0.75rem;
  grid-template-columns: 1fr;
}

@media (min-width: 640px) {
  .ai-recommend-skel {
    grid-template-columns: repeat(3, 1fr);
  }
}

.sk-rec {
  height: 8.5rem;
  border-radius: var(--radius-lg);
}

.ai-recommend-empty,
.ai-recommend-login {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.ai-recommend-fail {
  margin: 0;
  font-size: 0.88rem;
  color: #b91c1c;
}

@media (max-width: 1023px) {
  .sidebar {
    display: none;
  }
}
</style>
