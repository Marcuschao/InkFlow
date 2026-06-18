<template>
  <div class="profile-header">
    <UserAvatar
      class="profile-header-avatar"
      :src="avatar"
      :name="displayName"
      :size="64"
      :equipped-items="equippedItems"
    />
    <div class="profile-header-main">
      <div class="profile-header-top">
        <h1 class="profile-header-name" :class="nameColorClass">{{ displayName }}</h1>
        <div v-if="$slots.action" class="profile-header-action">
          <slot name="action" />
        </div>
      </div>
      <p v-if="bio" class="profile-header-bio muted">{{ bio }}</p>
      <n-space class="profile-header-stats muted" :size="12">
        <span><span class="ds-stat-num">{{ followerCount }}</span> 粉丝</span>
        <span><span class="ds-stat-num">{{ followingCount }}</span> 关注</span>
        <span v-if="points != null"><span class="ds-stat-num">{{ points }}</span> 积分</span>
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
import { effectClass } from '../../utils/itemEffects';

const props = defineProps({
  user: { type: Object, required: true },
  badges: { type: Array, default: () => [] },
  points: { type: Number, default: null },
  equippedItems: { type: Array, default: () => [] },
});

const displayName = computed(() => props.user?.nickname || props.user?.username || '用户');
const avatar = computed(() => props.user?.avatar);
const bio = computed(() => props.user?.bio);
const followerCount = computed(() => props.user?.followerCount ?? 0);
const followingCount = computed(() => props.user?.followingCount ?? 0);
const nameColorClass = computed(() => effectClass(props.equippedItems, 'NICKNAME_COLOR'));

</script>

<style scoped>
.profile-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
}

.profile-header-avatar {
  flex-shrink: 0;
  padding: 2px;
  border-radius: 50%;
  border: 1px solid var(--color-border-strong);
  background: transparent;
  box-shadow: none;
}

.profile-header-avatar :deep(.n-avatar) {
  border: 2px solid var(--color-page);
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

.item-name-gold {
  color: var(--color-warn);
}

.item-name-pink {
  color: var(--color-accent-pink);
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
