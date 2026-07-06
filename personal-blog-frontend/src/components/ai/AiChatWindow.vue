<template>
  <div
    v-if="visible"
    class="ai-chat-window ai-glass"
    :class="{ 'ai-chat-window--embedded': embedded, 'ai-chat-window--floating': !embedded }"
    :style="floatingStyle"
    role="dialog"
    aria-label="AI 知识助手"
  >
    <header class="ai-chat-header" @mousedown="onDragStart">
      <div class="ai-chat-header-text">
        <span class="ai-chat-brand">AI 知识助手</span>
        <span v-if="!embedded" class="ai-chat-session-name">{{ aiChat.currentSessionTitle }}</span>
      </div>
      <div class="ai-chat-header-actions">
        <n-button v-if="!embedded" size="tiny" quaternary @click="historyOpen = true">历史</n-button>
        <n-button v-if="!embedded" size="tiny" quaternary @click="aiChat.newSession()">新建</n-button>
        <button v-if="!embedded" type="button" class="ai-chat-icon-btn" aria-label="关闭" @click="aiChat.closeWindow()">×</button>
      </div>
    </header>

    <div ref="scrollRef" class="ai-chat-messages">
      <div v-if="showEmpty" class="ai-chat-empty">
        <p class="ai-chat-empty-title">向知识库提问</p>
        <p class="ai-chat-empty-sub">基于站内文档与文章，带引用溯源</p>
        <div class="ai-chat-samples">
          <button
            v-for="q in sampleQuestions"
            :key="q"
            type="button"
            class="ai-chat-sample-btn"
            @click="onSample(q)"
          >{{ q }}</button>
        </div>
      </div>
      <div v-for="(m, idx) in aiChat.messages" :key="idx" class="ai-msg-row" :class="m.role">
        <div class="ai-bubble" :class="m.role">
          {{ displayContent(m) }}<span v-if="m.streaming" class="ai-cursor-blink">▍</span>
        </div>
        <p v-if="m.role === 'assistant' && sourceLine(m)" class="ai-source-line">{{ sourceLine(m) }}</p>
        <div v-if="m.role === 'assistant' && m.content && !m.streaming" class="ai-msg-actions">
          <button type="button" class="ai-action-btn" @click="aiChat.copyMessage(displayContent(m))">复制</button>
          <button v-if="idx === aiChat.messages.length - 1" type="button" class="ai-action-btn" @click="aiChat.regenerate()">重新生成</button>
        </div>
      </div>
    </div>

    <form class="ai-chat-form" @submit.prevent="submit">
      <textarea
        ref="inputRef"
        v-model="draft"
        class="ai-chat-input"
        rows="1"
        placeholder="输入问题，Enter 发送"
        @keydown="onKeydown"
      />
      <n-button type="primary" :loading="aiChat.streaming" :disabled="!draft.trim()" attr-type="submit">发送</n-button>
    </form>

    <n-drawer v-if="!embedded" v-model:show="historyOpen" placement="left" :width="280">
      <n-drawer-content title="历史会话">
        <AiSessionList
          :sessions="aiChat.sessions"
          :active-id="aiChat.sessionId"
          @select="onSelectHistory"
          @new="onNewHistory"
          @delete="aiChat.removeSession"
        />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue';
import { NButton, NDrawer, NDrawerContent } from 'naive-ui';
import { useAiChatStore } from '../../stores/aiChat';
import { stripCitationMarkers, formatSourceLine } from '../../utils/aiChatFormat';
import AiSessionList from './AiSessionList.vue';

const props = defineProps({
  embedded: { type: Boolean, default: false },
  visible: { type: Boolean, default: true },
});

const aiChat = useAiChatStore();
const scrollRef = ref(null);
const inputRef = ref(null);
const draft = ref('');
const historyOpen = ref(false);

const sampleQuestions = [
  '博客有哪些技术主题？',
  '如何开始学习微服务？',
  '最近更新了哪些内容？',
];

const showEmpty = computed(() => !aiChat.messages.length && !aiChat.streaming);

watch(
  () => aiChat.draftQuestion,
  (v) => {
    if (v) draft.value = v;
  },
  { immediate: true }
);

watch(
  () => aiChat.messages,
  () => nextTick(scrollToBottom),
  { deep: true }
);

watch(
  () => props.visible,
  (v) => {
    if (v) {
      nextTick(scrollToBottom);
      aiChat.hydrateFromBackend();
    }
  }
);

async function onSelectHistory(id) {
  historyOpen.value = false;
  await aiChat.selectSession(id);
}

function onNewHistory() {
  aiChat.newSession();
  historyOpen.value = false;
}

const pos = ref({ x: null, y: null });
const dragging = ref(false);
const dragOffset = { x: 0, y: 0 };

const floatingStyle = computed(() => {
  if (props.embedded || pos.value.x == null) return {};
  return { left: `${pos.value.x}px`, top: `${pos.value.y}px`, right: 'auto', bottom: 'auto' };
});

function displayContent(m) {
  if (m.role !== 'assistant') return m.content || '';
  return stripCitationMarkers(m.content || '');
}

function sourceLine(m) {
  if (m.streaming || !m.sources?.length) return '';
  return formatSourceLine(m.sources);
}

function scrollToBottom() {
  const el = scrollRef.value;
  if (el) el.scrollTop = el.scrollHeight;
}

function onSample(q) {
  draft.value = q;
  submit();
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    submit();
  }
}

async function submit() {
  const text = draft.value.trim();
  if (!text) return;
  draft.value = '';
  await aiChat.send(text);
}

function onDragStart(e) {
  if (props.embedded) return;
  if (e.target.closest('button')) return;
  dragging.value = true;
  const rect = e.currentTarget.parentElement.getBoundingClientRect();
  dragOffset.x = e.clientX - rect.left;
  dragOffset.y = e.clientY - rect.top;
  document.addEventListener('mousemove', onDragMove);
  document.addEventListener('mouseup', onDragEnd);
}

function onDragMove(e) {
  if (!dragging.value) return;
  pos.value = {
    x: Math.max(8, e.clientX - dragOffset.x),
    y: Math.max(8, e.clientY - dragOffset.y),
  };
}

function onDragEnd() {
  dragging.value = false;
  document.removeEventListener('mousemove', onDragMove);
  document.removeEventListener('mouseup', onDragEnd);
}

onMounted(() => {
  if (props.embedded) aiChat.hydrateFromBackend();
});
</script>

<style scoped>
.ai-chat-window--floating {
  position: fixed;
  right: var(--space-4);
  bottom: calc(var(--ai-fab-size) + var(--space-6) + env(safe-area-inset-bottom, 0px));
  z-index: 1295;
  width: min(var(--ai-window-w), calc(100vw - var(--space-8)));
  height: min(var(--ai-window-h), calc(100vh - 8rem));
  display: flex;
  flex-direction: column;
  animation: ai-slide-in 0.25s var(--ease-out-soft);
}

.ai-chat-window--embedded {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.ai-chat-window {
  border: var(--border-brutal);
  border-radius: var(--radius-brutal-card);
  box-shadow: var(--shadow-brutal-lg);
  overflow: hidden;
  font-family: var(--font-ui, inherit);
}

.ai-chat-window--floating.ai-glass {
  backdrop-filter: blur(var(--ai-glass-blur));
  background: rgba(31, 31, 35, 0.92);
}

html:not(.dark) .ai-chat-window--floating.ai-glass {
  background: rgba(255, 255, 255, 0.94);
}

.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: var(--border-brutal);
  background: var(--color-accent);
  color: var(--color-on-primary);
  cursor: grab;
  user-select: none;
}

.ai-chat-brand {
  font-weight: var(--weight-bold);
  font-size: var(--text-sm);
}

.ai-chat-session-name {
  display: block;
  font-size: var(--text-xs);
  opacity: 0.85;
  margin-top: 2px;
}

.ai-chat-header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.ai-chat-icon-btn {
  border: none;
  background: transparent;
  font-size: 1.25rem;
  cursor: pointer;
  color: inherit;
  padding: var(--space-1);
}

.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--color-surface);
}

.ai-chat-empty {
  text-align: center;
  padding: var(--space-8) var(--space-4);
}

.ai-chat-empty-title {
  font-size: var(--text-lg);
  font-weight: var(--weight-bold);
  margin: 0 0 var(--space-2);
}

.ai-chat-empty-sub {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin: 0 0 var(--space-6);
}

.ai-chat-samples {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: center;
}

.ai-chat-sample-btn {
  border: var(--border-brutal);
  border-radius: var(--radius-pill);
  padding: var(--space-2) var(--space-4);
  background: var(--color-primary-soft);
  cursor: pointer;
  font-size: var(--text-sm);
  font-family: inherit;
  color: var(--color-text);
  max-width: 100%;
}

.ai-msg-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  max-width: 92%;
}

.ai-msg-row.user {
  align-self: flex-end;
  align-items: flex-end;
}

.ai-msg-row.assistant {
  align-self: flex-start;
}

.ai-bubble {
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-bubble.user {
  background: var(--color-primary-soft);
  border: var(--border-brutal);
}

.ai-bubble.assistant {
  background: var(--color-surface-raised);
  border: var(--border-brutal);
}

.ai-source-line {
  margin: 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-msg-actions {
  display: flex;
  gap: var(--space-2);
}

.ai-action-btn {
  border: none;
  background: transparent;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  cursor: pointer;
  font-family: inherit;
  padding: 0;
}

.ai-action-btn:hover {
  color: var(--color-accent);
}

.ai-chat-form {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-3);
  border-top: var(--border-brutal);
  background: var(--color-surface);
  align-items: flex-end;
}

.ai-chat-input {
  flex: 1;
  min-width: 0;
  resize: none;
  padding: var(--space-2) var(--space-3);
  border: var(--border-brutal);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-family: inherit;
  background: var(--surface-input);
  color: var(--color-text);
  max-height: 120px;
}

@keyframes ai-slide-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 767px) {
  .ai-chat-window--floating {
    right: 0;
    left: 0;
    bottom: 0;
    width: 100%;
    height: calc(100vh - var(--layout-navbar-bottom));
    border-radius: var(--radius-brutal-card) var(--radius-brutal-card) 0 0;
  }
}
</style>
