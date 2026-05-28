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
  NVirtualList,
  useMessage,
} from 'naive-ui';
import UserAvatar from '../components/UserAvatar.vue';
import { fetchChatHistory, fetchOnlineUsers, sendChatMessage, pingPresence } from '../api/chat';
import { useAuthStore } from '../stores/auth';
import { connect, onChatMessage, onStatusChange } from '../services/websocket';

const INITIAL_LIMIT = 50;
const PAGE_SIZE = 30;
const MAX_RENDER = 200;
const TRIM_COUNT = 50;
const NEAR_BOTTOM_PX = 100;

const authStore = useAuthStore();
const message = useMessage();
const messages = ref([]);
const onlineUsers = ref([]);
const draft = ref('');
const loading = ref(true);
const loadingMore = ref(false);
const hasMore = ref(true);
const sending = ref(false);
const showOnlineDrawer = ref(false);
const showNewHint = ref(false);
const isMobile = ref(false);
const isNearBottom = ref(true);
const virtualListRef = ref(null);
const renderedIds = new Set();
let onlineTimer = null;
let stopChatListener = () => {};
let stopStatusListener = () => {};

function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  return d.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    timeZone: 'Asia/Shanghai',
  });
}

function isMine(msg) {
  return authStore.user?.id != null && msg.userId === authStore.user.id;
}

function trimMessages() {
  if (messages.value.length <= MAX_RENDER) return;
  const removed = messages.value.splice(0, TRIM_COUNT);
  removed.forEach((m) => renderedIds.delete(m.id));
  hasMore.value = true;
}

async function scrollToBottom() {
  await nextTick();
  const inst = virtualListRef.value;
  if (!inst || !messages.value.length) return;
  inst.scrollTo({ index: messages.value.length - 1, debounce: false });
  isNearBottom.value = true;
  showNewHint.value = false;
}

function prependMessages(list) {
  const incoming = [];
  for (const msg of list) {
    if (!msg?.id || renderedIds.has(msg.id)) continue;
    renderedIds.add(msg.id);
    incoming.push(msg);
  }
  if (incoming.length) {
    messages.value = [...incoming, ...messages.value];
  }
}

function appendMessage(msg, forceScroll = false) {
  if (!msg?.id || renderedIds.has(msg.id)) return;
  renderedIds.add(msg.id);
  messages.value.push(msg);
  trimMessages();
  if (forceScroll || isNearBottom.value) {
    scrollToBottom();
  } else {
    showNewHint.value = true;
  }
}

async function loadHistory() {
  loading.value = true;
  renderedIds.clear();
  try {
    const res = await fetchChatHistory({ limit: INITIAL_LIMIT });
    const data = res?.data ?? res;
    const rows = data?.messages || [];
    hasMore.value = !!data?.hasMore;
    rows.forEach((msg) => {
      if (msg?.id) renderedIds.add(msg.id);
    });
    messages.value = rows;
    await scrollToBottom();
  } catch {
    messages.value = [];
    hasMore.value = false;
  } finally {
    loading.value = false;
  }
}

async function loadOlder() {
  if (loadingMore.value || !hasMore.value || !messages.value.length) return;
  loadingMore.value = true;
  const anchorId = messages.value[0].id;
  try {
    const res = await fetchChatHistory({ cursor: anchorId, limit: PAGE_SIZE });
    const data = res?.data ?? res;
    const older = data?.messages || [];
    hasMore.value = !!data?.hasMore;
    if (!older.length) {
      hasMore.value = false;
      return;
    }
    prependMessages(older);
    await nextTick();
    const idx = messages.value.findIndex((m) => m.id === anchorId);
    if (idx >= 0) {
      virtualListRef.value?.scrollTo({ index: idx, debounce: false });
    }
  } catch {
    /* ignore */
  } finally {
    loadingMore.value = false;
  }
}

async function syncMissedMessages() {
  const last = messages.value[messages.value.length - 1];
  if (!last?.id) return;
  try {
    const res = await fetchChatHistory({ afterId: last.id, limit: 100 });
    const data = res?.data ?? res;
    const rows = data?.messages || [];
    rows.forEach((msg) => appendMessage(msg, false));
    if (isNearBottom.value) {
      await scrollToBottom();
    }
  } catch {
    /* ignore */
  }
}

function onListScroll(e) {
  const el = e?.target;
  if (!el) return;
  const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
  isNearBottom.value = distance <= NEAR_BOTTOM_PX;
  if (isNearBottom.value) {
    showNewHint.value = false;
  }
  if (el.scrollTop <= 8 && !loadingMore.value && hasMore.value) {
    loadOlder();
  }
}

function onJumpNew() {
  scrollToBottom();
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
    if (res.data) appendMessage(res.data, true);
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
    syncMissedMessages();
    if (authStore.isLoggedIn) pingPresence().catch(() => {});
  }
}

function setupListeners() {
  stopChatListener = onChatMessage((msg) => appendMessage(msg, false));
  stopStatusListener = onStatusChange((connected) => {
    if (connected) {
      loadOnlineUsers();
      syncMissedMessages();
    }
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
  await syncMissedMessages();
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
        <div class="message-list-wrap">
          <n-virtual-list
            v-if="messages.length"
            ref="virtualListRef"
            class="message-list"
            :items="messages"
            :item-size="88"
            item-resizable
            key-field="id"
            @scroll="onListScroll"
          >
            <template #default="{ item }">
              <div class="message-row" :class="{ mine: isMine(item) }">
                <UserAvatar
                  v-if="!isMine(item)"
                  class="msg-avatar"
                  :src="item.avatar"
                  :name="item.username"
                  :size="36"
                />
                <div class="bubble-wrap">
                  <div class="bubble-meta">
                    <span class="name">{{ item.username }}</span>
                    <n-tag v-if="item.admin" size="small" type="warning" :bordered="false">管理员</n-tag>
                    <time class="time">{{ formatTime(item.createTime) }}</time>
                  </div>
                  <div class="bubble">{{ item.content }}</div>
                </div>
                <UserAvatar
                  v-if="isMine(item)"
                  class="msg-avatar"
                  :src="item.avatar"
                  :name="item.username"
                  :size="36"
                />
              </div>
            </template>
          </n-virtual-list>
          <n-empty v-else-if="!loading" description="暂无消息，来聊两句吧" />
          <n-button
            v-if="showNewHint"
            class="new-msg-hint"
            size="small"
            type="primary"
            secondary
            @click="onJumpNew"
          >
            新消息
          </n-button>
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

.message-list-wrap {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
}

.message-list {
  height: 100%;
  padding: var(--space-4);
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  max-width: 85%;
  width: fit-content;
  padding-bottom: var(--space-3);
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

.new-msg-hint {
  position: absolute;
  left: 50%;
  bottom: var(--space-3);
  transform: translateX(-50%);
  z-index: 2;
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
