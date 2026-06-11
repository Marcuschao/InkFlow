<template>
  <n-card class="hot-card" :bordered="true">
    <template #header>
      <div class="hot-card-head">
        <span class="hot-card-title">{{ list?.sourceName || '热搜' }}</span>
        <router-link
          v-if="list?.source"
          :to="{ path: '/hot-search', query: { source: list.source } }"
          class="hot-card-more"
        >更多</router-link>
      </div>
    </template>
    <n-skeleton v-if="loading" text :repeat="5" />
    <n-empty v-else-if="!items.length" description="暂无数据" size="small" />
    <ol v-else class="hot-list">
      <li v-for="item in items" :key="item.rank + '-' + item.title" class="hot-item">
        <span class="hot-rank" :class="{ top: item.rank <= 3 }">{{ item.rank }}</span>
        <a :href="item.url" target="_blank" rel="noopener noreferrer" class="hot-link">{{ item.title }}</a>
        <span v-if="item.heat" class="hot-heat">{{ item.heat }}</span>
      </li>
    </ol>
  </n-card>
</template>

<script setup>
import { computed } from 'vue';
import { NCard, NEmpty, NSkeleton } from 'naive-ui';

const props = defineProps({
  list: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  limit: { type: Number, default: 5 },
});

const items = computed(() => (props.list?.items || []).slice(0, props.limit));
</script>

<style scoped>
.hot-card {
  min-width: 260px;
  height: 100%;
}

.hot-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.hot-card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.hot-card-more {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  text-decoration: none;
}

.hot-card-more:hover {
  color: var(--color-primary);
}

.hot-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.hot-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.hot-rank {
  flex-shrink: 0;
  width: 20px;
  text-align: center;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
}

.hot-rank.top {
  color: var(--color-primary);
}

.hot-link {
  flex: 1;
  min-width: 0;
  font-size: var(--text-sm);
  color: var(--color-text);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-link:hover {
  color: var(--color-primary);
}

.hot-heat {
  flex-shrink: 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}
</style>
