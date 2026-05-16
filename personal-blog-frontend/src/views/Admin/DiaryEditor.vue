<template>
  <div class="diary-editor-page admin-page">
    <div class="container editor-wrap">
      <header class="editor-head ds-admin-header">
        <div>
          <h1 class="ds-page-title">{{ isEdit ? '编辑日记' : '写日记' }}</h1>
          <p class="ds-page-sub">纯文本或 Markdown，支持插图粘贴上传</p>
        </div>
        <div class="head-actions">
          <router-link to="/admin/diary/list" class="ds-btn ds-btn--secondary ds-btn--pill">日记列表</router-link>
          <button
            v-if="isEdit"
            type="button"
            class="ds-btn ds-btn--ghost ds-btn--pill"
            @click="revisionDrawerOpen = true"
          >
            历史版本
          </button>
          <button type="button" class="ds-btn ds-btn--primary ds-btn--pill" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </header>

      <div class="meta-row">
        <label class="meta-item">
          <span class="meta-label">日期</span>
          <input v-model="diaryDate" type="date" class="ds-input date-inp" />
        </label>
        <label class="meta-item chk">
          <input v-model="useMarkdown" type="checkbox" />
          <span>Markdown</span>
        </label>
        <label class="meta-item chk">
          <input v-model="isPublic" type="checkbox" />
          <span>公开</span>
        </label>
        <button
          v-if="useMarkdown"
          type="button"
          class="ds-btn ds-btn--ghost ds-btn--pill"
          @click="showPreview = !showPreview"
        >
          {{ showPreview ? '关闭预览' : '预览' }}
        </button>
      </div>

      <label class="tags-label">
        <span class="meta-label">标签（逗号分隔）</span>
        <input v-model="tags" type="text" class="ds-input" placeholder="Java, 笔记" autocomplete="off" />
      </label>

      <div class="editor-split" :class="{ hasPreview: showPreview && useMarkdown }">
        <div class="pane write-pane">
          <textarea
            ref="taRef"
            v-model="content"
            class="diary-textarea"
            placeholder="记录今天的学习…"
            spellcheck="false"
            @keydown="onKeydown"
            @paste="onPaste"
          />
          <div class="toolbar">
            <button type="button" class="ds-btn ds-btn--secondary ds-btn--pill" :disabled="uploading" @click="pickImage">
              {{ uploading ? '上传中…' : '插入图片' }}
            </button>
            <input
              ref="fileRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="sr-only"
              @change="onFileChange"
            />
          </div>
        </div>
        <div v-if="showPreview && useMarkdown" class="pane preview-pane markdown-renderer markdown-prose" v-html="previewHtml" />
      </div>
    </div>

    <RevisionHistoryDrawer
      v-if="isEdit"
      v-model="revisionDrawerOpen"
      kind="diary"
      :resource-id="Number(props.id)"
      @restored="load()"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { marked } from 'marked';
import { createDiary, getDiary, updateDiary } from '../../api/diary';
import { uploadDiaryImage } from '../../api/upload';
import RevisionHistoryDrawer from '../../components/RevisionHistoryDrawer.vue';
import { useToastStore } from '../../stores/toast';

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();

const props = defineProps({
  id: { type: [String, Number], default: undefined },
});

const taRef = ref(null);
const fileRef = ref(null);
const diaryDate = ref(new Date().toISOString().slice(0, 10));
const tags = ref('');
const content = ref('');
const useMarkdown = ref(false);
const isPublic = ref(false);
const showPreview = ref(false);
const saving = ref(false);
const uploading = ref(false);
const revisionDrawerOpen = ref(false);

const isEdit = computed(() => props.id != null && props.id !== '' && !Number.isNaN(Number(props.id)));

const previewHtml = computed(() => {
  if (!useMarkdown.value || !content.value) return '';
  return marked.parse(content.value, { async: false });
});

function insertSnippet(snippet) {
  const ta = taRef.value;
  if (!ta) {
    content.value += snippet;
    return;
  }
  const start = ta.selectionStart;
  const end = ta.selectionEnd;
  const v = content.value;
  content.value = v.slice(0, start) + snippet + v.slice(end);
  nextTick(() => {
    const pos = start + snippet.length;
    ta.selectionStart = ta.selectionEnd = pos;
    ta.focus();
  });
}

async function uploadAndInsert(file) {
  if (!file || !file.type.startsWith('image/')) return;
  uploading.value = true;
  try {
    const res = await uploadDiaryImage(file);
    const url = res.data;
    if (!url || typeof url !== 'string') throw new Error('无返回地址');
    const snippet = useMarkdown.value ? `![](${url})\n` : `${url}\n`;
    insertSnippet(snippet);
  } catch (e) {
    toastStore.push(e?.message || '上传失败', 'error');
  } finally {
    uploading.value = false;
  }
}

function pickImage() {
  fileRef.value?.click();
}

function onFileChange(e) {
  const f = e.target.files?.[0];
  if (f) uploadAndInsert(f);
  e.target.value = '';
}

async function onPaste(e) {
  const items = e.clipboardData?.files;
  if (!items?.length) return;
  for (const f of items) {
    if (f.type.startsWith('image/')) {
      e.preventDefault();
      await uploadAndInsert(f);
      break;
    }
  }
}

function onKeydown(e) {
  if (e.ctrlKey && (e.key === 's' || e.key === 'S')) {
    e.preventDefault();
    save();
  }
}

async function load() {
  if (!isEdit.value) return;
  try {
    const res = await getDiary(Number(props.id));
    const d = res.data;
    if (!d) return;
    diaryDate.value = (d.diaryDate || '').toString().slice(0, 10);
    tags.value = d.tags || '';
    content.value = d.content || '';
    useMarkdown.value = Number(d.contentType) === 1;
    isPublic.value = Number(d.isPublic) === 1;
  } catch {
    router.replace('/admin/diary/list');
  }
}

async function save() {
  if (!content.value.trim()) {
    toastStore.push('内容不能为空', 'error');
    return;
  }
  saving.value = true;
  const body = {
    diaryDate: diaryDate.value || undefined,
    title: '',
    content: content.value,
    contentType: useMarkdown.value ? 1 : 0,
    tags: tags.value.trim(),
    isPublic: isPublic.value,
  };
  try {
    if (isEdit.value) {
      await updateDiary(Number(props.id), body);
      toastStore.push('已保存', 'success');
    } else {
      const res = await createDiary(body);
      const nid = res.data;
      toastStore.push('已保存', 'success');
      if (nid != null) router.replace(`/admin/diary/edit/${nid}`);
    }
  } catch (e) {
    toastStore.push(e?.message || '保存失败', 'error');
  } finally {
    saving.value = false;
  }
}

watch(
  () => props.id,
  () => load(),
  { immediate: true }
);
</script>

<style scoped>
.editor-wrap {
  max-width: 960px;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.meta-item.chk span {
  color: var(--color-text-muted);
}

.meta-label {
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.date-inp {
  width: auto;
  min-width: 11rem;
}

.tags-label {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.editor-split {
  display: grid;
  gap: var(--space-4);
}

.editor-split.hasPreview {
  grid-template-columns: 1fr 1fr;
}

@media (max-width: 900px) {
  .editor-split.hasPreview {
    grid-template-columns: 1fr;
  }
}

.diary-textarea {
  width: 100%;
  min-height: 420px;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 0.9rem;
  line-height: 1.55;
  background: var(--surface-input);
  resize: vertical;
}

.toolbar {
  margin-top: var(--space-3);
}

.preview-pane {
  min-height: 200px;
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
  overflow: auto;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}
</style>
