<template>
  <aside class="article-ai-sidebar" :class="{ collapsed }">
    <button type="button" class="ai-collapse-toggle" @click="collapsed = !collapsed">
      {{ collapsed ? '展开 AI' : '收起' }}
    </button>
    <div v-show="!collapsed" class="ai-panel-inner">
      <p class="ai-panel-title">写作助手</p>
      <label class="ai-label">关键词 / 补充说明（可选）</label>
      <textarea v-model="helperInput" class="ai-textarea ai-helper" rows="3" placeholder="大纲时可填写侧重点…" />
      <div class="ai-actions">
        <button type="button" class="ai-btn" :disabled="!!busy" @click="runOutline">
          <span v-if="busy === 'outline'" class="mini-spin" aria-hidden="true" />
          生成大纲
        </button>
        <button type="button" class="ai-btn" :disabled="!!busy" @click="runContinue">
          <span v-if="busy === 'continue'" class="mini-spin" aria-hidden="true" />
          续写内容
        </button>
        <button type="button" class="ai-btn" :disabled="!!busy" @click="runPolish">
          <span v-if="busy === 'polish'" class="mini-spin" aria-hidden="true" />
          润色 / 纠错
        </button>
      </div>
      <label class="ai-label">生成结果</label>
      <textarea v-model="resultText" class="ai-textarea ai-result" rows="10" readonly />
      <div class="ai-row">
        <button type="button" class="ai-btn-secondary" :disabled="!resultText" @click="copyResult">复制</button>
        <button type="button" class="ai-btn-secondary" :disabled="!resultText" @click="applyInsert">
          应用到正文（插入光标）
        </button>
        <button type="button" class="ai-btn-secondary" :disabled="!resultText" @click="applyReplace">
          替换选中文本
        </button>
      </div>
      <p v-if="aiError" class="ai-error">{{ aiError }}</p>
    </div>
  </aside>
</template>

<script setup>
import { ref } from 'vue';
import { agentEditorContinue, agentEditorOutline, agentEditorPolish } from '../../api/agent';

const props = defineProps({
  title: { type: String, default: '' },
  tagsHint: { type: String, default: '' },
  getContext: { type: Function, required: true },
});

const emit = defineEmits(['apply']);

const collapsed = ref(false);
const helperInput = ref('');
const resultText = ref('');
const busy = ref('');
const aiError = ref('');

const keywordsMerged = () => {
  const a = (props.tagsHint || '').trim();
  const b = helperInput.value.trim();
  if (a && b) return `${a}；${b}`;
  return a || b;
};

const runOutline = async () => {
  aiError.value = '';
  busy.value = 'outline';
  try {
    const ctx = props.getContext();
    resultText.value = await agentEditorOutline({
      title: props.title,
      keywords: keywordsMerged(),
      content: ctx.content,
    });
  } catch (e) {
    aiError.value = e?.message || '生成失败';
  } finally {
    busy.value = '';
  }
};

const runContinue = async () => {
  aiError.value = '';
  busy.value = 'continue';
  try {
    const ctx = props.getContext();
    resultText.value = await agentEditorContinue({
      title: props.title,
      keywords: keywordsMerged(),
      content: ctx.content,
      selectionStart: ctx.selectionStart,
      selectionEnd: ctx.selectionEnd,
    });
  } catch (e) {
    aiError.value = e?.message || '续写失败';
  } finally {
    busy.value = '';
  }
};

const runPolish = async () => {
  aiError.value = '';
  busy.value = 'polish';
  try {
    const ctx = props.getContext();
    let selectedText = ctx.selectedText || '';
    if (!selectedText.trim()) {
      const pad = 400;
      const i = ctx.selectionStart;
      selectedText = ctx.content.slice(Math.max(0, i - pad), Math.min(ctx.content.length, i + pad));
    }
    if (!selectedText.trim()) {
      aiError.value = '请先选中需要润色的文本，或输入正文。';
      busy.value = '';
      return;
    }
    resultText.value = await agentEditorPolish({
      title: props.title,
      content: ctx.content,
      selectedText,
      selectionStart: ctx.selectionStart,
      selectionEnd: ctx.selectionEnd,
    });
  } catch (e) {
    aiError.value = e?.message || '润色失败';
  } finally {
    busy.value = '';
  }
};

const copyResult = async () => {
  try {
    await navigator.clipboard.writeText(resultText.value);
  } catch {
    aiError.value = '复制失败';
  }
};

const applyInsert = () => {
  emit('apply', { mode: 'insert', text: resultText.value });
};

const applyReplace = () => {
  emit('apply', { mode: 'replace', text: resultText.value });
};
</script>

<style scoped>
.article-ai-sidebar {
  flex-shrink: 0;
  width: min(100%, 340px);
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.article-ai-sidebar.collapsed {
  width: auto;
  align-self: flex-start;
}

.ai-collapse-toggle {
  width: 100%;
  padding: 0.65rem 1rem;
  border: none;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: 650;
  font-size: 0.85rem;
  cursor: pointer;
  font-family: inherit;
}

.article-ai-sidebar.collapsed .ai-collapse-toggle {
  width: auto;
  min-width: 5rem;
  border-radius: var(--radius-md);
}

.ai-panel-inner {
  padding: 1rem 1.05rem 1.25rem;
}

.ai-panel-title {
  margin: 0 0 0.85rem;
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text);
}

.ai-label {
  display: block;
  font-size: 0.72rem;
  font-weight: 650;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
  margin-bottom: 0.35rem;
}

.ai-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 0.65rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  font-family: inherit;
  font-size: 0.88rem;
  resize: vertical;
  margin-bottom: 0.75rem;
  background: rgba(248, 250, 252, 0.65);
}

.ai-helper {
  min-height: 4rem;
}

.ai-result {
  font-family: ui-monospace, 'SF Mono', Menlo, Monaco, Consolas, monospace;
  font-size: 0.82rem;
  min-height: 10rem;
}

.ai-actions {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  margin-bottom: 0.85rem;
}

.ai-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.55rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-accent-strong);
  background: #fff;
  color: var(--color-primary);
  font-weight: 650;
  font-size: 0.82rem;
  cursor: pointer;
  font-family: inherit;
}

.ai-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.ai-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.45rem;
  margin-top: 0.35rem;
}

.ai-btn-secondary {
  flex: 1;
  min-width: 6rem;
  padding: 0.48rem 0.55rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
  color: var(--color-text-muted);
}

.ai-btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ai-error {
  margin: 0.65rem 0 0;
  font-size: 0.78rem;
  color: #b91c1c;
}

.mini-spin {
  width: 0.85rem;
  height: 0.85rem;
  border-radius: 50%;
  border: 2px solid var(--border-accent-muted);
  border-top-color: var(--color-primary);
  animation: spin 0.7s linear infinite;
}

@media (max-width: 960px) {
  .article-ai-sidebar {
    width: 100%;
    order: 2;
  }

  .article-ai-sidebar.collapsed {
    width: 100%;
  }
}
</style>
