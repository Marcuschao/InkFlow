<template>
  <div class="article-editor-page admin-page">
    <div class="container editor-shell">
      <header class="editor-head ds-admin-header">
        <div>
          <h1 class="ds-page-title">{{ isEditMode ? '编辑文章' : '新建文章' }}</h1>
          <p class="ds-page-sub">支持 Markdown，提交后将在后台列表中显示</p>
        </div>
        <router-link to="/admin" class="admin-link-btn">返回管理</router-link>
      </header>

      <div v-if="isEditMode" class="lang-tabs">
        <button
          v-for="t in langTabs"
          :key="t.key"
          type="button"
          :class="['lang-tab', { on: activeLang === t.key }]"
          @click="setLang(t.key)"
        >
          {{ t.label }}
        </button>
      </div>

      <div class="editor-layout">
        <form class="article-form" @submit.prevent="handleSubmit">
          <template v-if="activeLang === 'zh'">
            <div class="form-group">
              <label class="ds-form-label" for="title">文章标题</label>
              <input class="ds-input" id="title" v-model="article.title" type="text" required />
            </div>

            <div class="form-group">
              <label class="ds-form-label" for="summary">摘要</label>
              <input class="ds-input" id="summary" v-model="article.summary" type="text" placeholder="可选，显示在列表卡片中" />
            </div>

            <div class="form-group">
              <label class="ds-form-label" for="tags">标签（逗号分隔）</label>
              <input class="ds-input" id="tags" v-model="tagsInput" type="text" placeholder="Vue, JavaScript, CSS" />
            </div>

            <div class="form-group form-group-grow">
              <label class="ds-form-label" for="content">正文（Markdown）</label>
              <textarea
                id="content"
                class="ds-textarea"
                ref="contentRef"
                v-model="article.content"
                rows="20"
                required
              />
            </div>

            <div class="form-group">
              <label class="ds-form-label" for="seoTitle">SEO 标题</label>
              <input class="ds-input" id="seoTitle" v-model="article.seoTitle" type="text" />
            </div>

            <div class="form-group">
              <label class="ds-form-label" for="seoDescription">SEO 描述</label>
              <input class="ds-input" id="seoDescription" v-model="article.seoDescription" type="text" />
            </div>

            <div class="ai-meta-row">
              <button
                type="button"
                class="ai-meta-btn ds-btn ds-btn--ghost"
                :disabled="aiSummaryLoading || !article.content.trim()"
                @click="fillSummary"
              >
                <span v-if="aiSummaryLoading" class="ds-spin" aria-hidden="true" />
                智能提取摘要
              </button>
              <button
                type="button"
                class="ai-meta-btn ds-btn ds-btn--ghost"
                :disabled="aiTagsLoading || !article.content.trim()"
                @click="fillTags"
              >
                <span v-if="aiTagsLoading" class="ds-spin" aria-hidden="true" />
                智能推荐标签
              </button>
              <button
                type="button"
                class="ai-meta-btn ds-btn ds-btn--ghost"
                :disabled="zhSeoBusy || !article.id"
                @click="runZhSeoAi"
              >
                <span v-if="zhSeoBusy" class="ds-spin" aria-hidden="true" />
                中文 SEO 助手
              </button>
            </div>
          </template>

          <template v-else>
            <div class="form-group">
              <label class="ds-form-label" :for="'tt-' + activeLang">标题</label>
              <input class="ds-input" :id="'tt-' + activeLang" v-model="trans[activeLang].title" type="text" required />
            </div>

            <div class="form-group">
              <label class="ds-form-label" :for="'ts-' + activeLang">摘要</label>
              <input class="ds-input" :id="'ts-' + activeLang" v-model="trans[activeLang].summary" type="text" />
            </div>

            <div class="form-group form-group-grow">
              <label class="ds-form-label" :for="'tc-' + activeLang">正文</label>
              <textarea
                class="ds-textarea"
                :id="'tc-' + activeLang" v-model="trans[activeLang].content" rows="20" required />
            </div>

            <div class="form-group">
              <label class="ds-form-label" :for="'st-' + activeLang">SEO 标题</label>
              <input class="ds-input" :id="'st-' + activeLang" v-model="trans[activeLang].seoTitle" type="text" />
            </div>

            <div class="form-group">
              <label class="ds-form-label" :for="'sd-' + activeLang">SEO 描述</label>
              <input class="ds-input" :id="'sd-' + activeLang" v-model="trans[activeLang].seoDescription" type="text" />
            </div>

            <div class="ai-meta-row">
              <button type="button" class="ai-meta-btn ds-btn ds-btn--ghost" :disabled="transBusy" @click="saveTrans">
                保存译文
              </button>
              <button type="button" class="ai-meta-btn ds-btn ds-btn--ghost" :disabled="transBusy" @click="machineTrans">
                机翻填充
              </button>
              <button type="button" class="ai-meta-btn ds-btn ds-btn--ghost" :disabled="transBusy" @click="seoTrans">
                译文 SEO 助手
              </button>
            </div>
          </template>

          <button
            v-if="activeLang === 'zh'"
            type="submit"
            class="submit-button ds-btn ds-btn--primary"
            :disabled="isLoading"
          >
            <span v-if="isLoading" class="ds-spin-lg" aria-hidden="true" />
            <span>{{ isLoading ? '提交中…' : '提交' }}</span>
          </button>
          <p v-if="error" :key="error" class="error-message ds-error-box">{{ error }}</p>
        </form>

        <ArticleAiSidebar
          v-if="activeLang === 'zh'"
          :title="article.title"
          :tags-hint="tagsInput"
          :get-context="getTextareaContext"
          @apply="applyAiResult"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
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

const tagNamesParam = () =>
  tagsInput.value
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
    .join(',');

function getTextareaContext() {
  const el = contentRef.value;
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
  const el = contentRef.value;
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

const handleSubmit = async () => {
  if (activeLang.value !== 'zh') {
    error.value = '请先切换到「原文」标签再提交。';
    return;
  }

  isLoading.value = true;
  error.value = null;

  const payload = {
    title: article.value.title,
    content: article.value.content,
    summary: article.value.summary || '',
    seoTitle: article.value.seoTitle || '',
    seoDescription: article.value.seoDescription || '',
    status: 1,
  };
  const tags = tagNamesParam();

  try {
    if (isEditMode.value) {
      await updateArticle(article.value.id, payload, tags);
    } else {
      await createArticle(payload, tags);
      article.value = {
        id: null,
        title: '',
        content: '',
        summary: '',
        seoTitle: '',
        seoDescription: '',
        tags: [],
      };
      tagsInput.value = '';
      resetTranslations();
    }
    router.push('/admin');
  } catch (err) {
    console.error('Submission Error:', err);
    error.value = err.message || err.response?.data?.message || '提交失败，请稍后再试。';
  } finally {
    isLoading.value = false;
  }
};

watch(
  () => route.params.id,
  (newId) => {
    isEditMode.value = !!newId;
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

.lang-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: center;
  margin-bottom: var(--space-5);
}

.lang-tab {
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--surface-muted);
  font-size: var(--text-sm);
  font-weight: 650;
  cursor: pointer;
  font-family: inherit;
  color: var(--color-text-muted);
}

.lang-tab.on {
  border-color: transparent;
  color: #fff;
  background: var(--gradient-cta);
}

.editor-layout {
  display: flex;
  flex-wrap: wrap;
  gap: 1.35rem;
  align-items: flex-start;
}

.article-form {
  flex: 1;
  min-width: 0;
  background: var(--color-surface);
  padding: clamp(1.65rem, 4vw, 2.35rem);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

@media (min-width: 961px) {
  .editor-layout .article-form {
    max-width: calc(100% - 360px);
  }
}

.form-group {
  margin-bottom: var(--space-5);
}

.form-group-grow {
  margin-bottom: 1.65rem;
}

.ai-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.ai-meta-btn {
  flex: 1;
  min-width: 8rem;
}

.submit-button {
  width: 100%;
  padding: 0.95rem var(--space-5);
  font-size: var(--text-lg);
}

.ds-error-box.error-message {
  margin-top: var(--space-4);
}

@media (prefers-reduced-motion: reduce) {
  .submit-button.ds-btn:hover:not(:disabled) {
    transform: none;
    filter: none;
  }
}
</style>
