<template>
  <aside
    v-if="showSessionList"
    class="ai-session-list"
    aria-label="对话历史"
  >
    <div class="ai-session-head">
      <div>
        <span class="ai-session-eyebrow">YOUR LIBRARY</span>
        <span class="ai-session-head-title">对话记录</span>
      </div>
      <n-button quaternary circle size="small" title="新建对话" aria-label="新建对话" @click="$emit('new')">
        <template #icon><SquarePen :size="16" /></template>
      </n-button>
    </div>
    <label v-if="sessions.length > 4" class="ai-session-search">
      <Search :size="15" />
      <input v-model="keyword" type="search" placeholder="搜索对话" />
    </label>
    <div v-if="!filteredSessions.length" class="ai-session-empty">
      <MessageSquareText :size="22" />
      <span>{{ keyword ? '没有匹配的对话' : '开始提问后，对话会保存在这里' }}</span>
    </div>
    <ul v-else class="ai-session-items">
      <li
        v-for="s in filteredSessions"
        :key="s.id"
        class="ai-session-item"
        :class="{ active: s.id === activeId }"
      >
        <button type="button" class="ai-session-btn" @click="$emit('select', s.id)">
          <span class="ai-session-title">{{ s.title || '新对话' }}</span>
          <time v-if="s.updateTime || s.createTime" class="ai-session-time">
            {{ formatTime(s.updateTime || s.createTime) }}
          </time>
        </button>
        <n-popconfirm @positive-click="$emit('delete', s.id)">
          <template #trigger>
            <button type="button" class="ai-session-del" title="删除对话" aria-label="删除对话"><Trash2 :size="14" /></button>
          </template>
          删除此会话？
        </n-popconfirm>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { computed, ref } from 'vue';
import { NButton, NPopconfirm } from 'naive-ui';
import { MessageSquareText, Search, SquarePen, Trash2 } from 'lucide-vue-next';

defineEmits(['select', 'new', 'delete']);

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  activeId: { type: Number, default: null },
  showSessionList: { type: Boolean, default: true },
});

const keyword = ref('');
const filteredSessions = computed(() => {
  const q = keyword.value.trim().toLowerCase();
  if (!q) return props.sessions;
  return props.sessions.filter((session) => (session.title || '新对话').toLowerCase().includes(q));
});

function formatTime(t) {
  if (!t) return '';
  return new Date(t).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
</script>

<style scoped>
.ai-session-list {
  display: flex;
  flex-direction: column;
  height: max-content;
  min-height: 20rem;
  background: transparent;
  min-width: 0;
}

.ai-session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.ai-session-head > div { min-width: 0; }

.ai-session-eyebrow {
  display: block;
  margin-bottom: 3px;
  color: var(--color-text-soft);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: .05em;
}

.ai-session-head-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
}

.ai-session-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-10) var(--space-5);
  font-size: var(--text-sm);
  line-height: 1.5;
  text-align: center;
  color: var(--color-text-muted);
}

.ai-session-search {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-3) var(--space-3) var(--space-1);
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-soft);
  background: var(--surface-input);
}

.ai-session-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: 0;
  color: var(--color-text);
  background: transparent;
  font: inherit;
  font-size: var(--text-xs);
}

.ai-session-items {
  list-style: none;
  margin: 0;
  padding: var(--space-4) 0;
  overflow: visible;
  flex: none;
}

.ai-session-item {
  display: flex;
  align-items: stretch;
  gap: var(--space-1);
  margin-bottom: 6px;
}

.ai-session-item.active .ai-session-btn {
  border-left: 2px solid var(--color-accent);
  border-radius: 0 6px 6px 0;
  background: var(--color-primary-soft);
}

.ai-session-btn {
  flex: 1;
  text-align: left;
  padding: 12px var(--space-3);
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  color: var(--color-text);
}

.ai-session-btn:hover {
  background: var(--surface-muted);
}

.ai-session-title {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-session-time {
  display: block;
  margin-top: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.ai-session-del {
  display: grid;
  place-items: center;
  width: 28px;
  opacity: 0;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  border-radius: var(--radius-sm);
}

.ai-session-item:hover .ai-session-del,
.ai-session-item:focus-within .ai-session-del { opacity: 1; }

.ai-session-del:hover {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}
</style>
