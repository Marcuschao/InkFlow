<template>
  <div class="diary-editor-page admin-page">
    <div class="container editor-wrap">
      <header class="editor-head ds-admin-header" style="margin-bottom: 24px;">
        <div>
          <h1 class="ds-page-title">{{ isEdit ? '编辑日记' : '写日记' }}</h1>
          <p class="ds-page-sub">纯文本或 Markdown，支持插图粘贴上传</p>
        </div>
        <n-space class="head-actions" :size="12">
          <router-link to="/admin/diary/list">
            <n-button size="small">日记列表</n-button>
          </router-link>
          <n-button
            v-if="isEdit"
            size="small"
            secondary
            @click="revisionDrawerOpen = true"
          >
            历史版本
          </n-button>
          <n-button type="primary" size="small" :loading="saving" @click="save">
            保存
          </n-button>
        </n-space>
      </header>

      <n-card :bordered="true">
        <n-space class="meta-row" align="center" :size="24" style="margin-bottom: 16px;">
          <div class="meta-item">
            <span class="meta-label" style="margin-right: 8px;">日期</span>
            <n-date-picker
              v-model:formatted-value="diaryDate"
              value-format="yyyy-MM-dd"
              type="date"
              style="width: 160px;"
            />
          </div>
          <n-checkbox v-model:checked="useMarkdown">Markdown</n-checkbox>
          <n-checkbox v-model:checked="isPublic">公开</n-checkbox>
          <n-button
            v-if="useMarkdown"
            secondary
            size="small"
            @click="showPreview = !showPreview"
          >
            {{ showPreview ? '关闭预览' : '预览' }}
          </n-button>
        </n-space>

        <n-form-item label="标签（逗号分隔）">
          <n-input v-model:value="tags" placeholder="Java, 笔记" autocomplete="off" />
        </n-form-item>

        <div class="editor-split" :class="{ hasPreview: showPreview && useMarkdown }">
          <div class="pane write-pane">
            <n-input
              v-model:value="content"
              type="textarea"
              ref="taInputRef"
              :rows="20"
              placeholder="记录今天的学习…"
              spellcheck="false"
              @keydown="onKeydown"
              @paste="onPaste"
            />
            <div class="toolbar" style="margin-top: 12px;">
              <n-button secondary size="small" :loading="uploading" @click="pickImage">
                插入图片
              </n-button>
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
      </n-card>
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
import {
  NButton,
  NCard,
  NCheckbox,
  NDatePicker,
  NFormItem,
  NInput,
  NSpace,
} from 'naive-ui';
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

const taInputRef = ref(null);
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
  if (!useMarkdown.value) return '';
  return marked.parse(content.value || '');
});

async function load() {
  if (!isEdit.value) return;
  try {
    const res = await getDiary(props.id);
    const d = res.data;
    diaryDate.value = d.diaryDate ? d.diaryDate.slice(0, 10) : '';
    tags.value = d.tags || '';
    content.value = d.content || '';
    useMarkdown.value = Number(d.contentType) === 1;
    isPublic.value = Number(d.isPublic) === 1;
  } catch {
    /* toast */
  }
}

watch(() => props.id, load, { immediate: true });

function onKeydown(e) {
  if (e.key === 'Tab') {
    e.preventDefault();
    const el = taInputRef.value?.$el?.querySelector('textarea');
    if (!el) return;
    const start = el.selectionStart;
    const end = el.selectionEnd;
    content.value = content.value.substring(0, start) + '\t' + content.value.substring(end);
    nextTick(() => {
      el.selectionStart = el.selectionEnd = start + 1;
    });
  }
}

function pickImage() {
  fileRef.value?.click();
}

async function uploadAndInsert(file) {
  const el = taInputRef.value?.$el?.querySelector('textarea');
  uploading.value = true;
  try {
    const res = await uploadDiaryImage(file);
    const url = res.data?.url || res.data || '';
    if (!url) {
      toastStore.push('上传返回空地址', 'error');
      return;
    }
    const md = `![图片](${url})`;
    if (!el) {
      content.value += '\n' + md;
      return;
    }
    const start = el.selectionStart;
    const end = el.selectionEnd;
    content.value = content.value.substring(0, start) + md + content.value.substring(end);
    nextTick(() => {
      el.selectionStart = el.selectionEnd = start + md.length;
    });
  } catch (err) {
    toastStore.push(err?.message || '图片上传失败', 'error');
  } finally {
    uploading.value = false;
  }
}

function onFileChange(e) {
  const f = e.target.files?.[0];
  if (f) uploadAndInsert(f);
}

async function onPaste(e) {
  const items = e.clipboardData?.items || [];
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault();
      const f = item.getAsFile();
      if (f) {
        await uploadAndInsert(f);
      }
    }
  }
}

async function save() {
  saving.value = true;
  try {
    const payload = {
      diaryDate: diaryDate.value,
      title: content.value.split('\n')[0]?.substring(0, 50) || '日记',
      content: content.value,
      contentType: useMarkdown.value ? 1 : 0,
      isPublic: isPublic.value,
      tags: tags.value.trim(),
    };
    if (isEdit.value) {
      await updateDiary(props.id, payload);
      toastStore.push('日记已更新', 'success');
    } else {
      await createDiary(payload);
      toastStore.push('日记已创建', 'success');
    }
    router.push('/admin/diary/list');
  } catch {
    /* toast */
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.editor-split {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.editor-split.hasPreview {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.pane {
  min-width: 0;
}

.preview-pane {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  background: var(--surface-muted);
  overflow-y: auto;
  max-height: 480px;
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
