<template>
  <nav class="mobile-dock" aria-label="快捷导航">
    <router-link to="/" class="dock-link">
      <n-icon :component="HomeOutline" :size="20" />
      <span>首页</span>
    </router-link>
    <router-link to="/archive" class="dock-link">
      <n-icon :component="ArchiveOutline" :size="20" />
      <span>归档</span>
    </router-link>
    <router-link v-if="aiChatVisible" to="/ai-chat" class="dock-link">
      <span class="dock-icon-wrap">
        <n-icon :component="SparklesOutline" :size="20" />
        <span v-if="aiChat.hasNew" class="dock-dot" />
      </span>
      <span>AI</span>
    </router-link>
    <router-link to="/chat" class="dock-link">
      <n-icon :component="ChatbubbleOutline" :size="20" />
      <span>聊天</span>
    </router-link>
    <router-link
      v-if="authStore.isLoggedIn"
      to="/user/me"
      class="dock-link"
    >
      <n-icon :component="PersonOutline" :size="20" />
      <span>我的</span>
    </router-link>
    <router-link v-else to="/login" class="dock-link">
      <n-icon :component="PersonOutline" :size="20" />
      <span>登录</span>
    </router-link>
    <router-link to="/hot-search" class="dock-link">
      <n-icon :component="FlameOutline" :size="20" />
      <span>热搜</span>
    </router-link>
  </nav>
</template>

<script setup>
import { NIcon } from 'naive-ui';
import {
  HomeOutline,
  ArchiveOutline,
  ChatbubbleOutline,
  FlameOutline,
  PersonOutline,
  SparklesOutline,
} from '@vicons/ionicons5';
import { useAuthStore } from '../stores/auth';
import { useAiChatStore } from '../stores/aiChat';
import { useAiChatVisibility } from '../composables/useAiChatVisibility';

const authStore = useAuthStore();
const aiChat = useAiChatStore();
const { visible: aiChatVisible } = useAiChatVisibility();
</script>

<style scoped>
.mobile-dock {
  display: none;
}

@media (max-width: 767px) {
  .mobile-dock {
    display: flex;
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 1250;
    justify-content: space-around;
    align-items: center;
    padding: var(--space-2) var(--space-2) calc(var(--space-2) + env(safe-area-inset-bottom, 0px));
    background: var(--color-surface);
    border-top: var(--border-brutal);
    box-shadow: var(--shadow-brutal-sm);
  }

  .dock-link {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-1);
    text-align: center;
    font-size: var(--text-xs);
    font-weight: var(--weight-bold);
    color: var(--color-text-soft);
    text-decoration: none;
    padding: var(--space-1);
    cursor: pointer;
    border-radius: var(--radius-md);
    transition:
      color var(--transition-fast),
      background var(--transition-fast),
      transform var(--transition-fast);
    position: relative;
  }

  .dock-link.router-link-active {
    color: var(--color-on-primary);
    background: var(--color-accent);
    border: var(--border-brutal);
    box-shadow: var(--shadow-brutal-sm);
  }

  .dock-link.router-link-active::before {
    display: none;
  }

  .dock-icon-wrap {
    position: relative;
    display: inline-flex;
  }

  .dock-dot {
    position: absolute;
    top: -2px;
    right: -4px;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--color-danger);
  }
}
</style>
