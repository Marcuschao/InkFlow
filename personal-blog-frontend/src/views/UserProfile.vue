<template>
  <div class="profile-page ds-page container">
    <div v-if="loading" class="profile-skel ui-skeleton" />
    <div v-else-if="!user" class="state-msg">无法加载资料</div>
    <div v-else class="profile-panel">
      <div class="profile-head">
        <h1 class="profile-title">{{ user.nickname || user.username }}</h1>
        <p class="counts muted">
          <span>{{ user.followerCount ?? 0 }} 粉丝</span>
          <span>{{ user.followingCount ?? 0 }} 关注</span>
        </p>
      </div>
      <nav class="profile-tabs" aria-label="个人主页">
        <button
          v-for="t in tabs"
          :key="t.id"
          type="button"
          class="tab-btn"
          :class="{ active: tab === t.id }"
          @click="setTab(t.id)"
        >{{ t.label }}</button>
      </nav>

      <div v-if="tab === 'profile'" class="tab-panel">
        <p class="extra muted">
          <span v-if="user.registerRegion">注册地区：{{ user.registerRegion }}</span>
          <span v-if="user.region" class="ml">展示地区：{{ user.region }}</span>
        </p>
        <form class="profile-form" @submit.prevent="save">
          <div class="fg">
            <label class="ds-form-label" for="nickname">昵称</label>
            <input id="nickname" v-model="nickname" class="ds-input" type="text" maxlength="50" />
          </div>
          <div class="fg">
            <label class="ds-form-label" for="avatar">头像 URL</label>
            <input id="avatar" v-model="avatar" class="ds-input" type="url" maxlength="255" />
          </div>
          <div class="fg">
            <label class="ds-form-label" for="gender">性别</label>
            <select id="gender" v-model.number="gender" class="ds-input fg-select">
              <option :value="0">未知</option>
              <option :value="1">男</option>
              <option :value="2">女</option>
            </select>
          </div>
          <div class="fg">
            <label class="ds-form-label" for="bio">简介</label>
            <textarea id="bio" v-model="bio" class="ds-textarea" maxlength="500" rows="4" />
          </div>
          <button type="submit" class="ds-btn ds-btn--primary" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
        </form>
      </div>

      <div v-else-if="tab === 'favorites'" class="tab-panel">
        <div v-if="favLoading" class="list-skel ui-skeleton" />
        <p v-else-if="!favorites.length" class="empty-hint">暂无收藏</p>
        <div v-else class="card-grid">
          <ArticleCard
            v-for="a in favorites"
            :key="a.id"
            :article="a"
            :like-count="a.likeCount"
            :liked="a.liked"
            show-like
          />
        </div>
        <Pagination
          v-if="favTotal > favSize"
          :total="favTotal"
          :page-size="favSize"
          :current-page="favPage"
          @changePage="loadFavorites"
        />
      </div>

      <div v-else-if="tab === 'following'" class="tab-panel">
        <div v-if="listLoading" class="list-skel ui-skeleton" />
        <p v-else-if="!followingList.length" class="empty-hint">暂无关注</p>
        <UserListItem
          v-for="u in followingList"
          :key="u.id"
          :user="u"
          @follow-changed="loadFollowing"
        />
      </div>

      <div v-else-if="tab === 'followers'" class="tab-panel">
        <div v-if="listLoading" class="list-skel ui-skeleton" />
        <p v-else-if="!followersList.length" class="empty-hint">暂无粉丝</p>
        <UserListItem
          v-for="u in followersList"
          :key="u.id"
          :user="u"
          @follow-changed="loadFollowers"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchMe, updateProfile } from '../api/user';
import { fetchMyFavorites, fetchFollowers, fetchFollowing } from '../api/interaction';
import { useAuthStore } from '../stores/auth';
import { useToastStore } from '../stores/toast';
import ArticleCard from '../components/ArticleCard.vue';
import UserListItem from '../components/UserListItem.vue';
import Pagination from '../components/Pagination.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toast = useToastStore();

const tabs = [
  { id: 'profile', label: '资料' },
  { id: 'favorites', label: '收藏' },
  { id: 'following', label: '关注' },
  { id: 'followers', label: '粉丝' },
];

const loading = ref(true);
const saving = ref(false);
const tab = ref('profile');
const user = ref(null);
const nickname = ref('');
const avatar = ref('');
const gender = ref(0);
const bio = ref('');

const favLoading = ref(false);
const favorites = ref([]);
const favPage = ref(1);
const favSize = ref(10);
const favTotal = ref(0);

const listLoading = ref(false);
const followingList = ref([]);
const followersList = ref([]);

function setTab(id) {
  tab.value = id;
  router.replace({ query: { ...route.query, tab: id === 'profile' ? undefined : id } });
  if (id === 'favorites') loadFavorites(1);
  if (id === 'following') loadFollowing();
  if (id === 'followers') loadFollowers();
}

async function loadFavorites(page = 1) {
  favLoading.value = true;
  favPage.value = page;
  try {
    const res = await fetchMyFavorites({ page, size: favSize.value });
    const d = res.data;
    favorites.value = d?.records || [];
    favTotal.value = d?.total || 0;
  } catch {
    favorites.value = [];
  } finally {
    favLoading.value = false;
  }
}

async function loadFollowing() {
  if (!user.value?.id) return;
  listLoading.value = true;
  try {
    const res = await fetchFollowing(user.value.id, { page: 1, size: 50 });
    followingList.value = res.data?.records || [];
  } catch {
    followingList.value = [];
  } finally {
    listLoading.value = false;
  }
}

async function loadFollowers() {
  if (!user.value?.id) return;
  listLoading.value = true;
  try {
    const res = await fetchFollowers(user.value.id, { page: 1, size: 50 });
    followersList.value = res.data?.records || [];
  } catch {
    followersList.value = [];
  } finally {
    listLoading.value = false;
  }
}

onMounted(async () => {
  const qTab = route.query.tab;
  if (qTab && tabs.some((t) => t.id === qTab)) tab.value = qTab;

  loading.value = true;
  try {
    const res = await fetchMe();
    const u = res.data;
    user.value = u;
    nickname.value = u?.nickname ?? u?.username ?? '';
    avatar.value = u?.avatar ?? '';
    gender.value = u?.gender ?? 0;
    bio.value = u?.bio ?? '';
    authStore.user = u;
  } catch {
    user.value = null;
  } finally {
    loading.value = false;
  }

  if (tab.value === 'favorites') loadFavorites(1);
  if (tab.value === 'following') loadFollowing();
  if (tab.value === 'followers') loadFollowers();
});

watch(
  () => route.query.tab,
  (q) => {
    const id = q && tabs.some((t) => t.id === q) ? q : 'profile';
    if (id !== tab.value) setTab(id);
  }
);

async function save() {
  saving.value = true;
  try {
    const res = await updateProfile({
      nickname: nickname.value.trim(),
      avatar: avatar.value.trim() || null,
      gender: gender.value,
      bio: bio.value.trim() || null,
    });
    const next = res.data;
    user.value = next;
    nickname.value = next?.nickname ?? next?.username ?? '';
    avatar.value = next?.avatar ?? '';
    gender.value = next?.gender ?? 0;
    bio.value = next?.bio ?? '';
    authStore.user = next;
    toast.push('已保存', 'success');
  } catch {
    /* request toast */
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.profile-page {
  padding-top: var(--space-8);
  padding-bottom: var(--space-16);
}

.profile-skel,
.list-skel {
  height: 8rem;
  border-radius: var(--radius-lg);
}

.profile-panel {
  max-width: 40rem;
  margin: 0 auto;
  background: var(--color-surface);
  padding: clamp(1.25rem, 3vw, 1.85rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.profile-title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.counts span + span {
  margin-left: var(--space-4);
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

.muted {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.extra {
  margin: 0 0 var(--space-6);
}

.ml {
  margin-left: var(--space-4);
}

.fg {
  margin-bottom: var(--space-5);
}

.fg-select {
  width: 100%;
}

.empty-hint {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  text-align: center;
  padding: var(--space-8) 0;
}

.card-grid {
  display: grid;
  gap: var(--space-4);
}
</style>
