<template>
  <header class="profile-header">
    <div class="profile-header-identity">
      <UserAvatar
        class="profile-header-avatar"
        :src="avatar"
        :name="displayName"
        :size="96"
        :equipped-items="equippedItems"
      />

      <div class="profile-header-main">
        <p class="profile-header-kicker">InkFlow · 创作者档案</p>
        <div class="profile-header-top">
          <div class="profile-header-copy">
            <h1 class="profile-header-name" :class="nameColorClass">{{ displayName }}</h1>
            <p v-if="bio" class="profile-header-bio">{{ bio }}</p>
            <p v-else class="profile-header-bio profile-header-bio-empty">这个人还没有写下简介</p>
          </div>
          <div v-if="$slots.action" class="profile-header-action">
            <slot name="action" />
          </div>
        </div>

        <div class="profile-header-stats" aria-label="社交数据">
          <span class="profile-stat"><strong>{{ followerCount }}</strong> 粉丝</span>
          <span class="profile-stat"><strong>{{ followingCount }}</strong> 关注</span>
          <span v-if="points != null" class="profile-stat"><strong>{{ points }}</strong> 积分</span>
        </div>

        <BadgeStrip v-if="badges?.length" :badges="badges" />
      </div>
    </div>

    <dl class="profile-index" aria-label="作者档案索引">
      <div class="profile-index-item">
        <dt>身份</dt>
        <dd>{{ identityLabel }}</dd>
      </div>
      <div v-if="user.region" class="profile-index-item">
        <dt>常驻</dt>
        <dd>{{ user.region }}</dd>
      </div>
      <div v-if="user.registerRegion && user.registerRegion !== user.region" class="profile-index-item">
        <dt>加入地</dt>
        <dd>{{ user.registerRegion }}</dd>
      </div>
      <div v-if="user.id != null" class="profile-index-item profile-index-id">
        <dt>档案号</dt>
        <dd>#{{ user.id }}</dd>
      </div>
    </dl>
  </header>
</template>

<script setup>
import { computed } from 'vue';
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
const identityLabel = computed(() => {
  if (String(props.user?.role || '').includes('ADMIN')) return '站点管理者';
  return 'InkFlow 创作者';
});

</script>

<style scoped>
.profile-header {
  border-top: 2px solid var(--color-text);
  border-bottom: 1px solid var(--color-border-strong);
  animation: profile-enter 0.48s var(--ease-out-soft) both;
}

.profile-header-identity {
  display: grid;
  grid-template-columns: 6rem minmax(0, 1fr);
  gap: var(--space-6);
  align-items: center;
  padding: var(--space-8) 0 var(--space-6);
}

.profile-header-avatar {
  flex-shrink: 0;
  width: 6rem;
  height: 6rem;
  padding: 3px;
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
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.profile-header-copy {
  min-width: 0;
}

.profile-header-action {
  flex-shrink: 0;
}

.profile-header-name {
  margin: 0;
  max-width: 22ch;
  overflow-wrap: anywhere;
  font-family: var(--font-display);
  font-size: 2rem;
  line-height: 1.18;
  font-weight: var(--weight-semibold);
}

.profile-header-kicker {
  margin: 0 0 var(--space-2);
  color: var(--color-primary);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: var(--weight-medium);
  text-transform: uppercase;
}

.item-name-gold {
  color: var(--color-warn);
}

.item-name-pink {
  color: var(--color-accent-pink);
}

.profile-header-bio {
  margin: var(--space-2) 0 0;
  max-width: 46rem;
  color: var(--color-text-muted);
  font-family: var(--font-prose);
  font-size: var(--text-base);
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.profile-header-stats {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
  margin-top: var(--space-4);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.profile-stat strong {
  margin-right: var(--space-1);
  color: var(--color-text);
  font-family: var(--font-mono);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.profile-header-bio-empty {
  color: var(--color-text-soft);
}

.profile-index {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(12rem, 1fr));
  margin: 0;
  border-top: 1px solid var(--color-border);
  background: var(--surface-pages-well);
}

.profile-index-item {
  min-width: 0;
  padding: var(--space-3) var(--space-4);
  border-right: 1px solid var(--color-border);
}

.profile-index-item:last-child {
  border-right: 0;
}

.profile-index dt {
  margin: 0 0 2px;
  color: var(--color-text-soft);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.profile-index dd {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@keyframes profile-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 767px) {
  .profile-header-identity {
    grid-template-columns: 4.5rem minmax(0, 1fr);
    gap: var(--space-4);
    align-items: start;
    padding: var(--space-5) 0;
  }

  .profile-header-avatar {
    width: 4.5rem;
    height: 4.5rem;
  }

  .profile-header-avatar :deep(.n-avatar) {
    width: calc(4.5rem - 6px) !important;
    height: calc(4.5rem - 6px) !important;
  }

  .profile-header-top {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-3);
  }

  .profile-header-action {
    width: 100%;
  }

  .profile-header-name {
    font-size: 1.55rem;
  }

  .profile-header-bio {
    font-size: var(--text-sm);
  }

  .profile-header-stats {
    gap: var(--space-4);
  }

  .profile-index {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .profile-index-item:nth-child(2n) {
    border-right: 0;
  }

  .profile-index-item:nth-child(n + 3) {
    border-top: 1px solid var(--color-border);
  }

}

@media (prefers-reduced-motion: reduce) {
  .profile-header {
    animation: none;
  }
}
</style>
