<template>
  <div class="article-detail-page ds-page">
    <div
      v-show="readProgress > 0"
      class="read-progress-bar"
      :style="{ transform: `scaleX(${readProgress})` }"
      aria-hidden="true"
    />
    <div class="container article-grid" :class="{ 'article-grid--with-toc': hasToc }">
      <div v-if="loading" class="detail-skeleton-wrap">
        <ArticleDetailSkeleton />
      </div>
      <template v-else>
        <div ref="articleMainRef" class="article-main-stack">
          <article v-if="articleStore.currentArticle" class="article-content">
            <p class="article-kicker">§ ESSAY</p>
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
            <p v-if="articleStore.currentArticle.summary" class="article-summary">
              {{ articleStore.currentArticle.summary }}
            </p>
            <n-space class="article-meta" align="center" :size="12">
              <span class="meta-date">
                {{ formatDate(articleStore.currentArticle.createTime || articleStore.currentArticle.createdAt) }}
              </span>
              <span aria-hidden="true">·</span>
              <span>{{ readingMinutes }} 分钟阅读</span>
            </n-space>
            <div v-if="articleStore.currentArticle.tags?.length" class="article-tags" aria-label="文章标签">
              <router-link
                v-for="tag in articleStore.currentArticle.tags"
                :key="tag.id"
                :to="{ path: '/', query: { tag: tag.id } }"
              >#{{ tag.name }}</router-link>
            </div>
            <div class="article-toolbar">
              <router-link
                v-if="articleStore.currentArticle.authorId"
                :to="`/user/${articleStore.currentArticle.authorId}`"
                class="author-link"
              >
                <UserAvatar
                  :src="articleStore.currentArticle.authorAvatar"
                  :name="articleStore.currentArticle.authorNickname || '作者'"
                  :size="32"
                />
                <span class="author-name">{{ articleStore.currentArticle.authorNickname || '作者' }}</span>
              </router-link>
              <div class="article-toolbar-actions">
                <FollowButton
                  v-if="articleStore.currentArticle.authorId"
                  :user-id="articleStore.currentArticle.authorId"
                  :following="authorFollowing"
                  @update:following="authorFollowing = $event"
                />
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
              </div>
            </div>
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
            <div v-if="articleStore.currentArticle && aiChatVisible" class="article-ask-ai">
              <n-button type="primary" secondary @click="askAiAboutArticle">
                对这篇文章有疑问？问 AI
              </n-button>
            </div>
            <ArticleRewardPanel
              v-if="articleIdNum"
              :article-id="articleIdNum"
              :records="rewardRecords"
              :logged-in="authStore.isLoggedIn"
              :current-user-id="authStore.user?.id"
              @rewarded="loadRewards(articleIdNum)"
            />
          </article>
          <div v-else class="state-msg state-fail">
            <n-empty description="文章不存在或加载失败" />
          </div>

          <section
            v-if="articleStore.currentArticle"
            class="ai-recommend-section"
            aria-label="延伸阅读"
          >
            <p class="section-kicker">§ FURTHER READING</p>
            <h2 class="ai-recommend-title">延伸阅读</h2>
            <div v-if="recommendLoading" class="recommend-list">
              <n-skeleton v-for="n in 3" :key="'rec-sk-' + n" height="72px" />
            </div>
            <n-alert
              v-else-if="recommendError"
              :type="recommendError === loginHintText ? 'warning' : 'error'"
              class="recommend-alert"
            >{{ recommendError }}</n-alert>
            <div v-else-if="recommendArticles.length" class="recommend-list">
                <EditorialArticleCard
                  v-for="(item, index) in recommendArticles"
                  :key="item.id"
                  :article="item"
                  mode="compact"
                  :number="String(index + 1).padStart(2, '0')"
                />
            </div>
            <n-empty v-else description="暂无推荐" />
          </section>

          <section v-if="articleIdNum" class="kg-section" aria-label="知识星系">
            <KnowledgeGraphCard :article-id="articleIdNum" />
          </section>

          <section v-if="articleStore.currentArticle" class="comments-section" aria-label="评论">
            <p class="section-kicker">§ DISCUSSION</p>
            <h2 class="comments-title">评论</h2>
            <n-skeleton v-if="commentsLoading" height="100px" />
            <template v-else>
              <n-list v-if="commentsFlat.length">
                <n-list-item
                  v-for="c in commentsFlat"
                  :key="c.id"
                  :style="{ marginLeft: Math.min(c.depth || 0, 6) * 16 + 'px' }"
                  class="comment-row"
                >
                  <template #prefix>
                    <UserAvatar :src="c.avatar" :name="commentName(c)" :size="32" :equipped-items="c.equippedItems || []" />
                  </template>
                  <div class="comment-body-wrap">
                    <div class="comment-head">
                      <strong class="comment-author" :class="commentNameClass(c)">{{ commentName(c) }}</strong>
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
        <aside class="sidebar" aria-label="目录">
          <nav v-if="headings.length" class="table-of-contents" aria-label="文章目录">
            <p class="toc-label"><span>§</span> CONTENTS</p>
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
                  @click.prevent="goToHeading(heading.id)"
                >
                  {{ heading.text }}
                </a>
              </li>
            </ul>
          </nav>
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
  NEmpty,
  NInput,
  NList,
  NListItem,
  NSkeleton,
  NSpace,
} from 'naive-ui';
import { useArticleStore } from '../stores/article';
import MarkdownRenderer from '../components/MarkdownRenderer.vue';
import EditorialArticleCard from '../components/EditorialArticleCard.vue';
import ArticleDetailSkeleton from '../components/skeleton/ArticleDetailSkeleton.vue';
import KnowledgeGraphCard from '../components/knowledge/KnowledgeGraphCard.vue';
import ArticleActionBar from '../components/ArticleActionBar.vue';
import ArticleRewardPanel from '../components/reward/ArticleRewardPanel.vue';
import FollowButton from '../components/FollowButton.vue';
import UserAvatar from '../components/UserAvatar.vue';
import { getFollowStatus } from '../api/interaction';
import { agentRecommendContext } from '../api/agent';
import { fetchArticleComments, submitComment, deleteComment } from '../api/comments';
import { usePageViewArticle } from '../composables/usePageView';
import { useReadingHistory } from '../composables/useReadingHistory';
import { useToastStore } from '../stores/toast';
import { useAuthStore } from '../stores/auth';
import { useArticleAiChatStore } from '../stores/articleAiChat';
import { useAiChatVisibility } from '../composables/useAiChatVisibility';
import { reportArticle } from '../api/article';
import { fetchArticleRewards } from '../api/reward';
import { effectClass } from '../utils/itemEffects';

const route = useRoute();
const articleStore = useArticleStore();
const toastStore = useToastStore();
const authStore = useAuthStore();
const aiChatStore = useArticleAiChatStore();
const { visible: aiChatVisible } = useAiChatVisibility();
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
  if (!progressEnabled.value) {
    readProgress.value = 0;
    return;
  }
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
const progressEnabled = ref(false);
const headings = ref([]);
const loading = ref(false);
const activeTocId = ref('');
const recommendArticles = ref([]);
const recommendLoading = ref(false);
const recommendError = ref('');
const commentsFlat = ref([]);
const rewardRecords = ref([]);
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

function resetArticleScroll() {
  progressEnabled.value = false;
  readProgress.value = 0;
  if (route.hash) return;
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  document.documentElement.scrollTop = 0;
  document.body.scrollTop = 0;
}

const articleIdNum = computed(() => {
  const id = Number(route.params.id);
  return Number.isFinite(id) ? id : null;
});

const hasToc = computed(() => headings.value.length > 0);
const readingMinutes = computed(() => {
  const content = String(articleStore.currentArticle?.content || '')
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/[#>*_`\[\]()~-]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  return Math.max(1, Math.ceil(content.length / 500));
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

function askAiAboutArticle() {
  const title = articleStore.currentArticle?.title || '这篇文章';
  aiChatStore.openChat({
    draftQuestion: `关于《${title}》我想问：`,
  });
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

const getScrollOffset = () => {
  const styles = getComputedStyle(document.documentElement);
  const navOffset = Number.parseFloat(styles.getPropertyValue('--layout-main-pad-top'));
  const gap = Number.parseFloat(styles.getPropertyValue('--space-2'));
  return (Number.isFinite(navOffset) ? navOffset : 0) + (Number.isFinite(gap) ? gap : 0);
};

const scrollToHeading = (id) => {
  activeTocId.value = id;
  const element = document.getElementById(id);
  if (element) {
    const top = element.getBoundingClientRect().top + window.scrollY - getScrollOffset();
    window.scrollTo({
      top: Math.max(0, top),
      behavior: 'smooth',
    });
  }
};

const goToHeading = (id) => {
  const nextUrl = `${window.location.pathname}${window.location.search}#${encodeURIComponent(id)}`;
  window.history.pushState(window.history.state, '', nextUrl);
  scrollToHeading(id);
};

const formatCommentTime = (t) => {
  if (!t) return '';
  return new Date(t).toLocaleString();
};

const commentName = (c) => (c.nickname && String(c.nickname).trim() ? c.nickname : c.author);
const commentNameClass = (c) => effectClass(c.equippedItems || [], 'NICKNAME_COLOR');

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

async function loadRewards(aid) {
  try {
    const res = await fetchArticleRewards(aid);
    rewardRecords.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    rewardRecords.value = [];
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

function enableProgressFromUserInput() {
  progressEnabled.value = true;
}

watch(
  () => ({ id: route.params.id, lang: route.query.lang }),
  async ({ id: newId, lang }) => {
    progressEnabled.value = false;
    readProgress.value = 0;
    resetArticleScroll();
    headings.value = [];
    activeTocId.value = '';
    recommendArticles.value = [];
    recommendError.value = '';
    rewardRecords.value = [];
    teardownObserver();
    if (!newId) return;
    const langParam =
      typeof lang === 'string' && lang.trim() ? String(lang).trim().toLowerCase() : undefined;
    loading.value = true;
    await articleStore.fetchArticleDetail(String(newId), langParam);
    loading.value = false;
    resetArticleScroll();
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
        await loadRewards(rid);
        await loadComments(rid);
        nextTick(() => {
          resetArticleScroll();
          updateReadProgressBar();
        });
      }
    }
  },
  { immediate: true }
);

watch(
  () => route.hash,
  (hash) => {
    if (!hash || !headings.value.length) return;
    nextTick(() => scrollToHeading(hash.substring(1)));
  }
);

onMounted(() => {
  window.addEventListener('scroll', onReadingScroll, { passive: true });
  window.addEventListener('wheel', enableProgressFromUserInput, { passive: true });
  window.addEventListener('touchstart', enableProgressFromUserInput, { passive: true });
  window.addEventListener('pointerdown', enableProgressFromUserInput, { passive: true });
  window.addEventListener('keydown', enableProgressFromUserInput, { passive: true });
  nextTick(() => updateReadProgressBar());
});

onUnmounted(() => {
  teardownObserver();
  window.removeEventListener('scroll', onReadingScroll);
  window.removeEventListener('wheel', enableProgressFromUserInput);
  window.removeEventListener('touchstart', enableProgressFromUserInput);
  window.removeEventListener('pointerdown', enableProgressFromUserInput);
  window.removeEventListener('keydown', enableProgressFromUserInput);
  if (scrollTimer) clearTimeout(scrollTimer);
});
</script>

<style scoped>
.detail-skeleton-wrap {
  grid-column: 1 / -1;
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

.state-msg {
  text-align: center;
  padding: var(--space-12) var(--space-6);
}

.article-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-8);
  align-items: start;
}

.article-detail-page {
  overflow-anchor: none;
  padding-top: clamp(28px, 3vw, 40px);
}

.article-main-stack {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
}

@media (min-width: 1024px) {
  .article-grid {
    grid-template-columns: 1fr minmax(0, 720px) 15.5rem 1fr;
    grid-template-areas: '. main sidebar .';
    column-gap: var(--space-8);
    row-gap: var(--space-8);
  }

  .article-grid:not(.article-grid--with-toc) {
    grid-template-columns: 1fr minmax(0, 720px) 1fr;
    grid-template-areas: '. main .';
  }

  .detail-skeleton-wrap {
    grid-column: 2;
    grid-row: 1;
    margin: 0;
  }

  .article-main-stack {
    grid-area: main;
    max-width: none;
    margin: 0;
  }

  .sidebar {
    grid-area: sidebar;
    min-width: 0;
    position: sticky;
    top: calc(var(--nav-height) + 24px);
    align-self: start;
  }

  .article-grid--with-toc .sidebar {
    width: 15.5rem;
  }
}

.article-content {
  margin: 0;
  background: transparent;
  padding: 0;
  border-radius: 0;
  border: 0;
  box-shadow: none;
}

.article-kicker {
  margin: 0 0 var(--space-4);
  color: var(--color-accent-text, var(--color-accent));
  font: 500 11px/1 var(--font-mono);
  letter-spacing: .18em;
}

.lang-bar {
  display: inline-flex;
  gap: 0 !important;
  margin-bottom: var(--space-3);
  padding: 2px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill);
}

.lang-pill {
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-pill);
  border: 0;
  box-shadow: none;
  color: var(--color-text);
  text-decoration: none;
  background: transparent;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.lang-pill:hover {
  transform: none;
  background: var(--surface-primary-tint);
}

.lang-pill.lang-on {
  border-color: var(--color-border);
  color: var(--color-on-primary);
  background: var(--color-accent);
  box-shadow: none;
}

.trans-hint {
  margin: 0 0 var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-primary);
  font-weight: 600;
}

.article-title {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 600;
  letter-spacing: 0;
  color: var(--color-text);
  margin: 0 0 var(--space-4);
  word-break: break-word;
  line-height: 1.25;
}

.article-summary {
  max-width: 64ch;
  margin: 0 0 var(--space-5);
  color: var(--color-text-muted);
  font: italic 17px/1.75 var(--font-prose);
}

.article-meta {
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  font: 11px var(--font-mono);
  letter-spacing: .08em;
  color: var(--color-text-muted);
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin: -2px 0 var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.article-tags a {
  color: var(--color-accent-text, var(--color-accent));
  font: 11px var(--font-mono);
  text-decoration: none;
}

.article-tags a:hover {
  text-decoration: underline;
}

.article-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-3) var(--space-5);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.article-toolbar-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.article-toolbar :deep(.action-bar) {
  margin: 0;
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

.article-ask-ai {
  margin: var(--space-6) 0 var(--space-4);
  text-align: center;
}

.prose-shell {
  max-width: 100%;
  margin: 0;
}

@media (max-width: 600px) {
  .article-detail-page {
    padding-top: var(--space-5);
  }

  .article-toolbar {
    align-items: flex-start;
  }

  .article-toolbar-actions {
    width: 100%;
  }
}

.prose-shell :deep(.markdown-prose) {
  font-family: var(--font-prose);
  font-size: 18px;
  font-weight: 400;
  line-height: 2;
  letter-spacing: 0;
  color: var(--color-text-muted);
}

.prose-shell :deep(.markdown-prose p) {
  margin-bottom: 1.35em;
}

.prose-shell :deep(.markdown-prose img) {
  max-width: 100%;
  border-radius: 2px;
  cursor: zoom-in;
}

.prose-shell :deep(.markdown-prose code) {
  font-family: var(--font-mono);
  font-size: 0.9em;
}

.table-of-contents {
  position: static;
  border: 0;
  border-left: 1px solid var(--color-border);
  border-radius: 0;
  box-shadow: none;
  background: transparent;
  padding: 0 0 0 var(--space-4);
}

.toc-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 18px;
  color: var(--color-text);
  font: 500 11px/1 var(--font-mono);
  letter-spacing: .18em;
}

.toc-label span {
  color: var(--color-accent);
}

@media (min-width: 1024px) {
  .table-of-contents {
    width: 100%;
    max-height: calc(100vh - var(--nav-height) - 48px);
    overflow-y: auto;
  }
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
  border-radius: 0;
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
  background: transparent;
}

.table-of-contents a.is-active {
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
  background: transparent;
  border-left: 0;
  padding-left: var(--space-3);
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

.ai-recommend-section,
.kg-section,
.comments-section {
  padding: var(--space-8) 0 0;
  border-top: 1px solid var(--color-border);
  background: transparent;
  border-radius: 0;
  box-shadow: none;
}

.section-kicker {
  margin: 0 0 9px;
  color: var(--color-accent-text, var(--color-accent));
  font: 10px/1 var(--font-mono);
  letter-spacing: .16em;
}

.ai-recommend-title {
  margin: 0 0 var(--space-5);
  font: 600 clamp(24px, 3vw, 32px)/1.2 var(--font-display);
  letter-spacing: 0;
  color: var(--color-text);
}

.recommend-list {
  border-top: 1px solid var(--color-border);
}

.recommend-list :deep(.editorial-card:last-child) {
  border-bottom: 0;
}

.recommend-alert {
  margin-bottom: var(--space-4);
}

@media (max-width: 1023px) {
  .sidebar {
    display: none;
  }
}

@media (min-width: 1024px) {
  .article-grid:not(.article-grid--with-toc) .sidebar {
    display: none;
  }
}

.read-progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  z-index: 1350;
  transform-origin: left center;
  background: var(--color-accent);
  opacity: 0.65;
  pointer-events: none;
}

.comments-title {
  margin: 0 0 var(--space-5);
  color: var(--color-text);
  font: 600 clamp(24px, 3vw, 32px)/1.2 var(--font-display);
  letter-spacing: 0;
}

.comment-row {
  background: transparent;
  border-radius: 0;
  box-shadow: none;
  margin: 0;
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--color-border);
  border-left: 0;
}

.comments-section :deep(.n-list) {
  background: transparent;
}

.comments-section :deep(.n-list-item) {
  padding: 0;
  margin: 0;
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

.item-name-gold {
  color: var(--color-warn);
}

.item-name-pink {
  color: var(--color-accent-pink);
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
  padding: var(--space-4) 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
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
