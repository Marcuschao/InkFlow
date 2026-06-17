<template>
  <n-list-item>
    <template #prefix>
      <router-link :to="`/user/${user.id}`">
        <UserAvatar :src="user.avatar" :name="user.nickname" :size="40" />
      </router-link>
    </template>
    <router-link :to="`/user/${user.id}`" class="name">{{ user.nickname || '用户' }}</router-link>
    <p v-if="user.mutualFollowCount > 0" class="meta muted">你们共同关注了 {{ user.mutualFollowCount }} 位作者</p>
    <p v-if="user.lastActiveTime" class="meta muted">最近活跃：{{ formatActive(user.lastActiveTime) }}</p>
    <template #suffix>
      <FollowButton :user-id="user.id" :following="user.following" @toggled="$emit('follow-changed')" />
    </template>
  </n-list-item>
</template>

<script setup>
import { NListItem } from 'naive-ui';
import FollowButton from './FollowButton.vue';
import UserAvatar from './UserAvatar.vue';

defineProps({
  user: { type: Object, required: true },
});

defineEmits(['follow-changed']);

function formatActive(t) {
  if (!t) return '';
  const d = new Date(t);
  const diff = Date.now() - d.getTime();
  const hours = Math.floor(diff / 3600000);
  if (hours < 1) return '刚刚';
  if (hours < 24) return `${hours}小时前`;
  const days = Math.floor(hours / 24);
  if (days === 1) return '昨天';
  return `${days}天前`;
}
</script>

<style scoped>
.name {
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
  text-decoration: none;
  color: var(--color-text);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 12rem;
}

.meta {
  margin: var(--space-1) 0 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}
</style>
