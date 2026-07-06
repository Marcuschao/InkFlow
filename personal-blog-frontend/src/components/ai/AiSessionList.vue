<template>
  <aside
    v-if="showSessionList"
    class="ai-session-list"
    aria-label="对话历史"
  >
    <div class="ai-session-head">
      <span class="ai-session-head-title">历史会话</span>
      <n-button size="tiny" quaternary @click="$emit('new')">新建</n-button>
    </div>
    <div v-if="!sessions.length" class="ai-session-empty">暂无会话</div>
    <ul v-else class="ai-session-items">
      <li
        v-for="s in sessions"
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
            <button type="button" class="ai-session-del" aria-label="删除">×</button>
          </template>
          删除此会话？
        </n-popconfirm>
      </li>
    </ul>
  </aside>
</template>

<script setup>
import { NButton, NPopconfirm } from 'naive-ui';

defineProps({
  sessions: { type: Array, default: () => [] },
  activeId: { type: Number, default: null },
  showSessionList: { type: Boolean, default: true },
});

defineEmits(['select', 'new', 'delete']);

function formatTime(t) {
  if (!t) return '';
  return new Date(t).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}
</script>

<style scoped>
.ai-session-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  border-right: var(--border-brutal);
  background: var(--color-surface);
  min-width: 0;
}

.ai-session-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: var(--border-brutal);
}

.ai-session-head-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
}

.ai-session-empty {
  padding: var(--space-6) var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.ai-session-items {
  list-style: none;
  margin: 0;
  padding: var(--space-2);
  overflow-y: auto;
  flex: 1;
}

.ai-session-item {
  display: flex;
  align-items: stretch;
  gap: var(--space-1);
  margin-bottom: var(--space-1);
}

.ai-session-item.active .ai-session-btn {
  border-left: 3px solid var(--color-accent);
  background: var(--color-primary-soft);
}

.ai-session-btn {
  flex: 1;
  text-align: left;
  padding: var(--space-2) var(--space-3);
  border: none;
  border-radius: var(--radius-md);
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
  width: 28px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  font-size: 1.1rem;
  border-radius: var(--radius-sm);
}

.ai-session-del:hover {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}
</style>
