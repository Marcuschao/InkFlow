<template>
  <div class="profile-page ds-page container">
    <n-skeleton v-if="loading" height="128px" :sharp="false" />
    <n-empty v-else-if="!user" description="无法加载资料" />
    <n-card v-else class="profile-panel">
      <template #header>
        <div class="user-head">
          <UserAvatar class="user-head-avatar" :src="avatar" :name="user.nickname || user.username" :size="64" />
          <div class="user-head-main">
            <h1 class="profile-title user-head-name">{{ user.nickname || user.username }}</h1>
            <n-space class="counts muted user-head-counts" :size="12">
              <span>{{ user.followerCount ?? 0 }} 粉丝</span>
              <span>{{ user.followingCount ?? 0 }} 关注</span>
            </n-space>
          </div>
        </div>
      </template>

      <n-tabs type="line" :value="tab" @update:value="setTab">
        <n-tab-pane v-for="t in tabs" :key="t.id" :name="t.id" :tab="t.label">
          <div v-if="t.id === 'profile'" class="tab-panel">
            <p class="extra muted">
              <span v-if="user.registerRegion">注册地区：{{ user.registerRegion }}</span>
              <span v-if="user.region" class="ml">展示地区：{{ user.region }}</span>
            </p>
            <n-form class="profile-form" @submit.prevent="save">
              <n-form-item label="昵称">
                <n-input v-model:value="nickname" maxlength="50" />
              </n-form-item>
              <n-form-item label="头像">
                <n-space align="center">
                  <n-upload
                    :show-file-list="false"
                    accept="image/*"
                    :disabled="avatarUploading"
                    :custom-request="onAvatarUpload"
                  >
                    <n-button size="small" :loading="avatarUploading">上传头像</n-button>
                  </n-upload>
                  <n-input v-model:value="avatar" maxlength="512" placeholder="或粘贴图片地址" style="flex: 1" />
                </n-space>
              </n-form-item>
              <n-form-item label="性别">
                <n-radio-group v-model:value="gender">
                  <n-space>
                    <n-radio :value="0">未知</n-radio>
                    <n-radio :value="1">男</n-radio>
                    <n-radio :value="2">女</n-radio>
                  </n-space>
                </n-radio-group>
              </n-form-item>
              <n-form-item label="简介">
                <n-input v-model:value="bio" type="textarea" maxlength="500" :rows="4" />
              </n-form-item>
              <n-button type="primary" attr-type="submit" :loading="saving">{{ saving ? '保存中…' : '保存' }}</n-button>
            </n-form>
            <div class="oauth-section">
              <h2 class="oauth-heading">第三方账号</h2>
              <p v-if="githubBinding" class="muted">
                已绑定 GitHub：{{ githubBinding.providerUsername || '—' }}
              </p>
              <p v-else class="muted">未绑定 GitHub</p>
              <n-space>
                <n-button v-if="!githubBinding" :loading="oauthLoading" @click="startBindGithub">绑定 GitHub</n-button>
                <n-button v-else :loading="oauthLoading" @click="doUnbindGithub">解绑 GitHub</n-button>
              </n-space>
            </div>
          </div>

          <div v-else-if="t.id === 'favorites'" class="tab-panel">
            <n-skeleton v-if="favLoading" height="128px" :sharp="false" />
            <n-empty v-else-if="!favorites.length" description="暂无收藏" />
            <n-grid v-else :cols="1" :y-gap="16">
              <n-gi v-for="a in favorites" :key="a.id">
                <ArticleCard :article="a" :like-count="a.likeCount" :liked="a.liked" show-like />
              </n-gi>
            </n-grid>
            <Pagination v-if="favTotal > favSize" :total="favTotal" :page-size="favSize" :current-page="favPage" @changePage="loadFavorites" />
          </div>

          <div v-else-if="t.id === 'following'" class="tab-panel">
            <n-skeleton v-if="listLoading" height="128px" :sharp="false" />
            <n-empty v-else-if="!followingList.length" description="暂无关注" />
            <n-list v-else bordered>
              <UserListItem v-for="u in followingList" :key="u.id" :user="u" @follow-changed="loadFollowing" />
            </n-list>
          </div>

          <div v-else-if="t.id === 'followers'" class="tab-panel">
            <n-skeleton v-if="listLoading" height="128px" :sharp="false" />
            <n-empty v-else-if="!followersList.length" description="暂无粉丝" />
            <n-list v-else bordered>
              <UserListItem v-for="u in followersList" :key="u.id" :user="u" @follow-changed="loadFollowers" />
            </n-list>
          </div>
        </n-tab-pane>
      </n-tabs>
    </n-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton,
  NCard,
  NEmpty,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NList,
  NRadio,
  NRadioGroup,
  NSkeleton,
  NSpace,
  NTabPane,
  NTabs,
  NUpload,
} from 'naive-ui';
import { fetchMe, updateProfile, uploadAvatar } from '../api/user';
import { fetchMyFavorites, fetchFollowers, fetchFollowing } from '../api/interaction';
import { useAuthStore } from '../stores/auth';
import { useChatUserProfiles } from '../composables/useChatUserProfiles';
import { useToastStore } from '../stores/toast';
import ArticleCard from '../components/ArticleCard.vue';
import UserAvatar from '../components/UserAvatar.vue';
import UserListItem from '../components/UserListItem.vue';
import Pagination from '../components/Pagination.vue';
import { fetchOAuthBindings, bindGithub, unbindGithub } from '../api/oauth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { setProfile } = useChatUserProfiles();
const toast = useToastStore();

const tabs = [
  { id: 'profile', label: '资料' },
  { id: 'favorites', label: '收藏' },
  { id: 'following', label: '关注' },
  { id: 'followers', label: '粉丝' },
];

const loading = ref(true);
const saving = ref(false);
const avatarUploading = ref(false);
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
const oauthBindings = ref([]);
const oauthLoading = ref(false);

const githubBinding = computed(() =>
  oauthBindings.value.find((b) => b.provider === 'github')
);

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

  await loadOAuthBindings();

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

async function onAvatarUpload({ file, onFinish, onError }) {
  avatarUploading.value = true;
  try {
    const res = await uploadAvatar(file.file);
    const next = res.data;
    user.value = next;
    avatar.value = next?.avatar ?? '';
    authStore.user = next;
    setProfile(next.id, { username: next.nickname || next.username, avatar: next.avatar });
    toast.push('头像已更新', 'success');
    onFinish();
  } catch (e) {
    onError();
  } finally {
    avatarUploading.value = false;
  }
}

async function loadOAuthBindings() {
  try {
    const res = await fetchOAuthBindings();
    oauthBindings.value = res.data || [];
  } catch {
    oauthBindings.value = [];
  }
}

async function startBindGithub() {
  oauthLoading.value = true;
  try {
    const res = await bindGithub();
    const url = res.data?.authorizeUrl || '/oauth2/authorization/github';
    window.location.href = url;
  } catch {
    /* request toast */
  } finally {
    oauthLoading.value = false;
  }
}

async function doUnbindGithub() {
  oauthLoading.value = true;
  try {
    await unbindGithub();
    oauthBindings.value = [];
    toast.push('已解绑 GitHub', 'success');
  } catch {
    /* request toast */
  } finally {
    oauthLoading.value = false;
  }
}

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
    setProfile(next.id, { username: next.nickname || next.username, avatar: next.avatar });
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
  padding-bottom: var(--space-16);
}

.user-head {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.user-head-main {
  flex: 1;
  min-width: 0;
}

.profile-skel,
.list-skel {
  height: 8rem;
  border-radius: var(--radius-lg);
}

.profile-panel {
  max-width: 40rem;
  margin: 0 auto;
  border-radius: var(--radius-lg);
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

@media (max-width: 767px) {
  .profile-page {
    padding: var(--space-4) var(--space-4)
      calc(var(--space-12) + var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
    box-sizing: border-box;
  }

  .profile-page > .n-skeleton,
  .profile-page > .n-empty {
    padding: var(--space-4);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    background: var(--color-surface);
    box-shadow: var(--shadow-sm);
  }

  .profile-panel :deep(.n-card-header) {
    padding: var(--space-4) !important;
  }

  .profile-panel :deep(.n-card__content) {
    padding: 0 var(--space-4) var(--space-4) !important;
  }

  .profile-panel :deep(.n-tabs-tab) {
    padding: var(--space-3) var(--space-2);
  }

  .tab-panel {
    padding-top: var(--space-3);
  }

  .profile-panel :deep(.n-list) {
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    overflow: hidden;
    background: var(--surface-muted);
  }

  .profile-panel :deep(.n-list .n-list-item) {
    padding: var(--space-3) var(--space-4);
  }

  .profile-panel :deep(.n-list .n-list-item .n-list-item__prefix) {
    margin-right: var(--space-3);
  }

  .profile-title,
  .user-head-name {
    font-size: var(--text-lg);
  }

  .profile-form :deep(.n-form-item) {
    margin-bottom: var(--space-4);
  }
}

.oauth-section {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border);
}

.oauth-heading {
  margin: 0 0 var(--space-3);
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
}
</style>
