<template>
  <div class="article-detail-page ds-page">
    <div
      class="read-progress-bar"
      :style="{ transform: `scaleX(${readProgress})` }"
      aria-hidden="true"
    />
    <div class="container article-grid">
      <div v-if="loading" class="detail-skeleton">
        <n-skeleton height="40px" width="80%" style="margin-bottom: 20px" />
        <n-skeleton height="20px" width="40%" style="margin-bottom: 40px" />
        <n-skeleton text :repeat="5" />
      </div>
      <template v-else>
        <div ref="articleMainRef" class="article-main-stack">
          <article v-if="articleStore.currentArticle" class="article-content">
            <n-space class="lang-bar" :size="8">
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
            </n-space>
            <p v-if="articleStore.currentArticle.translationActive" class="trans-hint">
              当前为译文 · {{ (articleStore.currentArticle.viewingLocale || '').toUpperCase() }}
            </p>
            <h1 class="article-title">{{ articleStore.currentArticle.title }}</h1>
            <n-space class="article-meta" align="center" :size="12">
              <span class="meta-date">
                {{ formatDate(articleStore.currentArticle.createTime || articleStore.currentArticle.createdAt) }}
              </span>
              <n-space v-if="articleStore.currentArticle.tags && articleStore.currentArticle.tags.length" :size="6">
                <n-tag
                  v-for="tag in articleStore.currentArticle.tags"
                  :key="tag.id"
                  size="small"
                  :bordered="false"
                  type="primary"
                >{{ tag.name }}</n-tag>
              </n-space>
            </n-space>
            <div v-if="articleStore.currentArticle.authorId" class="author-row">
              <router-link :to="`/user/${articleStore.currentArticle.authorId}`" class="author-link">
                <UserAvatar
                  :src="articleStore.currentArticle.authorAvatar"
                  :name="articleStore.currentArticle.authorNickname || '作者'"
                  :size="32"
                />
                <span class="author-name">{{ articleStore.currentArticle.authorNickname || '作者' }}</span>
              </router-link>
              <FollowButton
                :user-id="articleStore.currentArticle.authorId"
                :following="authorFollowing"
                @update:following="authorFollowing = $event"
              />
            </div>
            <ArticleActionBar
              v-if="articleIdNum"
              :article-id="articleIdNum"
              :liked="liked"
              :favorited="favorited"
              :like-count="likeCount"
              @update:liked="liked = $event"
              @update:favorited="favorited = $event"
              @update:like-count="likeCount = $event"
            />
            <n-button
              v-if="authStore.isLoggedIn && articleIdNum"
              class="report-btn"
              size="small"
              quaternary
              type="error"
              @click="onReportArticle"
            >
              举报
            </n-button>
            <div class="prose-shell">
              <MarkdownRenderer
                :markdown="articleStore.currentArticle.content || ''"
                @headings-extracted="handleHeadings"
              />
            </div>
          </article>
          <div v-else class="state-msg state-fail">
            <n-empty description="文章不存在或加载失败" />
          </div>

          <section
            v-if="articleStore.currentArticle"
            class="ai-recommend-section"
            aria-label="延伸阅读"
          >
            <h2 class="ai-recommend-title">延伸阅读</h2>
            <n-grid v-if="recommendLoading" :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <n-gi v-for="n in 3" :key="'rec-sk-' + n" span="24 m:8">
                <n-card><n-skeleton height="80px" /></n-card>
              </n-gi>
            </n-grid>
            <n-alert
              v-else-if="recommendError"
              :type="recommendError === loginHintText ? 'warning' : 'error'"
              class="recommend-alert"
            >{{ recommendError }}</n-alert>
            <n-grid v-else-if="recommendArticles.length" :cols="1" :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
              <n-gi v-for="item in recommendArticles" :key="item.id" span="24 m:8">
                <ArticleCard
                  :article="item"
                  :reason="item.reason"
                />
              </n-gi>
            </n-grid>
            <n-empty v-else description="暂无推荐" />
          </section>

          <section v-if="articleIdNum" class="kg-section" aria-label="知识星系">
            <KnowledgeGraphCard :article-id="articleIdNum" />
          </section>

          <section v-if="articleStore.currentArticle" class="comments-section" aria-label="评论">
            <h2 class="comments-title">评论</h2>
            <n-skeleton v-if="commentsLoading" height="100px" />
            <template v-else>
              <n-list v-if="commentsFlat.length" hoverable>
                <n-list-item
                  v-for="c in commentsFlat"
                  :key="c.id"
                  :style="{ marginLeft: Math.min(c.depth || 0, 6) * 16 + 'px' }"
                  class="comment-row"
                >
                  <template #prefix>
                    <UserAvatar :src="c.avatar" :name="commentName(c)" :size="32" />
                  </template>
                  <div class="comment-body-wrap">
                    <div class="comment-head">
                      <strong class="comment-author">{{ commentName(c) }}</strong>
                      <time class="comment-time">{{ formatCommentTime(c.createTime) }}</time>
                    </div>
                    <p class="comment-body">{{ c.content }}</p>
                    <n-space class="comment-actions" :size="12">
                      <n-button text size="tiny" type="primary" @click="setReplyTo(c.id)">回复</n-button>
                      <n-button
                        v-if="canDeleteOwnComment(c)"
                        text
                        size="tiny"
                        type="error"
                        :loading="deletingId === c.id"
                        @click="removeCommentRow(c)"
                      >删除</n-button>
                    </n-space>
                  </div>
                </n-list-item>
              </n-list>
              <n-empty v-else description="暂无评论" />
            </template>

            <div v-if="!authStore.isLoggedIn" class="comment-login-hint">
              请<router-link :to="loginRedirect">登录</router-link>后发表评论
            </div>
            <form v-else class="comment-form" @submit.prevent="submitCommentForm">
              <p class="comment-user-hint">以 <strong>{{ authStore.displayName }}</strong> 的身份评论</p>
              <p v-if="replyParentId" class="reply-hint">
                回复评论 #{{ replyParentId }}
                <n-button text size="tiny" type="primary" @click="replyParentId = null">取消</n-button>
              </p>
              <div class="cf-row">
                <n-input
                  v-model:value="cf.content"
                  type="textarea"
                  placeholder="说点什么吧…"
                  :rows="4"
                  maxlength="4000"
                  show-count
                />
              </div>
              <n-button attr-type="submit" type="primary" :loading="commentSubmitting">
                提交评论
              </n-button>
            </form>
          </section>
        </div>
        <aside v-if="articleStore.currentArticle && headings.length" class="sidebar">
          <n-card class="table-of-contents" title="目录">
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
          </n-card>
        </aside>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, onMounted, onUnmounted, reactive, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  NAlert,
  NButton,
  NCard,
  NEmpty,
  NGi,
  NGrid,
  NInput,
  NList,
  NListItem,
  NSkeleton,
  NSpace,
  NTag,
} from 'naive-ui';
import { useArticleStore } from '../stores/article';
import MarkdownRenderer from '../components/MarkdownRenderer.vue';
import ArticleCard from '../components/ArticleCard.vue';
import KnowledgeGraphCard from '../components/knowledge/KnowledgeGraphCard.vue';
import ArticleActionBar from '../components/ArticleActionBar.vue';
import FollowButton from '../components/FollowButton.vue';
import UserAvatar from '../components/UserAvatar.vue';
import { getFollowStatus } from '../api/interaction';
import { agentRecommendContext } from '../api/agent';
import { fetchArticleComments, submitComment, deleteComment } from '../api/comments';
import { usePageViewArticle } from '../composables/usePageView';
import { useReadingHistory } from '../composables/useReadingHistory';
import { useToastStore } from '../stores/toast';
import { useAuthStore } from '../stores/auth';
import { reportArticle } from '../api/article';

const route = useRoute();
const articleStore = useArticleStore();
const toastStore = useToastStore();
const authStore = useAuthStore();
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
function updateReadProgressBar() {
  const el = articleMainRef.value;
  if (!el) {
    readProgress.value = 0;
    return;
  }
  const rect = el.getBoundingClientRect();
  const scrollTop = window.scrollY || document.documentElement.scrollTop || 0;
  const elTop = scrollTop + rect.top;
  const elHeight = Math.max(el.offsetHeight, 1);
  const win = window.innerHeight || 1;
  const progressPx = scrollTop + win * 0.15 - elTop;
  readProgress.value = Math.max(0, Math.min(1, progressPx / elHeight));
}

function onReadingScroll() {
  updateReadProgressBar();
  const id = Number(route.params.id);
  if (!Number.isFinite(id) || !articleStore.currentArticle) return;
  const doc = document.documentElement;
  const max = doc.scrollHeight - doc.clientHeight;
  const st = window.scrollY || doc.scrollTop || 0;
  const pct = max <= 0 ? 100 : Math.round((st / max) * 100);
  if (scrollTimer) clearTimeout(scrollTimer);
  scrollTimer = setTimeout(() => updateProgress(id, pct), 450);
}

const articleMainRef = ref(null);
const readProgress = ref(0);
const headings = ref([]);
const loading = ref(false);
const activeTocId = ref('');
const recommendArticles = ref([]);
const recommendLoading = ref(false);
const recommendError = ref('');
const commentsFlat = ref([]);
const commentsLoading = ref(false);
const commentSubmitting = ref(false);
const deletingId = ref(null);
const replyParentId = ref(null);
const liked = ref(false);
const favorited = ref(false);
const likeCount = ref(0);
const authorFollowing = ref(false);
const cf = reactive({
  content: '',
});

const loginRedirect = computed(() => ({
  path: '/login',
  query: { redirect: route.fullPath },
}));

const articleIdNum = computed(() => {
  const id = Number(route.params.id);
  return Number.isFinite(id) ? id : null;
});

function syncInteractionFromArticle(a) {
  if (!a) return;
  liked.value = !!a.liked;
  favorited.value = !!a.favorited;
  likeCount.value = a.likeCount ?? 0;
  if (a.authorId && authStore.isLoggedIn) {
    getFollowStatus(a.authorId)
      .then((res) => { authorFollowing.value = !!res.data?.following; })
      .catch(() => { authorFollowing.value = false; });
  } else {
    authorFollowing.value = false;
  }
}

async function onReportArticle() {
  if (!articleIdNum.value) return;
  const reason = prompt('请填写举报原因');
  if (!reason || !reason.trim()) return;
  try {
    await reportArticle(articleIdNum.value, reason.trim());
    toastStore.push('举报已提交', 'success');
  } catch (e) {
    toastStore.push(e?.message || '举报失败', 'error');
  }
}

watch(
  () => articleStore.currentArticle,
  (a) => syncInteractionFromArticle(a),
  { immediate: true }
);

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

const formatCommentTime = (t) => {
  if (!t) return '';
  return new Date(t).toLocaleString();
};

const commentName = (c) => (c.nickname && String(c.nickname).trim() ? c.nickname : c.author);

const canDeleteOwnComment = (c) =>
  authStore.isLoggedIn && c.userId != null && authStore.user?.id != null && c.userId === authStore.user.id;

async function removeCommentRow(c) {
  const aid = Number(route.params.id);
  deletingId.value = c.id;
  try {
    await deleteComment(c.id);
    toastStore.push('已删除', 'success');
    if (Number.isFinite(aid)) await loadComments(aid);
  } catch {
    /* toast */
  } finally {
    deletingId.value = null;
  }
}

function assignDepth(flat) {
  const map = Object.fromEntries(flat.map((x) => [x.id, x]));
  return flat.map((c) => {
    let d = 0;
    let cur = c;
    while (cur.parentId && map[cur.parentId]) {
      d += 1;
      cur = map[cur.parentId];
      if (d > 24) break;
    }
    return { ...c, depth: d };
  });
}

async function loadComments(aid) {
  commentsLoading.value = true;
  try {
    const res = await fetchArticleComments(aid);
    const list = Array.isArray(res.data) ? res.data : [];
    commentsFlat.value = assignDepth(list);
  } catch {
    commentsFlat.value = [];
  } finally {
    commentsLoading.value = false;
  }
}

function setReplyTo(id) {
  if (!authStore.isLoggedIn) return;
  replyParentId.value = id;
}

async function submitCommentForm() {
  const aid = Number(route.params.id);
  if (!Number.isFinite(aid) || !authStore.isLoggedIn) return;
  commentSubmitting.value = true;
  try {
    await submitComment({
      articleId: aid,
      parentId: replyParentId.value || undefined,
      content: cf.content,
    });
    toastStore.push('提交成功，审核通过后可见', 'success');
    cf.content = '';
    replyParentId.value = null;
    await loadComments(aid);
  } catch {
    /* request 已 toast */
  } finally {
    commentSubmitting.value = false;
  }
}

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
      const rid = Number(newId);
      if (!authStore.isLoggedIn) {
        recommendArticles.value = [];
        recommendError.value = loginHintText;
        recommendLoading.value = false;
      } else {
        recommendLoading.value = true;
        recommendError.value = '';
        try {
          const recent = getRecentArticleIds(24).filter((x) => x !== rid);
          const list = await agentRecommendContext({ articleId: rid, recentArticleIds: recent });
          recommendArticles.value = list
            .slice(0, 3)
            .map(normalizeRecommendItem)
            .filter(Boolean);
        } catch (e) {
          const status = e?.response?.status ?? e?.responseStatus;
          const msg = String(e?.message || '');
          const authDenied =
            status === 401 ||
            status === 403 ||
            e?.code === 401 ||
            e?.code === 403 ||
            /\b401\b/.test(msg) ||
            /\b403\b/.test(msg);
          recommendError.value = authDenied ? loginHintText : msg || '推荐加载失败';
        } finally {
          recommendLoading.value = false;
        }
      }
      if (Number.isFinite(rid)) {
        await loadComments(rid);
        nextTick(() => updateReadProgressBar());
      }
    }
  },
  { immediate: true }
);

onMounted(() => {
  window.addEventListener('scroll', onReadingScroll, { passive: true });
  nextTick(() => updateReadProgressBar());
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

.state-msg {
  text-align: center;
  padding: 4rem 1.5rem;
}

.article-main-stack {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

.article-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-8);
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
  margin: 0;
  background: var(--color-surface);
  padding: clamp(1.5rem, 3vw, 2.5rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.lang-bar {
  margin-bottom: var(--space-3);
}

.lang-pill {
  font-size: var(--text-xs);
  font-weight: 600;
  padding: var(--space-1) var(--space-3);
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
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.lang-pill.lang-on {
  border-color: transparent;
  color: #fff;
  background: var(--gradient-cta);
}

.trans-hint {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-primary);
  font-weight: 600;
}

.article-title {
  font-family: var(--font-ui);
  font-size: var(--text-display);
  font-weight: var(--weight-bold, 700);
  letter-spacing: -0.035em;
  color: var(--color-text);
  margin: 0 0 var(--space-4);
  word-break: break-word;
  line-height: 1.2;
}

.article-meta {
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.author-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.author-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  text-decoration: none;
  color: var(--color-text);
  min-width: 0;
}

.author-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
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
  background: var(--color-primary);
  vertical-align: middle;
}

.prose-shell {
  max-width: 42rem;
  margin: 0 auto;
}

.prose-shell :deep(.markdown-prose) {
  font-family: var(--font-prose);
}

.table-of-contents {
  position: sticky;
  top: calc(var(--layout-navbar-bottom) + 1rem);
}

.table-of-contents ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.table-of-contents li {
  margin-bottom: var(--space-1);
}

.table-of-contents a {
  position: relative;
  color: var(--color-text-muted);
  text-decoration: none;
  display: block;
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
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
  background: var(--color-primary);
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
  padding-left: var(--space-2);
}

.toc-item-3 {
  padding-left: var(--space-4);
}

.toc-item-4 {
  padding-left: var(--space-6);
}

.ai-recommend-section {
  background: var(--color-surface);
  padding: clamp(1.25rem, 3vw, 1.85rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.ai-recommend-title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.recommend-alert {
  margin-bottom: var(--space-4);
}

@media (max-width: 1023px) {
  .sidebar {
    display: none;
  }
}

.read-progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 1350;
  transform-origin: left center;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-hover));
  pointer-events: none;
  opacity: 0.95;
}

.comments-section {
  background: var(--color-surface);
  padding: clamp(1.25rem, 3vw, 1.85rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.comments-title {
  margin: 0 0 var(--space-4);
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
}

.comment-row {
  border-bottom: 1px solid var(--color-border);
}

.comment-row:last-child {
  border-bottom: none;
}

.comment-body-wrap {
  flex: 1;
  min-width: 0;
}

.comment-head {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
}

.comment-author {
  font-size: var(--text-sm);
}

.comment-time {
  font-size: var(--text-xs);
  color: var(--color-text-soft);
}

.comment-body {
  margin: 0;
  font-size: var(--text-sm);
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  margin-top: var(--space-2);
}

.comment-login-hint {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.comment-login-hint a {
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
}

.comment-user-hint {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.comment-form {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px dashed var(--color-border);
}

.cf-row {
  margin-bottom: var(--space-3);
}

.reply-hint {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin-bottom: var(--space-2);
}
</style>
