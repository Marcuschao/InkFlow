<template>
  <header
    class="navbar"
    :class="{ scrolled: isScrolled, 'nav-hidden': hideNav, 'navbar-menu-open': isMenuOpen }"
  >
    <nav class="nav-inner" aria-label="主导航">
      <div class="container nav-row">
        <router-link to="/" class="logo" @click="closeMenu">晓晓博客</router-link>
        <button
          type="button"
          class="menu-toggle"
          :class="{ open: isMenuOpen }"
          aria-label="菜单"
          :aria-expanded="isMenuOpen"
          aria-controls="primary-nav"
          @click="toggleMenu"
        >
          <span class="bar"></span>
          <span class="bar"></span>
          <span class="bar"></span>
        </button>
        <ul id="primary-nav" class="nav-links" :class="{ open: isMenuOpen }">
          <li><router-link to="/" @click="closeMenu">首页</router-link></li>
          <li><router-link to="/archive" @click="closeMenu">归档</router-link></li>
          <li><router-link to="/tags" @click="closeMenu">标签</router-link></li>
          <li><router-link to="/search" @click="closeMenu">搜索</router-link></li>
          <li><router-link to="/links" @click="closeMenu">友链</router-link></li>
          <li><router-link to="/diary" @click="closeMenu">日记</router-link></li>
          <li><router-link to="/reading-history" @click="closeMenu">阅读记录</router-link></li>
          <li>
            <router-link
              :to="authStore.isLoggedIn ? '/admin' : '/login'"
              class="nav-admin"
              @click="closeMenu"
            >{{ authStore.isLoggedIn ? '管理' : '登录' }}</router-link>
          </li>
        </ul>
      </div>
    </nav>
    <transition name="backdrop-fade">
      <div
        v-if="isMenuOpen"
        class="nav-backdrop"
        aria-hidden="true"
        @click="closeMenu"
      />
    </transition>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const authStore = useAuthStore();
const isMenuOpen = ref(false);
const isScrolled = ref(false);
const hideNav = ref(false);
let lastY = 0;

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

const closeMenu = () => {
  isMenuOpen.value = false;
};

const onScroll = () => {
  const y = window.scrollY || document.documentElement.scrollTop;
  isScrolled.value = y > 12;
  if (window.innerWidth <= 1023 || route.path.startsWith('/admin')) {
    hideNav.value = false;
    lastY = y;
    return;
  }
  hideNav.value = y > lastY && y > 96;
  lastY = y;
};

onMounted(() => {
  lastY = window.scrollY || 0;
  window.addEventListener('scroll', onScroll, { passive: true });
});

watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/admin')) {
      hideNav.value = false;
    }
  }
);

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll);
});
</script>

<style scoped>
.navbar {
  position: fixed;
  top: var(--layout-navbar-top);
  left: 0;
  right: 0;
  z-index: var(--z-nav);
  transition: transform var(--transition-smooth), box-shadow var(--transition-fast);
}

@media (min-width: 1024px) {
  .navbar {
    left: var(--nav-float-gap);
    right: var(--nav-float-gap);
    width: auto;
  }
}

.navbar.nav-hidden:not(.navbar-menu-open) {
  transform: translateY(-100%);
}

.navbar.navbar-menu-open .nav-inner {
  z-index: var(--z-nav-menu);
}

.navbar.scrolled .nav-inner {
  box-shadow: var(--shadow-sm);
  background: var(--color-surface);
}

.nav-inner {
  position: relative;
  min-height: var(--nav-height);
  display: flex;
  align-items: center;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  box-shadow: none;
  transition: background var(--transition-fast), box-shadow var(--transition-fast),
    min-height var(--transition-fast);
}

@media (min-width: 1024px) {
  .nav-inner {
    border-radius: var(--radius-xl);
    border: 1px solid var(--color-border);
    border-bottom: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
  }
}

.navbar.scrolled .nav-inner {
  min-height: 3.5rem;
}

.nav-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.logo {
  font-family: var(--font-display), var(--font-ui);
  font-weight: 700;
  font-size: 1.35rem;
  letter-spacing: 0.02em;
  text-decoration: none;
  color: var(--color-text);
  transition: color var(--transition-fast);
}

.logo:hover {
  color: var(--color-primary);
}

.menu-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 44px;
  height: 44px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: var(--radius-sm);
  color: var(--color-text);
}

.menu-toggle:focus-visible {
  outline: 2px solid var(--color-primary);
}

.bar {
  display: block;
  width: 22px;
  height: 2px;
  margin: 0 auto;
  background: currentColor;
  border-radius: var(--radius-pill);
  transition: transform var(--transition-fast), opacity var(--transition-fast);
}

.menu-toggle.open .bar:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}

.menu-toggle.open .bar:nth-child(2) {
  opacity: 0;
}

.menu-toggle.open .bar:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

.nav-links {
  list-style: none;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.nav-links li {
  margin: 0;
}

.nav-links a {
  position: relative;
  display: block;
  padding: 0.5rem 0.85rem;
  color: var(--color-text-muted);
  text-decoration: none;
  font-size: 0.92rem;
  font-weight: 500;
  border-radius: var(--radius-pill);
  cursor: pointer;
  transition: color var(--transition-fast), background var(--transition-fast);
}

.nav-links a::after {
  display: none;
}

.nav-links a:hover {
  color: var(--color-text);
  background: var(--surface-muted);
}

.nav-links a.router-link-active {
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
}

.nav-links a.router-link-active::after {
  width: 0;
}

.nav-links a.nav-admin {
  background: var(--color-text);
  color: #fff;
}

.nav-links a.nav-admin:hover {
  background: var(--color-admin-hover);
  color: #fff;
}

.nav-links a.nav-admin::after {
  display: none;
}

.nav-backdrop {
  position: fixed;
  inset: 0;
  top: var(--layout-navbar-bottom);
  z-index: var(--z-nav-backdrop);
  background: var(--color-overlay-nav);
  backdrop-filter: blur(4px);
}

.backdrop-fade-enter-active,
.backdrop-fade-leave-active {
  transition: opacity 0.28s var(--ease-out-soft);
}

.backdrop-fade-enter-from,
.backdrop-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1023px) {
  .nav-backdrop {
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    background: var(--color-overlay-nav-mobile);
  }

  .menu-toggle {
    display: flex;
  }

  .navbar.nav-hidden:not(.navbar-menu-open) {
    transform: none;
  }

  .nav-links {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    padding: 0.75rem 1rem 1.25rem;
    background: var(--color-surface);
    border-bottom: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    max-height: min(70vh, 440px);
    overflow-y: auto;
    transform-origin: top;
    transform: translateY(-12px) scale(0.98);
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transition: transform 0.32s var(--ease-out-soft), opacity 0.28s var(--ease-out-soft),
      visibility 0.32s;
  }

  .nav-links.open {
    transform: translateY(0) scale(1);
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }

  .nav-links li {
    border-bottom: 1px solid var(--color-border);
  }

  .nav-links li:last-child {
    border-bottom: none;
  }

  .nav-links a {
    padding: 0.95rem 0.5rem;
    border-radius: var(--radius-sm);
  }

  .nav-links a::after {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .nav-links {
    transition: none;
  }

  .navbar.nav-hidden:not(.navbar-menu-open) {
    transform: none;
  }
}
</style>
