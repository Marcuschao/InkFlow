<script setup>
import { ref, onMounted, onUnmounted, onActivated, nextTick } from 'vue';
import {
  NAlert,
  NButton,
  NDrawer,
  NDrawerContent,
  NEmpty,
  NInput,
  NList,
  NListItem,
  NTag,
  useMessage,
} from 'naive-ui';
import UserAvatar from '../components/UserAvatar.vue';
import { fetchChatHistory, fetchOnlineUsers, sendChatMessage, pingPresence } from '../api/chat';
import { useAuthStore } from '../stores/auth';
import { connect, onChatMessage, onStatusChange } from '../services/websocket';

const authStore = useAuthStore();
const message = useMessage();
const messages = ref([]);
const onlineUsers = ref([]);
const draft = ref('');
const loading = ref(true);
const sending = ref(false);
const showOnlineDrawer = ref(false);
const isMobile = ref(false);
const messageListRef = ref(null);
let onlineTimer = null;
let stopChatListener = () => {};
let stopStatusListener = () => {};

function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function isMine(msg) {
  return authStore.user?.id != null && msg.userId === authStore.user.id;
}

function appendMessage(msg) {
  if (!msg?.id) return;
  if (messages.value.some((m) => m.id === msg.id)) return;
  messages.value.push(msg);
  scrollToBottom();
}

async function scrollToBottom() {
  await nextTick();
  const el = messageListRef.value;
  if (el) el.scrollTop = el.scrollHeight;
}

async function loadHistory() {
  loading.value = true;
  try {
    const res = await fetchChatHistory();
    messages.value = res.data || [];
    await scrollToBottom();
  } catch {
    messages.value = [];
  } finally {
    loading.value = false;
  }
}

async function loadOnlineUsers() {
  try {
    const res = await fetchOnlineUsers();
    onlineUsers.value = res.data || [];
  } catch {
    onlineUsers.value = [];
  }
}

async function ensureConnected() {
  await connect(authStore.token || null);
}

async function onSend() {
  const content = draft.value.trim();
  if (!content || sending.value) return;
  sending.value = true;
  try {
    const res = await sendChatMessage(content);
    draft.value = '';
    if (res.data) appendMessage(res.data);
  } catch (err) {
    message.error(err?.message || '发送失败');
  } finally {
    sending.value = false;
  }
}

function updateMobile() {
  isMobile.value = window.innerWidth < 1024;
}

function onVisibilityChange() {
  if (document.visibilityState === 'visible') {
    loadOnlineUsers();
    ensureConnected();
    if (authStore.isLoggedIn) pingPresence().catch(() => {});
  }
}

function setupListeners() {
  stopChatListener = onChatMessage((msg) => appendMessage(msg));
  stopStatusListener = onStatusChange((connected) => {
    if (connected) loadOnlineUsers();
  });
}

async function initChat() {
  await ensureConnected();
  if (authStore.isLoggedIn) pingPresence().catch(() => {});
  await loadHistory();
  await loadOnlineUsers();
}

onMounted(async () => {
  updateMobile();
  window.addEventListener('resize', updateMobile);
  document.addEventListener('visibilitychange', onVisibilityChange);
  setupListeners();
  await initChat();
  onlineTimer = setInterval(loadOnlineUsers, 15000);
});

onActivated(async () => {
  await ensureConnected();
  if (authStore.isLoggedIn) pingPresence().catch(() => {});
  await loadOnlineUsers();
});

onUnmounted(() => {
  window.removeEventListener('resize', updateMobile);
  document.removeEventListener('visibilitychange', onVisibilityChange);
  if (onlineTimer) clearInterval(onlineTimer);
  stopChatListener();
  stopStatusListener();
});
</script>

<template>
  <div class="chat-page ds-page container">
    <header class="page-head">
      <h1 class="ds-page-title">聊天室</h1>
      <n-button
        v-if="isMobile"
        secondary
        size="small"
        @click="showOnlineDrawer = true"
      >
        在线 {{ onlineUsers.length }}
      </n-button>
    </header>

    <n-alert v-if="!authStore.isLoggedIn" type="info" :bordered="false" class="guest-tip">
      登录后可发言
    </n-alert>

    <div class="chat-layout">
      <section class="chat-main">
        <div ref="messageListRef" class="message-list">
          <n-empty v-if="!messages.length && !loading" description="暂无消息，来聊两句吧" />
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-row"
            :class="{ mine: isMine(msg) }"
          >
            <UserAvatar
              v-if="!isMine(msg)"
              class="msg-avatar"
              :src="msg.avatar"
              :name="msg.username"
              :size="36"
            />
            <div class="bubble-wrap">
              <div class="bubble-meta">
                <span class="name">{{ msg.username }}</span>
                <n-tag v-if="msg.admin" size="small" type="warning" :bordered="false">管理员</n-tag>
                <time class="time">{{ formatTime(msg.createTime) }}</time>
              </div>
              <div class="bubble">{{ msg.content }}</div>
            </div>
            <UserAvatar
              v-if="isMine(msg)"
              class="msg-avatar"
              :src="msg.avatar"
              :name="msg.username"
              :size="36"
            />
          </div>
        </div>

        <form v-if="authStore.isLoggedIn" class="composer" @submit.prevent="onSend">
          <n-input
            v-model:value="draft"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入消息..."
            maxlength="1000"
            show-count
            @keydown.enter.exact.prevent="onSend"
          />
          <n-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="onSend">
            发送
          </n-button>
        </form>
      </section>

      <aside v-if="!isMobile" class="online-panel">
        <div class="online-head">在线用户 ({{ onlineUsers.length }})</div>
        <div class="online-list-body">
          <n-list v-if="onlineUsers.length" hoverable>
            <n-list-item v-for="user in onlineUsers" :key="user.userId">
              <template #prefix>
                <UserAvatar :src="user.avatar" :name="user.username" :size="32" />
              </template>
              <div class="online-name">
                {{ user.username }}
                <n-tag v-if="user.admin" size="small" type="warning" :bordered="false">管理员</n-tag>
              </div>
            </n-list-item>
          </n-list>
          <n-empty v-else description="暂无在线用户" size="small" />
        </div>
      </aside>
    </div>

    <n-drawer v-model:show="showOnlineDrawer" placement="right" :width="280">
      <n-drawer-content title="在线用户">
        <n-list v-if="onlineUsers.length" hoverable>
          <n-list-item v-for="user in onlineUsers" :key="user.userId">
            <template #prefix>
              <UserAvatar :src="user.avatar" :name="user.username" :size="32" />
            </template>
            <div class="online-name">
              {{ user.username }}
              <n-tag v-if="user.admin" size="small" type="warning" :bordered="false">管理员</n-tag>
            </div>
          </n-list-item>
        </n-list>
        <n-empty v-else description="暂无在线用户" />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100dvh - var(--layout-main-pad-top) - 5rem);
  min-height: 20rem;
  padding-bottom: var(--space-4);
  box-sizing: border-box;
  overflow: hidden;
}

.page-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.guest-tip {
  flex-shrink: 0;
  margin-bottom: var(--space-4);
}

.chat-layout {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: var(--space-4);
}

.chat-main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  overflow: hidden;
}

.message-list {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  max-width: 85%;
  width: fit-content;
}

.message-row.mine {
  margin-left: auto;
  flex-direction: row-reverse;
}

.msg-avatar {
  flex-shrink: 0;
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  width: fit-content;
  max-width: 100%;
}

.message-row.mine .bubble-wrap {
  align-items: flex-end;
}

.bubble-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.bubble-meta .name {
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.bubble {
  display: inline-block;
  width: fit-content;
  max-width: min(100%, 20rem);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  color: var(--color-text);
  line-height: 1.5;
  word-break: break-word;
}

.message-row.mine .bubble {
  background: var(--color-primary);
  color: #fff;
}

.composer {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-2);
  align-items: flex-end;
  padding: var(--space-3);
  border-top: 1px solid var(--color-border);
}

.composer :deep(.n-input) {
  flex: 1;
}

.online-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  padding: var(--space-3);
  overflow: hidden;
}

.online-head {
  flex-shrink: 0;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-3);
}

.online-list-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.online-name {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

@media (max-width: 1023px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .chat-page {
    height: calc(
      100dvh - var(--layout-main-pad-top) - var(--mobile-dock-height) -
        env(safe-area-inset-bottom, 0px) - 4rem
    );
  }
}
</style>
