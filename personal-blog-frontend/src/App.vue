<script setup>
import { onMounted, onUnmounted, watch } from 'vue';
import { RouterView } from 'vue-router';
import {
  NConfigProvider,
  NMessageProvider,
  NDialogProvider,
  zhCN,
  dateZhCN,
} from 'naive-ui';
import Navbar from './components/Navbar.vue';
import OfflineBanner from './components/OfflineBanner.vue';
import Footer from './components/Footer.vue';
import AiChatbot from './components/AiChatbot.vue';
import ScrollToTop from './components/ScrollToTop.vue';
import ToastHost from './components/ToastHost.vue';
import MobileDock from './components/MobileDock.vue';
import { useSiteStore } from './stores/site';
import { useAuthStore } from './stores/auth';
import { useNotificationStore } from './stores/notification';
import { useToastStore } from './stores/toast';
import { useTheme } from './composables/useTheme';
import { themeOverrides } from './theme/naiveTheme';
import { mountClickRipple } from './composables/useClickRipple';
import { connect, disconnect, onNotification } from './services/websocket';
import { pingPresence } from './api/chat';

const siteStore = useSiteStore();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const toastStore = useToastStore();
const { naiveTheme } = useTheme();

let stopRipple = () => {};
let stopNotifListener = () => {};
let presenceTimer = null;

function startPresence() {
  stopPresence();
  if (!authStore.isLoggedIn) return;
  pingPresence().catch(() => {});
  presenceTimer = setInterval(() => pingPresence().catch(() => {}), 20000);
}

function stopPresence() {
  if (presenceTimer) {
    clearInterval(presenceTimer);
    presenceTimer = null;
  }
}

function syncWebSocket(loggedIn) {
  if (loggedIn) {
    connect(authStore.token);
    notificationStore.refreshUnread();
    startPresence();
  } else {
    connect(null);
    notificationStore.clearUnread();
    stopPresence();
  }
}

onMounted(() => {
  siteStore.loadPublicConfig();
  stopRipple = mountClickRipple();
  if (authStore.isLoggedIn && !authStore.user) {
    authStore.fetchMe().catch(() => authStore.logout());
  }
  connect(authStore.token || null);
  stopNotifListener = onNotification((vo) => {
    if (!authStore.isLoggedIn) return;
    notificationStore.applyRealtimeNotification(vo);
    if (vo?.content) {
      toastStore.push(vo.content, 'success');
    }
  });
  if (authStore.isLoggedIn) {
    notificationStore.refreshUnread();
    startPresence();
  }
});

watch(
  () => authStore.isLoggedIn,
  (loggedIn) => syncWebSocket(loggedIn)
);

watch(
  () => authStore.token,
  (token, prev) => {
    if (token !== prev) {
      connect(token || null);
    }
  }
);

watch(
  () => siteStore.siteTitle,
  (title) => {
    if (title) document.title = title;
  },
  { immediate: true }
);

onUnmounted(() => {
  stopRipple();
  stopNotifListener();
  stopPresence();
  disconnect();
});
</script>

<template>
  <n-config-provider
    :locale="zhCN"
    :date-locale="dateZhCN"
    :theme="naiveTheme"
    :theme-overrides="themeOverrides"
  >
    <n-message-provider>
      <n-dialog-provider>
        <div id="app-wrapper">
          <Navbar />
          <div class="nav-layout-spacer" aria-hidden="true" />
          <OfflineBanner />
          <main class="main-content">
            <RouterView v-slot="{ Component, route }">
              <Transition name="page-fade" mode="out-in">
                <component :is="Component" v-if="Component" :key="route.path" />
              </Transition>
            </RouterView>
          </main>
          <Footer />
          <MobileDock />
          <ScrollToTop />
          <ToastHost />
          <AiChatbot />
        </div>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<style scoped>
#app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  width: 100%;
}

@media (max-width: 767px) {
  .main-content {
    padding-bottom: calc(3.5rem + env(safe-area-inset-bottom, 0px));
  }
}
</style>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s var(--ease-out-soft), transform 0.2s var(--ease-out-soft);
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (prefers-reduced-motion: reduce) {
  .page-fade-enter-active,
  .page-fade-leave-active {
    transition-duration: 0.01ms;
  }

  .page-fade-enter-from,
  .page-fade-leave-to {
    transform: none;
  }
}
</style>
