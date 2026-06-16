<template>
  <div class="notifications-page ds-page container">
    <header class="page-head">
      <h1 class="ds-page-title">消息中心</h1>
      <n-button
        v-if="items.length"
        secondary
        size="small"
        :loading="markingAll"
        @click="onMarkAllRead"
      >全部已读</n-button>
    </header>

    <div class="notif-panel">
      <NotificationSkeleton v-if="loading" />
      <n-empty v-else-if="!items.length" description="暂无消息" class="notif-empty" />
      <div v-else class="notif-list">
        <div
          v-for="item in items"
          :key="item.id"
          :class="['notif-card', 'ds-hover-lift', { unread: !item.read }]"
        >
          <span v-if="!item.read" class="unread-dot" aria-hidden="true" />
          <UserAvatar :src="item.actorAvatar" :name="item.actorNickname" :size="40" />
          <div class="notif-body" @click="openItem(item)">
            <span class="content">{{ item.content }}</span>
            <time class="time">{{ formatTime(item.createTime) }}</time>
          </div>
          <n-button quaternary size="small" @click.stop="onDelete(item.id)">删除</n-button>
        </div>
      </div>
    </div>

    <Pagination
      v-if="total > pageSize"
      :total="total"
      :page-size="pageSize"
      :current-page="page"
      @changePage="loadPage"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { NButton, NEmpty } from 'naive-ui';
import NotificationSkeleton from '../components/skeleton/NotificationSkeleton.vue';
import Pagination from '../components/Pagination.vue';
import UserAvatar from '../components/UserAvatar.vue';
import {
  fetchNotifications,
  markRead,
  markAllRead,
  deleteNotification,
} from '../api/notification';
import { useNotificationStore } from '../stores/notification';

const router = useRouter();
const notificationStore = useNotificationStore();
const loading = ref(true);
const markingAll = ref(false);
const items = ref([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);

function formatTime(t) {
  if (!t) return '';
  const d = new Date(t);
  const now = Date.now();
  const diff = now - d.getTime();
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`;
  return d.toLocaleDateString();
}

function targetPath(item) {
  if (item.type === 'FOLLOW' && item.actorUserId) {
    return `/user/${item.actorUserId}`;
  }
  if (item.targetType === 'ARTICLE' && item.targetId) {
    return `/article/${item.targetId}`;
  }
  return null;
}

async function openItem(item) {
  if (!item.read) {
    try {
      await markRead(item.id);
      item.read = true;
      notificationStore.decrementUnread();
    } catch {
      /* ignore */
    }
  }
  const path = targetPath(item);
  if (path) router.push(path);
}

async function loadPage(p = 1) {
  loading.value = true;
  page.value = p;
  try {
    const res = await fetchNotifications({ page: p, size: pageSize.value });
    const d = res.data;
    items.value = d?.records || [];
    total.value = d?.total || 0;
  } catch {
    items.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

async function onMarkAllRead() {
  markingAll.value = true;
  try {
    await markAllRead();
    items.value = items.value.map((i) => ({ ...i, read: true }));
    notificationStore.clearUnread();
  } finally {
    markingAll.value = false;
  }
}

async function onDelete(id) {
  try {
    await deleteNotification(id);
    items.value = items.value.filter((i) => i.id !== id);
    if (!items.value.length && page.value > 1) {
      loadPage(page.value - 1);
    }
  } catch {
    /* ignore */
  }
}

onMounted(() => loadPage(1));
</script>

<style scoped>
.notifications-page {
  max-width: 40rem;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.notif-panel {
  background: transparent;
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.notif-card {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  position: relative;
  transition: background var(--transition-fast);
}

.notif-card.unread {
  background: var(--color-primary-soft);
}

.unread-dot {
  position: absolute;
  left: var(--space-2);
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary);
}

.notif-card.unread {
  padding-left: calc(var(--space-4) + var(--space-3));
}

.notif-empty {
  padding: var(--space-8) var(--space-4);
}

.notif-body {
  flex: 1;
  cursor: pointer;
  min-width: 0;
}

.content {
  display: block;
  font-size: var(--text-sm);
  line-height: 1.55;
  word-break: break-word;
}

.time {
  display: block;
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

@media (min-width: 768px) {
  .notifications-page {
    padding-top: var(--space-4);
    padding-bottom: var(--space-16);
  }
}

@media (max-width: 767px) {
  .notifications-page {
    padding: var(--space-4) var(--space-4)
      calc(var(--space-12) + var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
    box-sizing: border-box;
  }

  .notifications-page .page-head {
    margin-bottom: var(--space-4);
    padding: 0;
  }

  .notifications-page .page-head .ds-page-title {
    font-size: var(--text-xl);
    line-height: 1.25;
  }

  .notifications-page .notif-panel {
    border-radius: 0;
    box-shadow: none;
  }
}
</style>
