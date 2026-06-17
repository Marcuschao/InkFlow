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
} from '@vicons/ionicons5';
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();
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
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    border-top: 1px solid var(--color-border);
    box-shadow: var(--shadow-nav);
  }

  .dock-link {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-1);
    text-align: center;
    font-size: var(--text-xs);
    font-weight: var(--weight-medium);
    color: var(--color-text-soft);
    text-decoration: none;
    padding: var(--space-1);
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: color var(--transition-fast), background var(--transition-fast);
  }

  .dock-link.router-link-active {
    color: var(--color-primary);
    background: var(--surface-primary-tint);
  }
}
</style>
