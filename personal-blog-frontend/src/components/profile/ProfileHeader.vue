<template>
  <div class="profile-header">
    <UserAvatar class="profile-header-avatar" :src="avatar" :name="displayName" :size="64" />
    <div class="profile-header-main">
      <div class="profile-header-top">
        <h1 class="profile-header-name">{{ displayName }}</h1>
        <div v-if="$slots.action" class="profile-header-action">
          <slot name="action" />
        </div>
      </div>
      <p v-if="bio" class="profile-header-bio muted">{{ bio }}</p>
      <n-space class="profile-header-stats muted" :size="12">
        <span>{{ followerCount }} 粉丝</span>
        <span>{{ followingCount }} 关注</span>
        <span v-if="points != null">{{ points }} 积分</span>
      </n-space>
      <BadgeStrip v-if="badges?.length" :badges="badges" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { NSpace } from 'naive-ui';
import UserAvatar from '../UserAvatar.vue';
import BadgeStrip from './BadgeStrip.vue';

const props = defineProps({
  user: { type: Object, required: true },
  badges: { type: Array, default: () => [] },
  points: { type: Number, default: null },
});

const displayName = computed(() => props.user?.nickname || props.user?.username || '用户');
const avatar = computed(() => props.user?.avatar);
const bio = computed(() => props.user?.bio);
const followerCount = computed(() => props.user?.followerCount ?? 0);
const followingCount = computed(() => props.user?.followingCount ?? 0);
</script>

<style scoped>
.profile-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
}

.profile-header-main {
  flex: 1;
  min-width: 0;
}

.profile-header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.profile-header-action {
  flex-shrink: 0;
}

.profile-header-name {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.profile-header-bio {
  margin: var(--space-2) 0 0;
  font-size: var(--text-sm);
  line-height: 1.5;
}

.profile-header-stats {
  margin-top: var(--space-2);
  font-size: var(--text-sm);
}

.muted {
  color: var(--color-text-muted);
}
</style>
