<template>
  <nav class="mobile-dock" aria-label="快捷导航">
    <router-link to="/" class="dock-link">首页</router-link>
    <router-link to="/archive" class="dock-link">归档</router-link>
    <router-link to="/chat" class="dock-link">聊天</router-link>
    <router-link to="/hot-search" class="dock-link">热搜</router-link>
    <router-link to="/about" class="dock-link">关于</router-link>
  </nav>
</template>

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
    padding: 0.45rem 0.35rem calc(0.45rem + env(safe-area-inset-bottom, 0px));
    background: var(--color-surface);
    border-top: 1px solid var(--color-border);
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
  }

  .dock-link {
    flex: 1;
    text-align: center;
    font-size: var(--text-xs);
    font-weight: 650;
    color: var(--color-text-muted);
    text-decoration: none;
    padding: 0.35rem 0.15rem;
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
