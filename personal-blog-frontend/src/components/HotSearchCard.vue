<template>
  <n-card class="hot-card ds-brutal-surface" :bordered="false">
    <template #header>
      <div class="hot-card-head">
        <div class="hot-card-title-wrap">
          <span
            v-if="list?.source"
            class="platform-icon"
            :style="{ background: getPlatformMeta(list.source).color }"
          >
            {{ getPlatformMeta(list.source).short }}
          </span>
          <span class="hot-card-title">{{ list?.sourceName || '热搜' }}</span>
        </div>
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
        <span class="hot-rank" :class="rankClass(item.rank)">{{ item.rank }}</span>
        <a :href="item.url" target="_blank" rel="noopener noreferrer" class="hot-link">{{ item.title }}</a>
        <span v-if="item.heat" class="hot-heat">
          <n-icon :component="FlameOutline" :size="12" />
          {{ item.heat }}
        </span>
      </li>
    </ol>
  </n-card>
</template>

<script setup>
import { computed } from 'vue';
import { NCard, NEmpty, NIcon, NSkeleton } from 'naive-ui';
import { FlameOutline } from '@vicons/ionicons5';
import { getPlatformMeta } from '../constants/hotSearchPlatforms';

const props = defineProps({
  list: { type: Object, default: null },
  loading: { type: Boolean, default: false },
  limit: { type: Number, default: 5 },
});

const items = computed(() => (props.list?.items || []).slice(0, props.limit));

function rankClass(rank) {
  if (rank === 1) return 'hot-rank--1';
  if (rank === 2) return 'hot-rank--2';
  if (rank === 3) return 'hot-rank--3';
  return 'hot-rank--default';
}
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

.hot-card-title-wrap {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.hot-card-title {
  font-size: var(--text-base);
  font-weight: var(--weight-bold);
  color: var(--color-text);
}

.hot-card-more {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
  text-decoration: none;
}

.hot-card-more:hover {
  color: var(--color-text);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.platform-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-md);
  font-size: 10px;
  font-weight: var(--weight-bold);
  color: #ffffff;
  flex-shrink: 0;
  border: var(--border-brutal);
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  border: var(--border-brutal);
}

.hot-rank--1 {
  background: #ef4444;
  color: #ffffff;
}

.hot-rank--2 {
  background: #f97316;
  color: #ffffff;
}

.hot-rank--3 {
  background: var(--color-accent);
  color: var(--color-on-primary);
}

.hot-rank--default {
  background: var(--surface-muted);
  color: var(--color-text-muted);
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
  text-decoration: underline;
  text-underline-offset: 2px;
}

.hot-heat {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--color-accent-orange);
}
</style>
