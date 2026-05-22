<template>
  <div class="public-user ds-page container">
    <div v-if="loading" class="pub-skel ui-skeleton" />
    <div v-else-if="!u" class="state-msg">用户不存在</div>
    <div v-else class="pub-panel">
      <div class="pub-head">
        <img v-if="u.avatar" :src="u.avatar" alt="" class="pub-avatar" />
        <div v-else class="pub-letter">{{ initials }}</div>
        <div class="pub-info">
          <h1 class="pub-name">{{ u.nickname || '用户' }}</h1>
          <p v-if="genderLabel || u.region" class="pub-meta muted">
            <span v-if="genderLabel">{{ genderLabel }}</span>
            <span v-if="u.region">{{ u.region }}</span>
          </p>
          <p class="counts muted">
            <span>{{ u.followerCount ?? 0 }} 粉丝</span>
            <span>{{ u.followingCount ?? 0 }} 关注</span>
          </p>
        </div>
        <FollowButton :user-id="u.id" :following="following" @update:following="following = $event" />
      </div>

      <nav class="profile-tabs" aria-label="用户主页">
        <button
          v-for="t in tabs"
          :key="t.id"
          type="button"
          class="tab-btn"
          :class="{ active: tab === t.id }"
          @click="tab = t.id"
        >{{ t.label }}</button>
      </nav>

      <div v-if="tab === 'profile'" class="tab-panel">
        <p v-if="u.bio" class="pub-bio">{{ u.bio }}</p>
        <p v-else class="empty-hint">暂无简介</p>
      </div>

      <div v-else-if="tab === 'following'" class="tab-panel">
        <div v-if="listLoading" class="list-skel ui-skeleton" />
        <p v-else-if="!followingList.length" class="empty-hint">暂无关注</p>
        <UserListItem
          v-for="item in followingList"
          :key="item.id"
          :user="item"
          @follow-changed="loadFollowing"
        />
      </div>

      <div v-else-if="tab === 'followers'" class="tab-panel">
        <div v-if="listLoading" class="list-skel ui-skeleton" />
        <p v-else-if="!followersList.length" class="empty-hint">暂无粉丝</p>
        <UserListItem
          v-for="item in followersList"
          :key="item.id"
          :user="item"
          @follow-changed="loadFollowers"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import { fetchPublicUser } from '../api/user';
import { getFollowStatus, fetchFollowers, fetchFollowing } from '../api/interaction';
import FollowButton from '../components/FollowButton.vue';
import UserListItem from '../components/UserListItem.vue';

const route = useRoute();
const loading = ref(true);
const u = ref(null);
const following = ref(false);
const tab = ref('profile');
const listLoading = ref(false);
const followingList = ref([]);
const followersList = ref([]);

const tabs = [
  { id: 'profile', label: '资料' },
  { id: 'following', label: '关注' },
  { id: 'followers', label: '粉丝' },
];

const initials = computed(() => (u.value?.nickname || '?').slice(0, 1));

const genderLabel = computed(() => {
  const g = u.value?.gender;
  if (g === 1) return '男';
  if (g === 2) return '女';
  return '';
});

useHead(() => ({
  title: u.value?.nickname ? `${u.value.nickname} · 用户` : '用户',
}));

async function loadFollowing() {
  if (!u.value?.id) return;
  listLoading.value = true;
  try {
    const res = await fetchFollowing(u.value.id, { page: 1, size: 50 });
    followingList.value = res.data?.records || [];
  } catch {
    followingList.value = [];
  } finally {
    listLoading.value = false;
  }
}

async function loadFollowers() {
  if (!u.value?.id) return;
  listLoading.value = true;
  try {
    const res = await fetchFollowers(u.value.id, { page: 1, size: 50 });
    followersList.value = res.data?.records || [];
  } catch {
    followersList.value = [];
  } finally {
    listLoading.value = false;
  }
}

async function load(id) {
  loading.value = true;
  tab.value = 'profile';
  try {
    const res = await fetchPublicUser(id);
    u.value = res.data;
    try {
      const fs = await getFollowStatus(id);
      following.value = !!fs.data?.following;
    } catch {
      following.value = false;
    }
  } catch {
    u.value = null;
  } finally {
    loading.value = false;
  }
}

watch(tab, (t) => {
  if (t === 'following') loadFollowing();
  if (t === 'followers') loadFollowers();
});

watch(
  () => Number(route.params.id),
  (id) => {
    if (Number.isFinite(id)) load(id);
  },
  { immediate: true }
);
</script>

<style scoped>
.public-user {
  padding-top: var(--space-8);
  padding-bottom: var(--space-16);
}

.pub-skel,
.list-skel {
  height: 8rem;
  border-radius: var(--radius-lg);
}

.pub-panel {
  max-width: 40rem;
  margin: 0 auto;
  background: var(--color-surface);
  padding: clamp(1.25rem, 3vw, 1.85rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.pub-head {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.pub-info {
  flex: 1;
  min-width: 0;
}

.pub-avatar {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.pub-letter {
  width: 4rem;
  height: 4rem;
  border-radius: 50%;
  background: var(--surface-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--weight-semibold);
  font-size: var(--text-lg);
  flex-shrink: 0;
}

.pub-name {
  margin: 0;
  font-size: var(--text-xl);
}

.pub-meta span + span::before {
  content: ' · ';
}

.counts span + span {
  margin-left: var(--space-4);
}

.muted {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.profile-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-6) 0;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--space-2);
}

.tab-btn {
  border: none;
  background: none;
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text-muted);
  cursor: pointer;
  font-family: inherit;
  border-radius: var(--radius-sm);
}

.tab-btn.active {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.pub-bio {
  white-space: pre-wrap;
}

.empty-hint {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  text-align: center;
  padding: var(--space-8) 0;
}
</style>
