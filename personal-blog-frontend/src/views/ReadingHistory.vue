<template>
  <div class="rh-page ds-page">
    <div class="container">
      <header class="ds-page-hero rh-head-tight">
        <h1 class="ds-page-title ds-page-title-md">阅读记录</h1>
        <p class="ds-page-sub">本地保存，换设备不同步</p>
      </header>
      <ul v-if="entries.length" class="rh-list">
        <li v-for="row in entries" :key="row.id" class="rh-item">
          <router-link :to="`/article/${row.id}`" class="rh-link">
            <span class="rh-title">{{ row.title || '（无标题）' }}</span>
            <span class="rh-meta">
              <time>{{ formatTime(row.visitedAt) }}</time>
              <span v-if="row.scrollPercent > 0" class="rh-pct">已读 {{ row.scrollPercent }}%</span>
            </span>
          </router-link>
        </li>
      </ul>
      <p v-else class="rh-empty ds-empty-panel">暂无记录</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useReadingHistory } from '../composables/useReadingHistory';

const { listEntries } = useReadingHistory();
const entries = computed(() => listEntries());

function formatTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return d.toLocaleString();
}
</script>

<style scoped>
.rh-head-tight {
  margin-bottom: var(--space-8);
}

.rh-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.rh-item {
  margin: 0;
}

.rh-link {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 0.5rem 1rem;
  padding: 1rem 1.15rem;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
  text-decoration: none;
  color: inherit;
  transition:
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
}

.rh-link:hover {
  border-color: var(--border-accent-muted);
  box-shadow: var(--shadow-sm);
}

.rh-title {
  font-weight: 650;
  color: var(--color-text);
}

.rh-meta {
  font-size: 0.82rem;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.rh-pct {
  font-weight: 600;
  color: var(--color-primary);
}

.rh-empty {
  margin: 0;
}
</style>
