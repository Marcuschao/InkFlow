<template>
  <Teleport to="body">
    <div v-if="modelValue" class="rev-root" role="dialog" aria-modal="true" aria-labelledby="rev-drawer-title">
      <div class="rev-backdrop" @click="close" />
      <aside class="rev-panel">
        <header class="rev-head">
          <h2 id="rev-drawer-title" class="rev-title">历史版本</h2>
          <button type="button" class="rev-icon-btn ds-btn ds-btn--ghost ds-btn--pill" @click="close">关闭</button>
        </header>

        <div class="rev-body">
          <p v-if="loading" class="rev-muted">加载中…</p>
          <p v-else-if="!revisions.length" class="rev-muted">暂无历史版本（保存后将自动生成）。</p>

          <ul v-else class="rev-list">
            <li
              v-for="r in revisions"
              :key="r.id"
              :class="['rev-li', { on: previewId === r.id }]"
              @click="selectPreview(r.id)"
            >
              <span class="rev-li-no">#{{ r.revisionNo }}</span>
              <span class="rev-li-meta">{{ formatTime(r.createdAt) }}</span>
              <span class="rev-li-remark">{{ r.remark || '—' }}</span>
            </li>
          </ul>

          <div v-if="previewDetail" class="rev-preview">
            <h3 class="rev-preview-title">预览</h3>
            <template v-if="kind === 'article'">
              <p class="rev-preview-line"><strong>标题</strong> {{ previewDetail.title || '—' }}</p>
              <p class="rev-preview-line"><strong>摘要</strong> {{ previewDetail.summary || '—' }}</p>
              <p class="rev-preview-line"><strong>标签</strong> {{ previewDetail.articleTags || '—' }}</p>
            </template>
            <template v-else>
              <p class="rev-preview-line"><strong>标题</strong> {{ previewDetail.title || '—' }}</p>
              <p class="rev-preview-line"><strong>日期</strong> {{ previewDetail.diaryDate || '—' }}</p>
              <p class="rev-preview-line"><strong>标签</strong> {{ previewDetail.diaryTags || '—' }}</p>
            </template>
            <textarea class="rev-preview-ta ds-textarea" readonly rows="12" :value="previewDetail.content || ''" />
            <div class="rev-preview-actions">
              <button type="button" class="ds-btn ds-btn--secondary ds-btn--pill" :disabled="restoreBusy" @click="doRestore">
                {{ restoreBusy ? '处理中…' : '回退到此版本' }}
              </button>
            </div>
          </div>

          <div class="rev-compare">
            <p class="rev-compare-label">对比正文（按行）</p>
            <div class="rev-compare-row">
              <select v-model="compareA" class="ds-input rev-select">
                <option value="" disabled>版本 A</option>
                <option v-for="r in revisions" :key="'a-' + r.id" :value="String(r.id)">
                  #{{ r.revisionNo }} · {{ formatTime(r.createdAt) }}
                </option>
              </select>
              <select v-model="compareB" class="ds-input rev-select">
                <option value="" disabled>版本 B</option>
                <option v-for="r in revisions" :key="'b-' + r.id" :value="String(r.id)">
                  #{{ r.revisionNo }} · {{ formatTime(r.createdAt) }}
                </option>
              </select>
              <button
                type="button"
                class="ds-btn ds-btn--ghost ds-btn--pill"
                :disabled="diffBusy || !compareA || !compareB || compareA === compareB"
                @click="runDiff"
              >
                {{ diffBusy ? '…' : '对比' }}
              </button>
            </div>
          </div>
        </div>

        <div v-if="diffLines.length" class="rev-diff-layer">
          <div class="rev-diff-backdrop" @click="diffLines = []" />
          <div class="rev-diff-modal">
            <RevisionDiffSideBySide :lines="diffLines" @close="diffLines = []" />
          </div>
        </div>
      </aside>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue';
import {
  listArticleVersions,
  getArticleVersion,
  restoreArticleVersion,
  diffArticleVersions,
} from '../api/article';
import {
  listDiaryVersions,
  getDiaryVersion,
  restoreDiaryVersion,
  diffDiaryVersions,
} from '../api/diary';
import RevisionDiffSideBySide from './RevisionDiffSideBySide.vue';
import { useToastStore } from '../stores/toast';

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  kind: { type: String, required: true },
  resourceId: { type: [Number, String], default: null },
});

const emit = defineEmits(['update:modelValue', 'restored']);

const toastStore = useToastStore();

const loading = ref(false);
const revisions = ref([]);
const previewId = ref(null);
const previewDetail = ref(null);
const restoreBusy = ref(false);
const diffBusy = ref(false);
const compareA = ref('');
const compareB = ref('');
const diffLines = ref([]);

function close() {
  emit('update:modelValue', false);
}

function formatTime(t) {
  if (!t) return '';
  const s = typeof t === 'string' ? t : '';
  return s.replace('T', ' ').slice(0, 19);
}

async function loadList() {
  const id = props.resourceId;
  if (id == null || id === '') return;
  loading.value = true;
  previewId.value = null;
  previewDetail.value = null;
  compareA.value = '';
  compareB.value = '';
  diffLines.value = [];
  try {
    let res;
    if (props.kind === 'article') {
      res = await listArticleVersions(id);
    } else {
      res = await listDiaryVersions(id);
    }
    revisions.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    revisions.value = [];
  } finally {
    loading.value = false;
  }
}

async function selectPreview(versionId) {
  previewId.value = versionId;
  previewDetail.value = null;
  const id = props.resourceId;
  try {
    let res;
    if (props.kind === 'article') {
      res = await getArticleVersion(id, versionId);
    } else {
      res = await getDiaryVersion(id, versionId);
    }
    previewDetail.value = res.data || null;
  } catch {
    previewDetail.value = null;
  }
}

async function doRestore() {
  if (!previewId.value || !previewDetail.value) return;
  if (!window.confirm('确定将当前内容回退到该版本？将自动生成新的版本记录。')) return;
  restoreBusy.value = true;
  try {
    const id = props.resourceId;
    if (props.kind === 'article') {
      await restoreArticleVersion(id, previewId.value);
    } else {
      await restoreDiaryVersion(id, previewId.value);
    }
    toastStore.push('已回退', 'success');
    emit('restored');
    await loadList();
    previewDetail.value = null;
    previewId.value = null;
  } catch (e) {
    toastStore.push(e?.message || '回退失败', 'error');
  } finally {
    restoreBusy.value = false;
  }
}

async function runDiff() {
  if (!compareA.value || !compareB.value || compareA.value === compareB.value) return;
  diffBusy.value = true;
  diffLines.value = [];
  try {
    const id = props.resourceId;
    let res;
    if (props.kind === 'article') {
      res = await diffArticleVersions(id, compareA.value, compareB.value);
    } else {
      res = await diffDiaryVersions(id, compareA.value, compareB.value);
    }
    const payload = res.data || {};
    diffLines.value = Array.isArray(payload.lines) ? payload.lines : [];
    if (!diffLines.value.length) {
      toastStore.push('无差异或对比失败', 'error');
    }
  } catch (e) {
    toastStore.push(e?.message || '对比失败', 'error');
  } finally {
    diffBusy.value = false;
  }
}

watch(
  () => [props.modelValue, props.resourceId, props.kind],
  ([open]) => {
    if (open) loadList();
  }
);
</script>

<style scoped>
.rev-root {
  position: fixed;
  inset: 0;
  z-index: 2400;
  display: flex;
  justify-content: flex-end;
  pointer-events: none;
}

.rev-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.38);
  pointer-events: auto;
  cursor: pointer;
}

.rev-panel {
  position: relative;
  width: min(440px, 100vw);
  max-width: 100%;
  height: 100%;
  background: var(--color-surface);
  border-left: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  pointer-events: auto;
  animation: rev-slide 0.28s var(--ease-out-soft, ease-out) both;
}

@keyframes rev-slide {
  from {
    transform: translateX(100%);
    opacity: 0.92;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .rev-panel {
    animation: none;
  }
}

.rev-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.rev-title {
  margin: 0;
  font-size: var(--text-lg);
  font-weight: 800;
  color: var(--color-text);
}

.rev-icon-btn {
  cursor: pointer;
}

.rev-body {
  flex: 1;
  overflow: auto;
  padding: var(--space-4);
}

.rev-muted {
  margin: 0 0 var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.rev-list {
  list-style: none;
  margin: 0 0 var(--space-4);
  padding: 0;
}

.rev-li {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.25rem 0.65rem;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  margin-bottom: var(--space-2);
  cursor: pointer;
  font-size: var(--text-sm);
  transition: background var(--transition-fast, 0.15s ease), border-color var(--transition-fast, 0.15s ease);
}

.rev-li:hover {
  border-color: var(--color-primary-soft, rgba(59, 130, 246, 0.35));
}

.rev-li.on {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
}

.rev-li-no {
  font-weight: 800;
  color: var(--color-primary);
}

.rev-li-meta {
  grid-column: 2;
  color: var(--color-text-muted);
  font-size: 0.78rem;
}

.rev-li-remark {
  grid-column: 1 / -1;
  color: var(--color-text);
  font-size: 0.8rem;
}

.rev-preview-title {
  margin: 0 0 var(--space-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

.rev-preview-line {
  margin: 0 0 0.35rem;
  font-size: 0.82rem;
  color: var(--color-text);
}

.rev-preview-ta {
  width: 100%;
  margin-top: var(--space-2);
  box-sizing: border-box;
  font-size: 0.82rem;
}

.rev-preview-actions {
  margin-top: var(--space-3);
}

.rev-compare {
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.rev-compare-label {
  margin: 0 0 var(--space-2);
  font-size: 0.78rem;
  font-weight: 650;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.rev-compare-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
}

.rev-select {
  flex: 1;
  min-width: 8rem;
  font-size: 0.82rem;
}

.rev-diff-layer {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4);
  pointer-events: none;
}

.rev-diff-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  pointer-events: auto;
  cursor: pointer;
}

.rev-diff-modal {
  position: relative;
  z-index: 1;
  width: min(960px, 100%);
  pointer-events: auto;
}

@media (max-width: 720px) {
  .rev-panel {
    width: 100%;
  }
}
</style>
