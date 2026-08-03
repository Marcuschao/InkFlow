<template>
  <div class="public-user ds-page container">
    <UserProfileSkeleton v-if="loading" />
    <n-empty v-else-if="!u" description="用户不存在" />
    <div v-else class="public-shell">
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

      <div class="public-content-grid" :class="{ 'is-wide': !showPublicAside }">
        <section class="public-main" aria-label="创作者主页内容">
          <n-tabs class="public-tabs" type="line" :value="tab" @update:value="tab = $event">
            <n-tab-pane v-for="t in tabs" :key="t.id" :name="t.id" :tab="t.label">
              <div v-if="t.id === 'activity'" class="tab-panel">
                <InteractionTimeline :items="timelineItems" />
              </div>

              <div v-else-if="t.id === 'profile'" class="tab-panel public-about">
                <div v-if="u.bio || genderLabel || u.region">
                  <p class="section-kicker">关于作者</p>
                  <h2>{{ u.nickname || 'InkFlow 创作者' }}</h2>
                  <p v-if="u.bio" class="pub-bio">{{ u.bio }}</p>
                  <dl v-if="genderLabel || u.region" class="pub-meta">
                    <div v-if="genderLabel"><dt>性别</dt><dd>{{ genderLabel }}</dd></div>
                    <div v-if="u.region"><dt>地区</dt><dd>{{ u.region }}</dd></div>
                  </dl>
                </div>
                <n-empty v-else description="作者还没有补充个人资料" />
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
        </section>

        <aside v-if="showPublicAside" class="public-aside" aria-label="作者概况">
          <p class="public-aside-kicker">作者手记</p>
          <p v-if="u.bio" class="public-aside-bio">{{ u.bio }}</p>
          <dl>
            <div v-if="u.region"><dt>常驻</dt><dd>{{ u.region }}</dd></div>
            <div v-if="genderLabel"><dt>性别</dt><dd>{{ genderLabel }}</dd></div>
            <div><dt>粉丝</dt><dd>{{ u.followerCount ?? 0 }}</dd></div>
            <div><dt>关注</dt><dd>{{ u.followingCount ?? 0 }}</dd></div>
          </dl>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useHead } from '@vueuse/head';
import {
  NEmpty,
  NList,
  NSkeleton,
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

const showPublicAside = computed(() => !!(u.value?.bio || genderLabel.value || u.value?.region));

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
  max-width: 74rem;
  padding-right: var(--container-pad-x);
  padding-left: var(--container-pad-x);
}

.public-shell {
  width: 100%;
  margin: 0 auto;
}

.public-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16.25rem;
  gap: var(--space-8);
  align-items: start;
  margin-top: var(--space-8);
  animation: content-enter 0.52s 0.08s var(--ease-out-soft) both;
}

.public-content-grid.is-wide {
  grid-template-columns: minmax(0, 1fr);
}

.public-main {
  min-width: 0;
}

.public-tabs :deep(.n-tabs-nav) {
  border-bottom: 1px solid var(--color-border-strong);
}

.public-tabs :deep(.n-tabs-nav-scroll-content) {
  min-width: max-content;
}

.public-tabs :deep(.n-tabs-tab) {
  padding: 0 var(--space-4) var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  white-space: nowrap;
}

.public-tabs :deep(.n-tabs-tab:first-child) {
  padding-left: 0;
}

.public-tabs :deep(.n-tabs-tab--active) {
  color: var(--color-primary) !important;
}

.public-tabs :deep(.n-tabs-bar) {
  height: 2px !important;
  background: var(--color-primary) !important;
}

.public-tabs :deep(.n-tab-pane) {
  padding-top: var(--space-6);
}

.tab-panel {
  min-height: 20rem;
}

.public-aside {
  position: sticky;
  top: calc(var(--layout-navbar-bottom) + var(--space-6));
  padding: var(--space-5);
  border-top: 2px solid var(--color-text);
  border-bottom: 1px solid var(--color-border-strong);
}

.public-aside-kicker,
.section-kicker {
  margin: 0;
  color: var(--color-text-soft);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.public-aside-bio {
  display: -webkit-box;
  margin: var(--space-4) 0 0;
  overflow: hidden;
  color: var(--color-text-muted);
  font-family: var(--font-prose);
  font-size: var(--text-sm);
  line-height: 1.65;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5;
}

.public-aside dl {
  margin: var(--space-4) 0 0;
}

.public-aside dl > div,
.pub-meta > div {
  display: grid;
  grid-template-columns: 3.5rem minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
  font-size: var(--text-sm);
}

.public-aside dt,
.pub-meta dt {
  color: var(--color-text-soft);
}

.public-aside dd,
.pub-meta dd {
  margin: 0;
  color: var(--color-text);
  overflow-wrap: anywhere;
  text-align: right;
}

.public-about h2 {
  margin: var(--space-1) 0 var(--space-4);
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.pub-bio {
  max-width: 48rem;
  margin: 0;
  color: var(--color-text);
  font-family: var(--font-prose);
  font-size: var(--text-lg);
  line-height: 1.8;
  overflow-wrap: anywhere;
}

.pub-meta {
  max-width: 26rem;
  margin: var(--space-8) 0 0;
}

@keyframes content-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1023px) {
  .public-content-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .public-aside {
    position: static;
    grid-row: 1;
  }
}

@media (max-width: 767px) {
  .public-user {
    padding: var(--space-4) var(--space-4)
      calc(var(--space-12) + var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
  }

  .public-content-grid {
    gap: var(--space-6);
    margin-top: var(--space-6);
  }

  .public-aside {
    display: none;
  }

  .public-tabs :deep(.n-tabs-nav-scroll-wrapper) {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .public-tabs :deep(.n-tabs-nav-scroll-wrapper::-webkit-scrollbar) {
    display: none;
  }

  .public-tabs :deep(.n-tabs-tab) {
    padding-right: var(--space-3);
    padding-left: var(--space-3);
  }

  .pub-bio {
    font-size: var(--text-base);
  }
}

@media (prefers-reduced-motion: reduce) {
  .public-content-grid {
    animation: none;
  }
}
</style>
