<script setup>
import { ref, computed, onMounted, onUnmounted, onActivated, nextTick } from 'vue';
import {
  NAlert,
  NButton,
  NDrawer,
  NDrawerContent,
  NDropdown,
  NEmpty,
  NInput,
  NList,
  NListItem,
  NTag,
  NVirtualList,
  useMessage,
} from 'naive-ui';
import UserAvatar from '../components/UserAvatar.vue';
import { fetchChatHistory, fetchMuteStatus, fetchOnlineUsers, pingPresence, recallChatMessage, sendChatMessage } from '../api/chat';
import { useAuthStore } from '../stores/auth';
import { useChatUserProfiles } from '../composables/useChatUserProfiles';
import {
  connect,
  ensureSubscribed,
  onChatAck,
  onChatMessage,
  onChatOffline,
  onChatProfileUpdate,
  onChatRecall,
  onStatusChange,
  sendRecall,
} from '../services/websocket';
import { formatChatMessageTime } from '../utils/format';

const INITIAL_LIMIT = 50;
const PAGE_SIZE = 30;
const MAX_RENDER = 200;
const TRIM_COUNT = 50;
const NEAR_BOTTOM_PX = 100;
const ACK_TIMEOUT = 3000;
const LONG_PRESS_MS = 500;

const authStore = useAuthStore();
const { mergeFromMessages, setProfile, profileOf } = useChatUserProfiles();
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
const muteInfo = ref({ muted: false, muteUntil: null });
const menuShow = ref(false);
const menuX = ref(0);
const menuY = ref(0);
const menuTarget = ref(null);
const renderedIds = new Set();
const pendingAcks = new Map();
let onlineTimer = null;
let longPressTimer = null;
let stopChatListener = () => {};
let stopAckListener = () => {};
let stopOfflineListener = () => {};
let stopRecallListener = () => {};
let stopProfileListener = () => {};
let stopStatusListener = () => {};

const menuOptions = [{ label: '撤回', key: 'recall' }];
const composerDisabled = computed(() => muteInfo.value.muted || sending.value);

function makeKey(msg) {
  return msg.id ?? msg.clientMsgId ?? `tmp-${Date.now()}`;
}

function newClientMsgId() {
  return crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function isMine(msg) {
  return authStore.user?.id != null && msg.userId === authStore.user.id;
}

function displayContent(item) {
  if (item.recalled) return '消息已撤回';
  if (item.status === 'failed') return `${item.content}（发送失败）`;
  if (item.pending) return item.content;
  return item.content;
}

function canRecall(msg) {
  if (!isMine(msg) || msg.recalled || msg.pending || !msg.id) return false;
  if (!msg.createTime) return false;
  return Date.now() - new Date(msg.createTime).getTime() <= 2 * 60 * 1000;
}

function trimMessages() {
  if (messages.value.length <= MAX_RENDER) return;
  const removed = messages.value.splice(0, TRIM_COUNT);
  removed.forEach((m) => {
    if (m.id) renderedIds.delete(m.id);
  });
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
    incoming.push({ ...msg, _key: makeKey(msg) });
  }
  if (incoming.length) {
    messages.value = [...incoming, ...messages.value];
    mergeFromMessages(incoming);
  }
}

function appendMessage(msg, forceScroll = false) {
  if (msg?.id && renderedIds.has(msg.id)) return;
  if (msg?.id) renderedIds.add(msg.id);
  messages.value.push({ ...msg, _key: makeKey(msg) });
  mergeFromMessages([msg]);
  trimMessages();
  if (forceScroll || isNearBottom.value) {
    scrollToBottom();
  } else {
    showNewHint.value = true;
  }
}

function applyRecall(messageId) {
  const idx = messages.value.findIndex((m) => m.id === messageId);
  if (idx < 0) return;
  messages.value[idx] = {
    ...messages.value[idx],
    content: '消息已撤回',
    recalled: true,
    _key: makeKey(messages.value[idx]),
  };
}

function upsertAck(ack) {
  const idx = messages.value.findIndex(
    (m) => (ack.clientMsgId && m.clientMsgId === ack.clientMsgId) || m.id === ack.messageId
  );
  if (idx < 0) return;
  const prev = messages.value[idx];
  if (prev.id) renderedIds.delete(prev.id);
  renderedIds.add(ack.messageId);
  messages.value[idx] = {
    ...prev,
    id: ack.messageId,
    pending: false,
    status: 'sent',
    _key: String(ack.messageId),
  };
}

function markFailed(clientMsgId) {
  const idx = messages.value.findIndex((m) => m.clientMsgId === clientMsgId);
  if (idx < 0) return;
  messages.value[idx] = {
    ...messages.value[idx],
    pending: false,
    status: 'failed',
    _key: makeKey(messages.value[idx]),
  };
}

function clearPending(clientMsgId) {
  const pending = pendingAcks.get(clientMsgId);
  if (pending?.timer) clearTimeout(pending.timer);
  pendingAcks.delete(clientMsgId);
}

function scheduleAckWatch(clientMsgId) {
  clearPending(clientMsgId);
  const timer = setTimeout(() => {
    markFailed(clientMsgId);
    clearPending(clientMsgId);
  }, ACK_TIMEOUT);
  pendingAcks.set(clientMsgId, { timer });
}

function applySentMessage(clientMsgId, vo) {
  clearPending(clientMsgId);
  const idx = messages.value.findIndex((m) => m.clientMsgId === clientMsgId);
  if (idx < 0) {
    if (vo) appendMessage(vo, true);
    return;
  }
  if (vo?.id) renderedIds.add(vo.id);
  messages.value[idx] = {
    ...messages.value[idx],
    ...vo,
    id: vo.id,
    clientMsgId,
    pending: false,
    status: 'sent',
    _key: String(vo.id),
  };
}

async function loadHistory() {
  loading.value = true;
  renderedIds.clear();
  try {
    const res = await fetchChatHistory({ limit: INITIAL_LIMIT });
    const data = res ?? {};
    const rows = data?.messages || [];
    hasMore.value = !!data?.hasMore;
    rows.forEach((msg) => {
      if (msg?.id) renderedIds.add(msg.id);
    });
    messages.value = rows.map((msg) => ({ ...msg, _key: makeKey(msg) }));
    mergeFromMessages(rows);
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
    const data = res ?? {};
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
  const last = [...messages.value].reverse().find((m) => m.id && !String(m.id).startsWith('pending-'));
  if (!last?.id) return;
  try {
    const res = await fetchChatHistory({ afterId: last.id, limit: 100 });
    const data = res ?? {};
    const rows = data?.messages || [];
    rows.forEach((msg) => appendMessage(msg, false));
    if (isNearBottom.value) {
      await scrollToBottom();
    }
  } catch {
    /* ignore */
  }
}

async function loadMuteStatus() {
  if (!authStore.isLoggedIn) {
    muteInfo.value = { muted: false, muteUntil: null };
    return;
  }
  try {
    muteInfo.value = await fetchMuteStatus();
  } catch {
    muteInfo.value = { muted: false, muteUntil: null };
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
    const rows = await fetchOnlineUsers();
    onlineUsers.value = Array.isArray(rows) ? rows : [];
    for (const user of onlineUsers.value) {
      if (user?.userId) {
        setProfile(user.userId, { username: user.username, avatar: user.avatar });
      }
    }
  } catch {
    onlineUsers.value = [];
  }
}

function applyProfileUpdate(evt) {
  if (!evt?.userId) return;
  setProfile(evt.userId, { username: evt.username, avatar: evt.avatar });
  onlineUsers.value = onlineUsers.value.map((user) =>
    user.userId === evt.userId
      ? { ...user, username: evt.username, avatar: evt.avatar }
      : user
  );
}

async function ensureConnected() {
  await connect(authStore.token || null);
  await ensureSubscribed();
}

async function onSend() {
  const content = draft.value.trim();
  if (!content || sending.value || muteInfo.value.muted) return;
  sending.value = true;
  const clientMsgId = newClientMsgId();
  const optimistic = {
    clientMsgId,
    id: `pending-${clientMsgId}`,
    userId: authStore.user.id,
    username: authStore.displayName,
    avatar: authStore.user.avatar,
    admin: authStore.isAdmin,
    content,
    pending: true,
    status: 'sending',
    createTime: new Date().toISOString(),
    _key: clientMsgId,
  };
  messages.value.push(optimistic);
  draft.value = '';
  await scrollToBottom();
  scheduleAckWatch(clientMsgId);
  try {
    const vo = await sendChatMessage(content, clientMsgId);
    applySentMessage(clientMsgId, vo);
  } catch (err) {
    markFailed(clientMsgId);
    clearPending(clientMsgId);
    message.error(err?.message || '发送失败');
  } finally {
    sending.value = false;
  }
}

function openMenu(e, item) {
  if (!canRecall(item)) return;
  menuTarget.value = item;
  menuX.value = e.clientX;
  menuY.value = e.clientY;
  menuShow.value = true;
}

function onTouchStart(e, item) {
  if (!canRecall(item)) return;
  onTouchEnd();
  longPressTimer = setTimeout(() => {
    const touch = e.touches?.[0];
    if (!touch) return;
    menuTarget.value = item;
    menuX.value = touch.clientX;
    menuY.value = touch.clientY;
    menuShow.value = true;
  }, LONG_PRESS_MS);
}

function onTouchEnd() {
  if (longPressTimer) {
    clearTimeout(longPressTimer);
    longPressTimer = null;
  }
}

async function onMenuSelect(key) {
  menuShow.value = false;
  if (key !== 'recall' || !menuTarget.value?.id) return;
  try {
    await recallChatMessage(menuTarget.value.id);
  } catch {
    try {
      sendRecall(menuTarget.value.id);
    } catch {
      message.error('撤回失败');
    }
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
    loadMuteStatus();
    if (authStore.isLoggedIn) pingPresence().catch(() => {});
  }
}

function setupListeners() {
  stopChatListener = onChatMessage((msg) => {
    if (
      msg.clientMsgId
      && isMine(msg)
      && messages.value.some((m) => m.clientMsgId === msg.clientMsgId)
    ) {
      upsertAck({ clientMsgId: msg.clientMsgId, messageId: msg.id });
      return;
    }
    appendMessage(msg, false);
  });
  stopAckListener = onChatAck((ack) => {
    clearPending(ack.clientMsgId);
    upsertAck(ack);
  });
  stopOfflineListener = onChatOffline((msg) => appendMessage(msg, false));
  stopRecallListener = onChatRecall((evt) => {
    if (evt?.messageId) applyRecall(evt.messageId);
  });
  stopProfileListener = onChatProfileUpdate((evt) => applyProfileUpdate(evt));
  stopStatusListener = onStatusChange((connected) => {
    if (connected) {
      ensureSubscribed().catch(() => {});
      loadOnlineUsers();
      syncMissedMessages();
    }
  });
}

async function initChat() {
  await ensureConnected();
  if (authStore.isLoggedIn) {
    pingPresence().catch(() => {});
    await loadMuteStatus();
  }
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
  if (authStore.isLoggedIn) {
    pingPresence().catch(() => {});
    await loadMuteStatus();
  }
  await loadOnlineUsers();
  await syncMissedMessages();
});

onUnmounted(() => {
  window.removeEventListener('resize', updateMobile);
  document.removeEventListener('visibilitychange', onVisibilityChange);
  onTouchEnd();
  if (onlineTimer) clearInterval(onlineTimer);
  pendingAcks.forEach((item) => {
    if (item.timer) clearTimeout(item.timer);
  });
  pendingAcks.clear();
  stopChatListener();
  stopAckListener();
  stopOfflineListener();
  stopRecallListener();
  stopProfileListener();
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

    <n-alert v-if="muteInfo.muted" type="warning" :bordered="false" class="guest-tip">
      您已被禁言，解除时间：{{ formatChatMessageTime(muteInfo.muteUntil) }}
    </n-alert>

    <n-dropdown
      :show="menuShow"
      trigger="manual"
      placement="bottom-start"
      :x="menuX"
      :y="menuY"
      :options="menuOptions"
      @select="onMenuSelect"
      @clickoutside="menuShow = false"
    />

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
            key-field="_key"
            @scroll="onListScroll"
          >
            <template #default="{ item }">
              <div class="message-row" :class="{ mine: isMine(item) }">
                <UserAvatar
                  v-if="!isMine(item)"
                  class="msg-avatar"
                  :src="profileOf(item.userId).avatar"
                  :name="profileOf(item.userId).username"
                  :size="36"
                />
                <div class="bubble-wrap">
                  <div class="bubble-meta">
                    <span class="name">{{ profileOf(item.userId).username }}</span>
                    <n-tag v-if="item.admin" size="small" type="warning" :bordered="false">管理员</n-tag>
                    <time class="time">{{ formatChatMessageTime(item.createTime) }}</time>
                  </div>
                  <div
                    class="bubble"
                    :class="{ recalled: item.recalled, failed: item.status === 'failed' }"
                    @contextmenu.prevent="openMenu($event, item)"
                    @touchstart.passive="onTouchStart($event, item)"
                    @touchend="onTouchEnd"
                    @touchmove="onTouchEnd"
                  >
                    {{ displayContent(item) }}
                  </div>
                </div>
                <UserAvatar
                  v-if="isMine(item)"
                  class="msg-avatar"
                  :src="profileOf(item.userId).avatar"
                  :name="profileOf(item.userId).username"
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
            :disabled="muteInfo.muted"
            placeholder="输入消息..."
            maxlength="1000"
            show-count
            @keydown.enter.exact.prevent="onSend"
          />
          <n-button type="primary" :loading="sending" :disabled="!draft.trim() || composerDisabled" @click="onSend">
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
                <UserAvatar :src="profileOf(user.userId).avatar" :name="profileOf(user.userId).username" :size="32" />
              </template>
              <div class="online-name">
                {{ profileOf(user.userId).username }}
                <n-tag v-if="user.admin" size="small" type="warning" :bordered="false">管理员</n-tag>
              </div>
            </n-list-item>
          </n-list>
          <n-empty v-else description="暂无在线用户" size="small" />
        </div>
      </aside>
    </div>

    <n-drawer
      v-model:show="showOnlineDrawer"
      placement="right"
      :width="280"
      :block-scroll="true"
      :trap-focus="false"
    >
      <n-drawer-content title="在线用户">
        <n-list v-if="onlineUsers.length" hoverable>
          <n-list-item v-for="user in onlineUsers" :key="user.userId">
            <template #prefix>
              <UserAvatar :src="profileOf(user.userId).avatar" :name="profileOf(user.userId).username" :size="32" />
            </template>
            <div class="online-name">
              {{ profileOf(user.userId).username }}
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
  border: var(--border-brutal);
  border-radius: var(--radius-brutal-card);
  box-shadow: var(--shadow-brutal-lg);
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

.bubble.recalled,
.message-row.mine .bubble.recalled {
  background: var(--surface-muted);
  color: var(--color-text-muted);
}

.bubble.failed,
.message-row.mine .bubble.failed {
  opacity: 0.72;
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
        env(safe-area-inset-bottom, 0px) - 3rem
    );
    padding: var(--space-4) var(--space-4) var(--space-2);
    box-sizing: border-box;
  }

  .chat-page.ds-page {
    padding-top: var(--space-4);
    padding-bottom: var(--space-2);
  }

  .chat-page .page-head {
    margin-bottom: var(--space-4);
    padding: 0;
  }

  .chat-page .page-head .ds-page-title {
    font-size: var(--text-xl);
    line-height: 1.25;
  }

  .chat-page .guest-tip {
    margin-bottom: var(--space-3);
  }

  .chat-page .chat-layout {
    gap: 0;
  }

  .chat-page .chat-main {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
  }

  .chat-page .message-list-wrap {
    background: var(--surface-muted);
  }

  .chat-page .message-list {
    height: 100%;
    padding: var(--space-4) 0;
  }

  .chat-page .message-row {
    max-width: 86%;
    padding: 0 var(--space-3) var(--space-4);
    box-sizing: border-box;
  }

  .chat-page .message-row:not(.mine) {
    margin-right: auto;
  }

  .chat-page .message-row.mine {
    margin-left: auto;
  }

  .chat-page .msg-avatar {
    margin-top: var(--space-1);
  }

  .chat-page .bubble-meta {
    gap: var(--space-1) var(--space-2);
    margin-bottom: var(--space-2);
    padding: 0;
  }

  .chat-page .bubble {
    padding: var(--space-3) var(--space-4);
    max-width: 100%;
    font-size: var(--text-sm);
    line-height: 1.55;
    border-radius: var(--radius-lg);
  }

  .chat-page .message-row:not(.mine) .bubble {
    border-top-left-radius: var(--radius-sm);
  }

  .chat-page .message-row.mine .bubble {
    border-top-right-radius: var(--radius-sm);
  }

  .chat-page .composer {
    padding: var(--space-3) var(--space-4);
    gap: var(--space-2);
    background: var(--color-surface);
  }

  .chat-page .composer :deep(.n-input) {
    min-width: 0;
  }

  .chat-page .composer :deep(.n-input__textarea-el) {
    font-size: var(--text-md);
  }

  .chat-page .composer :deep(.n-button) {
    flex-shrink: 0;
    min-width: 4rem;
  }
}
</style>
