<template>
  <n-list v-if="visitors.length" bordered>
    <n-list-item v-for="v in visitors" :key="v.userId">
      <template #prefix>
        <router-link :to="`/user/${v.userId}`">
          <UserAvatar :src="v.avatar" :name="v.nickname" :size="40" />
        </router-link>
      </template>
      <router-link :to="`/user/${v.userId}`" class="visitor-name">{{ v.nickname || '用户' }}</router-link>
      <template #suffix>
        <span class="visitor-time muted">{{ formatTime(v.visitTime) }}</span>
      </template>
    </n-list-item>
  </n-list>
  <n-empty v-else description="暂无访客" />
</template>

<script setup>
import { NEmpty, NList, NListItem } from 'naive-ui';
import UserAvatar from '../UserAvatar.vue';

defineProps({
  visitors: { type: Array, default: () => [] },
});

function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  const diff = Date.now() - d.getTime();
  const hours = Math.floor(diff / 3600000);
  if (hours < 24) return `${hours || 1}小时前`;
  const days = Math.floor(hours / 24);
  if (days === 1) return '昨天';
  return `${days}天前`;
}
</script>

<style scoped>
.visitor-name {
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
  text-decoration: none;
  color: var(--color-text);
}

.visitor-time {
  font-size: var(--text-xs);
}

.muted {
  color: var(--color-text-muted);
}
</style>
