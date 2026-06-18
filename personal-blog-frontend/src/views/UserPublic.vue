<template>
  <div class="public-user ds-page container">
    <UserProfileSkeleton v-if="loading" />
    <n-empty v-else-if="!u" description="用户不存在" />
    <n-card v-else class="pub-panel ds-brutal-surface">
      <template #header>
        <ProfileHeader
          :user="u"
          :badges="socialCard?.badges || []"
          :points="socialCard?.points"
          :equipped-items="socialCard?.equippedItems || []"
        >
          <template #action>
            <FollowButton
              class="user-head-action"
              :user-id="u.id"
              :following="following"
              @update:following="following = $event"
            />
          </template>
        </ProfileHeader>
      </template>

      <n-tabs type="line" :value="tab" @update:value="tab = $event">
        <n-tab-pane v-for="t in tabs" :key="t.id" :name="t.id" :tab="t.label">
          <div v-if="t.id === 'activity'" class="tab-panel">
            <InteractionTimeline :items="timelineItems" />
          </div>

          <div v-else-if="t.id === 'profile'" class="tab-panel">
            <p v-if="u.bio" class="pub-bio user-head-bio">{{ u.bio }}</p>
            <n-space v-if="genderLabel || u.region" class="pub-meta muted" :size="8">
              <span v-if="genderLabel">{{ genderLabel }}</span>
              <span v-if="u.region">{{ u.region }}</span>
            </n-space>
            <n-empty v-if="!u.bio && !genderLabel && !u.region" description="暂无简介" />
          </div>

          <div v-else-if="t.id === 'landscape'" class="tab-panel">
            <UserLandscapePanel v-if="u?.id" :user-id="u.id" />
          </div>

          <div v-else-if="t.id === 'following'" class="tab-panel">
            <n-skeleton v-if="listLoading" height="128px" :sharp="false" />
            <n-empty v-else-if="!followingList.length" description="暂无关注" />
            <n-list v-else bordered>
              <UserListItem
                v-for="item in followingList"
                :key="item.id"
                :user="item"
                @follow-changed="loadFollowing"
              />
            </n-list>
          </div>

          <div v-else-if="t.id === 'followers'" class="tab-panel">
            <n-skeleton v-if="listLoading" height="128px" :sharp="false" />
            <n-empty v-else-if="!followersList.length" description="暂无粉丝" />
            <n-list v-else bordered>
              <UserListItem
                v-for="item in followersList"
                :key="item.id"
                :user="item"
                @follow-changed="loadFollowers"
              />
            </n-list>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  NCard,
  NEmpty,
  NList,
  NSkeleton,
  NSpace,
  NTabPane,
  NTabs,
} from 'naive-ui';
import { fetchPublicUser } from '../api/user';
import { getFollowStatus, fetchFollowers, fetchFollowing } from '../api/interaction';
import { getSocialCard, getTimeline, recordVisit } from '../api/social';
import { useAuthStore } from '../stores/auth';
import FollowButton from '../components/FollowButton.vue';
import ProfileHeader from '../components/profile/ProfileHeader.vue';
import InteractionTimeline from '../components/profile/InteractionTimeline.vue';
import UserProfileSkeleton from '../components/skeleton/UserProfileSkeleton.vue';
import UserListItem from '../components/UserListItem.vue';
import UserLandscapePanel from '../components/knowledge/UserLandscapePanel.vue';

const route = useRoute();
const authStore = useAuthStore();
const loading = ref(true);
const u = ref(null);
const following = ref(false);
const tab = ref('activity');
const listLoading = ref(false);
const followingList = ref([]);
const followersList = ref([]);
const socialCard = ref(null);
const timelineItems = ref([]);

const tabs = [
  { id: 'activity', label: '动态' },
  { id: 'profile', label: '资料' },
  { id: 'landscape', label: '知识版图' },
  { id: 'following', label: '关注' },
  { id: 'followers', label: '粉丝' },
];

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

async function loadTimeline(id) {
  try {
    const res = await getTimeline(id);
    timelineItems.value = res.data || [];
  } catch {
    timelineItems.value = socialCard.value?.timelinePreview || [];
  }
}

async function load(id) {
  loading.value = true;
  tab.value = 'activity';
  try {
    const res = await fetchPublicUser(id);
    u.value = res.data;
    try {
      const sc = await getSocialCard(id);
      socialCard.value = sc.data;
      timelineItems.value = sc.data?.timelinePreview || [];
    } catch {
      socialCard.value = null;
    }
    if (authStore.token && authStore.user?.id && authStore.user.id !== id) {
      recordVisit(id).catch(() => {});
    }
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
  if (t === 'activity' && u.value?.id) loadTimeline(u.value.id);
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
  padding-bottom: var(--space-16);
}

.pub-panel {
  max-width: 40rem;
  margin: 0 auto;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  background: var(--color-surface) !important;
  border: 1px solid var(--color-border) !important;
}

.pub-panel :deep(.n-tabs-tab--active) {
  color: var(--color-primary) !important;
}

.pub-panel :deep(.n-tabs-bar) {
  background: var(--color-text) !important;
  height: 1px !important;
}

.user-head {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.user-head-main {
  flex: 1;
  min-width: 0;
}

.pub-name {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.pub-meta {
  font-size: var(--text-sm);
  margin-top: var(--space-1);
}

.pub-bio {
  font-size: var(--text-base);
  line-height: 1.6;
  color: var(--color-text);
  word-break: break-word;
}

.tab-panel {
  padding-top: var(--space-4);
}

.muted {
  color: var(--color-text-muted);
}

@media (max-width: 767px) {
  .public-user {
    padding: var(--space-4) var(--space-4)
      calc(var(--space-12) + var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
    box-sizing: border-box;
  }

  .public-user > .n-skeleton,
  .public-user > .n-empty {
    padding: var(--space-4);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background: var(--color-surface);
    box-shadow: var(--shadow-sm);
  }

  .pub-panel {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }

  .pub-panel :deep(.n-card-header) {
    padding: var(--space-4) !important;
  }

  .pub-panel :deep(.n-card__content) {
    padding: 0 var(--space-4) var(--space-4) !important;
  }

  .pub-panel :deep(.n-tabs-tab) {
    padding: var(--space-3) var(--space-2);
  }

  .tab-panel {
    padding-top: var(--space-3);
  }

  .pub-panel :deep(.n-list) {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow: hidden;
    background: var(--surface-muted);
  }

  .pub-panel :deep(.n-list .n-list-item) {
    padding: var(--space-3) var(--space-4);
  }

  .pub-panel :deep(.n-list .n-list-item .n-list-item__prefix) {
    margin-right: var(--space-3);
  }

  .pub-bio {
    padding: var(--space-3) var(--space-4);
    background: var(--surface-muted);
    border-radius: var(--radius-md);
    line-height: 1.55;
  }

  .user-head-name {
    font-size: var(--text-lg);
  }
}
</style>
