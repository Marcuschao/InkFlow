<template>
  <n-timeline v-if="items.length">
    <n-timeline-item v-for="(item, i) in items" :key="i" :time="formatTime(item.eventTime)">
      {{ formatText(item) }}
    </n-timeline-item>
  </n-timeline>
  <n-empty v-else description="暂无动态" />
</template>

<script setup>
import { NEmpty, NTimeline, NTimelineItem } from 'naive-ui';

defineProps({
  items: { type: Array, default: () => [] },
});

function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  const diff = Date.now() - d.getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return '刚刚';
  if (mins < 60) return `${mins}分钟前`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}天前`;
  return d.toLocaleDateString();
}

function formatText(item) {
  const name = item.actorName || '用户';
  if (item.actionType === 'FOLLOW') return `${name} 关注了你`;
  if (item.actionType === 'LIKE') return `${name} 赞了你的文章《${item.articleTitle || ''}》`;
  if (item.actionType === 'FAVORITE') return `${name} 收藏了你的文章《${item.articleTitle || ''}》`;
  if (item.actionType === 'COMMENT') return `${name} 评论了你的文章《${item.articleTitle || ''}》`;
  return `${name} 与你互动`;
}
</script>
