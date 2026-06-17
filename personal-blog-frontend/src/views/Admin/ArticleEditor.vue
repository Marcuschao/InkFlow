<template>
  <div class="article-editor-page admin-page">
    <div class="container editor-shell">
      <header class="editor-head ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">{{ isEditMode ? '编辑文章' : '新建文章' }}</h1>
          <p class="ds-page-sub">{{ isAuthorMode ? '支持 Markdown，提交后将进入审核' : '支持 Markdown，管理员可直接发布' }}</p>
        </div>
        <router-link :to="backPath">
          <n-button>{{ isAuthorMode ? '我的文章' : '返回管理' }}</n-button>
        </router-link>
      </header>

      <n-tabs v-if="isEditMode && !isAuthorMode" type="line" :value="activeLang" @update:value="setLang" style="margin-bottom: 20px;">
        <n-tab v-for="t in langTabs" :key="t.key" :name="t.key" :tab="t.label" />
      </n-tabs>

      <div class="editor-layout">
        <n-card class="article-form-card">
          <form class="article-form" @submit.prevent="() => handleSubmit(false)">
            <template v-if="activeLang === 'zh'">
              <n-form-item label="文章标题">
                <n-input v-model:value="article.title" type="text" required placeholder="请输入文章标题" />
              </n-form-item>

              <n-form-item label="摘要">
                <n-input v-model:value="article.summary" type="text" placeholder="可选，显示在列表卡片中" />
              </n-form-item>

              <n-form-item label="标签（逗号分隔）">
                <n-input v-model:value="tagsInput" type="text" placeholder="Vue, JavaScript, CSS" />
              </n-form-item>

              <n-form-item label="正文（Markdown）">
                <n-input
                  v-model:value="article.content"
                  type="textarea"
                  ref="contentRef"
                  :rows="20"
                  required
                  placeholder="开始写作吧…"
                />
              </n-form-item>

              <n-form-item label="SEO 标题">
                <n-input v-model:value="article.seoTitle" type="text" />
              </n-form-item>

              <n-form-item label="SEO 描述">
                <n-input v-model:value="article.seoDescription" type="text" />
              </n-form-item>

              <n-space class="ai-meta-row" :size="12" style="margin-bottom: 20px;">
                <n-button
                  v-if="isEditMode && article.id && activeLang === 'zh'"
                  secondary
                  @click="revisionDrawerOpen = true"
                >
                  历史版本
                </n-button>
                <n-button
                  secondary
                  :loading="aiSummaryLoading"
                  :disabled="!article.content || !article.content.trim()"
                  @click="fillSummary"
                >
                  智能提取摘要
                </n-button>
                <n-button
                  secondary
                  :loading="aiTagsLoading"
                  :disabled="!article.content || !article.content.trim()"
                  @click="fillTags"
                >
                  智能推荐标签
                </n-button>
                <n-button
                  v-if="!isAuthorMode"
                  secondary
                  :loading="zhSeoBusy"
                  :disabled="!article.id"
                  @click="runZhSeoAi"
                >
                  中文 SEO 助手
                </n-button>
              </n-space>
            </template>

            <template v-else>
              <n-form-item label="标题">
                <n-input v-model:value="trans[activeLang].title" type="text" required />
              </n-form-item>

              <n-form-item label="摘要">
                <n-input v-model:value="trans[activeLang].summary" type="text" />
              </n-form-item>

              <n-form-item label="正文">
                <n-input v-model:value="trans[activeLang].content" type="textarea" :rows="20" required />
              </n-form-item>

              <n-form-item label="SEO 标题">
                <n-input v-model:value="trans[activeLang].seoTitle" type="text" />
              </n-form-item>

              <n-form-item label="SEO 描述">
                <n-input v-model:value="trans[activeLang].seoDescription" type="text" />
              </n-form-item>

              <n-space class="ai-meta-row" :size="12" style="margin-bottom: 20px;">
                <n-button :loading="transBusy" @click="saveTrans">
                  保存译文
                </n-button>
                <n-button :loading="transBusy" @click="machineTrans">
                  机翻填充
                </n-button>
                <n-button :loading="transBusy" @click="seoTrans">
                  译文 SEO 助手
                </n-button>
              </n-space>
            </template>

            <n-space v-if="activeLang === 'zh'" vertical :size="12" style="margin-top: 12px;">
              <n-button
                v-if="isAuthorMode"
                block
                :loading="isLoading"
                @click="handleSubmit(true)"
              >
                保存草稿
              </n-button>
              <n-button
                attr-type="submit"
                type="primary"
                block
                :loading="isLoading"
              >
                {{ isAuthorMode ? '提交审核' : '发布' }}
              </n-button>
            </n-space>
            <n-alert v-if="error" type="error" class="error-message" style="margin-top: 16px;">
              {{ error }}
            </n-alert>
          </form>
        </n-card>

        <ArticleAiSidebar
          v-if="activeLang === 'zh' && !isAuthorMode"
          :title="article.title"
          :tags-hint="tagsInput"
          :get-context="getTextareaContext"
          @apply="applyAiResult"
        />
      </div>

      <RevisionHistoryDrawer
        v-if="isEditMode && article.id && activeLang === 'zh'"
        v-model="revisionDrawerOpen"
        kind="article"
        :resource-id="article.id"
        @restored="fetchArticleForEdit(article.id)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, reactive, computed } from 'vue';
import { ARTICLE_STATUS } from '../../api/article';
import { useRoute, useRouter } from 'vue-router';
import {
  NAlert,
  NButton,
  NCard,
  NFormItem,
  NInput,
  NSpace,
  NTab,
  NTabs,
} from 'naive-ui';
import { createArticle, updateArticle, getArticleDetail } from '../../api/article';
import { agentSuggestTags, agentSummary } from '../../api/agent';
import {
  getTranslation,
  saveTranslation,
  machineTranslateArticle,
  translationSeoAi,
  articleSeoAi,
} from '../../api/translation';
import ArticleAiSidebar from './ArticleAiSidebar.vue';
import RevisionHistoryDrawer from '../../components/RevisionHistoryDrawer.vue';
import { useToastStore } from '../../stores/toast';

const langTabs = [
  { key: 'zh', label: '原文' },
  { key: 'en', label: 'English' },
  { key: 'ja', label: '日本語' },
  { key: 'ko', label: '한국어' },
];

function emptyTrans() {
  return {
    title: '',
    summary: '',
    content: '',
    seoTitle: '',
    seoDescription: '',
    status: 1,
  };
}

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();

const isAuthorMode = computed(() => !!route.meta.authorMode);
const backPath = computed(() => (isAuthorMode.value ? '/my-articles' : '/admin'));

const contentRef = ref(null);

const isEditMode = ref(false);
const activeLang = ref('zh');
const article = ref({
  id: null,
  title: '',
  summary: '',
  content: '',
  seoTitle: '',
  seoDescription: '',
  tags: [],
});
const trans = reactive({
  en: emptyTrans(),
  ja: emptyTrans(),
  ko: emptyTrans(),
});
const transLoaded = reactive({ en: false, ja: false, ko: false });
const tagsInput = ref('');
const isLoading = ref(false);
const error = ref(null);
const aiSummaryLoading = ref(false);
const aiTagsLoading = ref(false);
const transBusy = ref(false);
const zhSeoBusy = ref(false);
const revisionDrawerOpen = ref(false);

const tagNamesParam = () =>
  tagsInput.value
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
    .join(',');

function getTextareaContext() {
  const el = contentRef.value?.$el?.querySelector('textarea') || contentRef.value;
  const c = article.value.content || '';
  if (!el) {
    return {
      content: c,
      selectionStart: c.length,
      selectionEnd: c.length,
      selectedText: '',
    };
  }
  const selectionStart = typeof el.selectionStart === 'number' ? el.selectionStart : c.length;
  const selectionEnd = typeof el.selectionEnd === 'number' ? el.selectionEnd : selectionStart;
  return {
    content: c,
    selectionStart,
    selectionEnd,
    selectedText: c.slice(selectionStart, selectionEnd),
  };
}

function applyAiResult({ mode, text }) {
  if (!text) return;
  const el = contentRef.value?.$el?.querySelector('textarea') || contentRef.value;
  const c = article.value.content || '';
  if (mode === 'replace') {
    const ctx = getTextareaContext();
    const start = ctx.selectionStart;
    const end = ctx.selectionEnd;
    if (start === end && el) {
      toastStore.push('请先选中要替换的文本', 'error');
      return;
    }
    article.value.content = c.slice(0, start) + text + c.slice(end);
    toastStore.push('已替换选中文本', 'success');
    nextTick(() => {
      if (!el) return;
      el.focus();
      const pos = start + text.length;
      el.setSelectionRange(pos, pos);
    });
    return;
  }
  const insertAt = el ? el.selectionStart : c.length;
  const safeAt = typeof insertAt === 'number' ? insertAt : c.length;
  article.value.content = c.slice(0, safeAt) + text + c.slice(safeAt);
  toastStore.push('已插入到光标处', 'success');
  nextTick(() => {
    if (!el) return;
    el.focus();
    const pos = safeAt + text.length;
    el.setSelectionRange(pos, pos);
  });
}

async function fillSummary() {
  aiSummaryLoading.value = true;
  error.value = null;
  try {
    article.value.summary = await agentSummary({
      title: article.value.title,
      content: article.value.content,
    });
  } catch (err) {
    error.value = err?.message || '摘要生成失败';
  } finally {
    aiSummaryLoading.value = false;
  }
}

async function fillTags() {
  aiTagsLoading.value = true;
  error.value = null;
  try {
    const tags = await agentSuggestTags({
      title: article.value.title,
      content: article.value.content,
    });
    const slice = tags.slice(0, 5);
    tagsInput.value = slice.join(', ');
  } catch (err) {
    error.value = err?.message || '标签推荐失败';
  } finally {
    aiTagsLoading.value = false;
  }
}

function resetTranslations() {
  activeLang.value = 'zh';
  ['en', 'ja', 'ko'].forEach((k) => {
    Object.assign(trans[k], emptyTrans());
    transLoaded[k] = false;
  });
}

async function ensureTrans(locale, force = false) {
  if (!article.value.id || locale === 'zh') return;
  if (force) transLoaded[locale] = false;
  if (transLoaded[locale]) return;
  try {
    const response = await getTranslation(article.value.id, locale);
    const data = response?.data;
    if (data && typeof data === 'object') {
      trans[locale].title = data.title || '';
      trans[locale].summary = data.summary || '';
      trans[locale].content = data.content || '';
      trans[locale].seoTitle = data.seoTitle || '';
      trans[locale].seoDescription = data.seoDescription || '';
      trans[locale].status = data.status != null ? data.status : 1;
    }
  } catch {
    /* ignore */
  }
  transLoaded[locale] = true;
}

async function setLang(key) {
  activeLang.value = key;
  if (key !== 'zh') await ensureTrans(key);
}

async function saveTrans() {
  const loc = activeLang.value;
  if (loc === 'zh' || !article.value.id) return;
  transBusy.value = true;
  error.value = null;
  try {
    await saveTranslation(article.value.id, {
      articleId: article.value.id,
      locale: loc,
      title: trans[loc].title,
      summary: trans[loc].summary || '',
      content: trans[loc].content,
      seoTitle: trans[loc].seoTitle || '',
      seoDescription: trans[loc].seoDescription || '',
      status: trans[loc].status != null ? trans[loc].status : 1,
    });
  } catch (err) {
    error.value = err?.message || '保存译文失败';
  } finally {
    transBusy.value = false;
  }
}

async function machineTrans() {
  const loc = activeLang.value;
  if (loc === 'zh' || !article.value.id) return;
  transBusy.value = true;
  error.value = null;
  try {
    await machineTranslateArticle(article.value.id, loc);
    await ensureTrans(loc, true);
  } catch (err) {
    error.value = err?.message || '机翻失败';
  } finally {
    transBusy.value = false;
  }
}

async function seoTrans() {
  const loc = activeLang.value;
  if (loc === 'zh' || !article.value.id) return;
  transBusy.value = true;
  error.value = null;
  try {
    await translationSeoAi(article.value.id, loc);
    await ensureTrans(loc, true);
  } catch (err) {
    error.value = err?.message || 'SEO 生成失败';
  } finally {
    transBusy.value = false;
  }
}

async function runZhSeoAi() {
  if (!article.value.id) return;
  zhSeoBusy.value = true;
  error.value = null;
  try {
    await articleSeoAi(article.value.id);
    await fetchArticleForEdit(article.value.id);
  } catch (err) {
    error.value = err?.message || 'SEO 生成失败';
  } finally {
    zhSeoBusy.value = false;
  }
}

const fetchArticleForEdit = async (id) => {
  error.value = null;
  try {
    const response = await getArticleDetail(id);
    const d = response.data;
    article.value = {
      id: d.id,
      title: d.title,
      content: d.content,
      summary: d.summary || '',
      seoTitle: d.seoTitle || '',
      seoDescription: d.seoDescription || '',
      tags: d.tags || [],
    };
    tagsInput.value = (d.tags || []).map((t) => t.name).join(', ');
    resetTranslations();
  } catch (err) {
    console.error('Failed to fetch article for edit:', err);
    error.value = '加载文章失败。';
  }
};

const handleSubmit = async (asDraft = false) => {
  if (activeLang.value !== 'zh') {
    error.value = '请先切换到「原文」标签再提交。';
    return;
  }

  isLoading.value = true;
  error.value = null;

  let status = ARTICLE_STATUS.PUBLISHED;
  if (isAuthorMode.value) {
    status = asDraft ? ARTICLE_STATUS.DRAFT : ARTICLE_STATUS.PENDING;
  }

  const payload = {
    title: article.value.title,
    content: article.value.content,
    summary: article.value.summary || '',
    seoTitle: article.value.seoTitle || '',
    seoDescription: article.value.seoDescription || '',
    status,
  };
  const tags = tagNamesParam();

  try {
    let result;
    if (isEditMode.value) {
      result = await updateArticle(article.value.id, payload, tags);
    } else {
      result = await createArticle(payload, tags);
    }
    const d = result?.data;
    toastStore.push(d?.message || '保存成功', 'success');
    if (!isEditMode.value && d?.id) {
      router.replace(isAuthorMode.value ? `/write/edit/${d.id}` : `/admin/edit/${d.id}`);
      isEditMode.value = true;
      await fetchArticleForEdit(d.id);
    }
    if (!asDraft) {
      router.push(backPath.value);
    }
  } catch (err) {
    error.value = err.message || '提交失败，请稍后再试。';
  } finally {
    isLoading.value = false;
  }
};

watch(
  () => route.params.id,
  (newId) => {
    isEditMode.value = !!newId && String(newId).length > 0;
    if (isEditMode.value) {
      fetchArticleForEdit(newId);
    } else {
      article.value = {
        id: null,
        title: '',
        summary: '',
        content: '',
        seoTitle: '',
        seoDescription: '',
        tags: [],
      };
      tagsInput.value = '';
      resetTranslations();
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.editor-shell {
  max-width: 1280px;
}

.editor-layout {
  display: flex;
  flex-wrap: wrap;
  gap: 1.35rem;
  align-items: flex-start;
}

.article-form-card {
  flex: 1;
  min-width: 0;
  background: var(--color-surface) !important;
  border: var(--border-brutal) !important;
}

.article-form-card :deep(.n-card) {
  background: transparent !important;
}

@media (min-width: 961px) {
  .editor-layout .article-form-card {
    max-width: calc(100% - 360px);
  }
}
</style>
