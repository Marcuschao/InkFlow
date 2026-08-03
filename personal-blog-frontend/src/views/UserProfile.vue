<template>
  <div class="profile-page ds-page container">
    <UserProfileSkeleton v-if="loading" />
    <n-empty v-else-if="!user" description="无法加载资料" />
    <div v-else class="profile-shell">
      <ProfileHeader
        :user="user"
        :badges="socialCard?.badges || []"
        :points="socialCard?.points"
        :equipped-items="socialCard?.equippedItems || []"
      >
        <template #action>
          <n-button class="profile-edit-button" secondary @click="setTab('profile')">
            <template #icon><PencilLine :size="16" /></template>
            编辑资料
          </n-button>
        </template>
      </ProfileHeader>

      <div class="profile-content-grid">
        <section class="profile-main" aria-label="个人主页内容">
          <n-tabs class="profile-tabs" type="line" :value="tab" @update:value="setTab">
        <n-tab-pane v-for="t in tabs" :key="t.id" :name="t.id" :tab="t.label">
          <div v-if="t.id === 'activity'" class="tab-panel">
            <InteractionTimeline :items="timelineItems" />
          </div>

          <div v-else-if="t.id === 'visitors'" class="tab-panel">
            <VisitorList :visitors="visitors" />
          </div>

          <div v-else-if="t.id === 'inventory'" class="tab-panel">
            <UserInventoryPanel @changed="loadSocialCard" />
          </div>

          <div v-else-if="t.id === 'profile'" class="tab-panel profile-settings">
            <div class="section-heading">
              <p class="section-kicker">账户设置</p>
              <h2>编辑个人资料</h2>
              <p>这些信息会展示在你的公开创作者档案中。</p>
            </div>
            <n-form class="profile-form" @submit.prevent="save">
              <div class="profile-form-grid">
                <n-form-item label="昵称">
                  <n-input v-model:value="nickname" maxlength="50" />
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
              </div>
              <n-form-item label="头像">
                <div class="avatar-field">
                  <n-upload
                    :show-file-list="false"
                    accept="image/*"
                    :disabled="avatarUploading"
                    :custom-request="onAvatarUpload"
                  >
                    <n-button :loading="avatarUploading">上传头像</n-button>
                  </n-upload>
                  <n-input v-model:value="avatar" maxlength="512" placeholder="或粘贴图片地址" />
                </div>
              </n-form-item>
              <n-form-item class="bio-field" label="个人说明">
                <n-input v-model:value="bio" type="textarea" maxlength="500" :rows="4" />
              </n-form-item>
              <div class="profile-form-footer">
                <p v-if="user.registerRegion || user.region" class="profile-region muted">
                  <span v-if="user.registerRegion">注册于 {{ user.registerRegion }}</span>
                  <span v-if="user.region">当前展示 {{ user.region }}</span>
                </p>
                <n-button type="primary" attr-type="submit" :loading="saving">
                  {{ saving ? '保存中…' : '保存更改' }}
                </n-button>
              </div>
            </n-form>
            <div class="oauth-section">
              <h2 class="oauth-heading">第三方账号</h2>
              <p v-if="githubBinding" class="muted">
                已绑定 GitHub：{{ githubBinding.providerUsername || '—' }}
              </p>
              <div class="oauth-providers">
                <button
                  v-if="!githubBinding"
                  type="button"
                  class="oauth-provider-btn"
                  aria-label="绑定 GitHub"
                  :disabled="oauthLoading"
                  @click="startBindGithub"
                >
                  <svg class="oauth-provider-icon" aria-hidden="true">
                    <use :href="`${iconsSprite}#github-icon`" />
                  </svg>
                </button>
                <n-button v-else :loading="oauthLoading" size="small" @click="doUnbindGithub">解绑 GitHub</n-button>
              </div>
            </div>
          </div>

          <div v-else-if="t.id === 'landscape'" class="tab-panel">
            <UserLandscapePanel v-if="user?.id" :user-id="user.id" />
          </div>

          <div v-else-if="t.id === 'favorites'" class="tab-panel">
            <n-skeleton v-if="favLoading" height="128px" :sharp="false" />
            <n-empty v-else-if="!favorites.length" description="暂无收藏" />
            <n-grid v-else :cols="1" :y-gap="16">
              <n-gi v-for="a in favorites" :key="a.id">
                <ArticleCard
                  :article="a"
                  :like-count="a.likeCount"
                  :liked="a.liked"
                  :equipped-items="socialCard?.equippedItems || []"
                  show-like
                />
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
        </section>

        <aside class="profile-aside" aria-label="个人概况">
          <SignWidget />
          <section class="profile-facts">
            <p class="profile-facts-kicker">账户概况</p>
            <dl>
              <div v-if="user.email">
                <dt>邮箱</dt>
                <dd>{{ user.email }}</dd>
              </div>
              <div v-if="user.region">
                <dt>地区</dt>
                <dd>{{ user.region }}</dd>
              </div>
              <div>
                <dt>粉丝</dt>
                <dd>{{ user.followerCount ?? 0 }}</dd>
              </div>
              <div>
                <dt>关注</dt>
                <dd>{{ user.followingCount ?? 0 }}</dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  NButton,
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
import { PencilLine } from 'lucide-vue-next';
import { fetchMe, fetchPublicUser, updateProfile, uploadAvatar } from '../api/user';
import { fetchMyFavorites, fetchFollowers, fetchFollowing } from '../api/interaction';
import { getSocialCard, getTimeline, getVisitors } from '../api/social';
import { useAuthStore } from '../stores/auth';
import { useChatUserProfiles } from '../composables/useChatUserProfiles';
import { useToastStore } from '../stores/toast';
import ArticleCard from '../components/ArticleCard.vue';
import UserLandscapePanel from '../components/knowledge/UserLandscapePanel.vue';
import UserProfileSkeleton from '../components/skeleton/UserProfileSkeleton.vue';
import UserListItem from '../components/UserListItem.vue';
import ProfileHeader from '../components/profile/ProfileHeader.vue';
import SignWidget from '../components/profile/SignWidget.vue';
import InteractionTimeline from '../components/profile/InteractionTimeline.vue';
import VisitorList from '../components/profile/VisitorList.vue';
import UserInventoryPanel from '../components/profile/UserInventoryPanel.vue';
import Pagination from '../components/Pagination.vue';
import { fetchOAuthBindings, bindGithub, unbindGithub } from '../api/oauth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { setProfile } = useChatUserProfiles();
const toast = useToastStore();
const iconsSprite = `${import.meta.env.BASE_URL}icons.svg`;

const tabs = [
  { id: 'activity', label: '动态' },
  { id: 'profile', label: '资料' },
  { id: 'landscape', label: '知识版图' },
  { id: 'favorites', label: '收藏' },
  { id: 'inventory', label: '背包' },
  { id: 'visitors', label: '访客' },
  { id: 'following', label: '关注' },
  { id: 'followers', label: '粉丝' },
];

const loading = ref(true);
const saving = ref(false);
const avatarUploading = ref(false);
const tab = ref('activity');
const user = ref(null);
const socialCard = ref(null);
const timelineItems = ref([]);
const visitors = ref([]);
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
  router.replace({ query: { ...route.query, tab: id } });
  if (id === 'favorites') loadFavorites(1);
  if (id === 'following') loadFollowing();
  if (id === 'followers') loadFollowers();
  if (id === 'activity') loadTimeline();
  if (id === 'visitors') loadVisitorList();
}

async function loadSocialCard() {
  if (!user.value?.id) return;
  try {
    const res = await getSocialCard(user.value.id);
    socialCard.value = res.data;
    if (res.data?.recentVisitors) visitors.value = res.data.recentVisitors;
  } catch {
    socialCard.value = null;
  }
}

async function loadTimeline() {
  if (!user.value?.id) return;
  try {
    const res = await getTimeline(user.value.id);
    timelineItems.value = res.data || [];
  } catch {
    timelineItems.value = [];
  }
}

async function loadVisitorList() {
  try {
    const res = await getVisitors();
    visitors.value = res.data || [];
  } catch {
    visitors.value = socialCard.value?.recentVisitors || [];
  }
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
    if (u?.id) {
      try {
        const pub = await fetchPublicUser(u.id);
        if (pub.data) {
          u.followerCount = pub.data.followerCount;
          u.followingCount = pub.data.followingCount;
        }
      } catch {
        /* keep auth counts */
      }
    }
    user.value = u;
    nickname.value = u?.nickname ?? u?.username ?? '';
    avatar.value = u?.avatar ?? '';
    gender.value = u?.gender ?? 0;
    bio.value = u?.bio ?? '';
    authStore.user = u;
    await loadSocialCard();
  } catch {
    user.value = null;
  } finally {
    loading.value = false;
  }

  await loadOAuthBindings();

  if (tab.value === 'favorites') loadFavorites(1);
  if (tab.value === 'following') loadFollowing();
  if (tab.value === 'followers') loadFollowers();
  if (tab.value === 'activity') loadTimeline();
  if (tab.value === 'visitors') loadVisitorList();
});

watch(
  () => route.query.tab,
  (q) => {
    const id = q && tabs.some((t) => t.id === q) ? q : 'activity';
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
  max-width: 74rem;
  padding-right: var(--container-pad-x);
  padding-left: var(--container-pad-x);
}

.profile-shell {
  width: 100%;
  margin: 0 auto;
}

.profile-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 16.25rem;
  gap: var(--space-8);
  align-items: start;
  margin-top: var(--space-8);
  animation: content-enter 0.52s 0.08s var(--ease-out-soft) both;
}

.profile-main {
  min-width: 0;
}

.profile-aside {
  position: sticky;
  top: calc(var(--layout-navbar-bottom) + var(--space-6));
  display: grid;
  gap: var(--space-4);
}

.profile-edit-button {
  white-space: nowrap;
}

.profile-tabs :deep(.n-tabs-nav) {
  border-bottom: 1px solid var(--color-border-strong);
}

.profile-tabs :deep(.n-tabs-nav-scroll-content) {
  min-width: max-content;
}

.profile-tabs :deep(.n-tabs-tab) {
  padding: 0 var(--space-3) var(--space-3);
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  white-space: nowrap;
}

.profile-tabs :deep(.n-tabs-tab:first-child) {
  padding-left: 0;
}

.profile-tabs :deep(.n-tabs-tab--active) {
  color: var(--color-primary) !important;
}

.profile-tabs :deep(.n-tabs-bar) {
  height: 2px !important;
  background: var(--color-primary) !important;
}

.profile-tabs :deep(.n-tab-pane) {
  padding-top: var(--space-6);
}

.tab-panel {
  min-height: 20rem;
}

.profile-facts {
  padding: var(--space-5);
  border-top: 2px solid var(--color-text);
  border-bottom: 1px solid var(--color-border-strong);
}

.profile-facts-kicker,
.section-kicker {
  margin: 0;
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.profile-facts dl {
  margin: var(--space-4) 0 0;
}

.profile-facts dl > div {
  display: grid;
  grid-template-columns: 3.5rem minmax(0, 1fr);
  gap: var(--space-3);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
  font-size: var(--text-sm);
}

.profile-facts dl > div:last-child {
  border-bottom: 0;
}

.profile-facts dt {
  color: var(--color-text-soft);
}

.profile-facts dd {
  min-width: 0;
  margin: 0;
  color: var(--color-text);
  overflow-wrap: anywhere;
  text-align: right;
}

.section-heading {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.section-heading h2 {
  margin: var(--space-1) 0 0;
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: var(--weight-semibold);
}

.section-heading > p:last-child {
  margin: var(--space-2) 0 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--space-5);
}

.avatar-field {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--space-3);
  width: 100%;
}

.profile-form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-top: var(--space-2);
}

.profile-region {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin: 0;
}

.muted {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.oauth-section {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border-strong);
}

.oauth-heading {
  margin: 0 0 var(--space-3);
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
}

.oauth-providers {
  display: flex;
  align-items: center;
  gap: var(--space-3);
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
  .profile-content-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-aside {
    position: static;
    grid-row: 1;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .profile-page {
    padding: var(--space-4) var(--space-4)
      calc(var(--space-12) + var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
  }

  .profile-content-grid {
    gap: var(--space-6);
    margin-top: var(--space-6);
  }

  .profile-aside {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-facts {
    display: none;
  }

  .profile-edit-button {
    width: 100%;
  }

  .profile-tabs :deep(.n-tabs-nav-scroll-wrapper) {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .profile-tabs :deep(.n-tabs-nav-scroll-wrapper::-webkit-scrollbar) {
    display: none;
  }

  .profile-tabs :deep(.n-tabs-tab) {
    padding-right: var(--space-3);
    padding-left: var(--space-3);
  }

  .profile-form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .avatar-field {
    grid-template-columns: minmax(0, 1fr);
  }

  .profile-form-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .profile-form-footer :deep(.n-button) {
    width: 100%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .profile-content-grid {
    animation: none;
  }
}
</style>
