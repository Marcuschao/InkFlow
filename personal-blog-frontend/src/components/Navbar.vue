<template>
  <header
    ref="navbarRef"
    class="navbar"
    :class="{ scrolled: isScrolled, 'nav-hidden': hideNav, 'navbar-menu-open': isMenuOpen }"
  >
    <nav class="nav-inner" aria-label="主导航">
      <div class="container nav-row">
        <router-link to="/" class="logo" @click="closeMenu">{{ siteStore.siteTitle }}</router-link>
        <button
          type="button"
          class="menu-toggle"
          :class="{ open: isMenuOpen }"
          aria-label="菜单"
          :aria-expanded="isMenuOpen"
          aria-controls="primary-nav"
          @click="toggleMenu"
        >
          <n-icon :component="isMenuOpen ? CloseOutline : MenuOutline" :size="24" />
        </button>
        <div id="primary-nav" class="nav-links" :class="{ open: isMenuOpen }">
          <div v-if="!authStore.isLoggedIn && isMobileNav && isMenuOpen" class="nav-auth-actions">
            <router-link to="/login" class="nav-auth-btn nav-auth-btn--primary" @click="closeMenu">登录</router-link>
            <router-link to="/register" class="nav-auth-btn nav-auth-btn--outline" @click="closeMenu">注册</router-link>
          </div>

          <n-menu
            v-if="!isMobileNav"
            class="nav-naive-menu nav-naive-menu--desktop"
            mode="horizontal"
            responsive
            :options="navMenuOptions"
            :value="navMenuActiveKey"
            accordion
            @update:value="onNavMenuUpdate"
          />
          <n-menu
            v-else
            class="nav-naive-menu nav-naive-menu--mobile"
            mode="vertical"
            :options="mobileNavMenuOptions"
            :value="navMenuActiveKey"
            accordion
            @update:value="onNavMenuUpdate"
          />

          <div class="nav-search-wrap">
            <SearchSuggest />
          </div>

          <router-link
            v-if="authStore.isLoggedIn"
            to="/write"
            class="nav-write-btn"
            @click="closeMenu"
          >
            写文章
          </router-link>

          <div v-if="authStore.isLoggedIn" class="nav-notif-wrap">
            <router-link
              to="/notifications"
              class="nav-bell"
              aria-label="消息中心"
              @click="closeMenu"
            >
              <n-badge dot :show="unreadCount > 0" :offset="[-2, 2]" processing>
                <n-icon :component="NotificationsOutline" :size="20" />
              </n-badge>
            </router-link>
          </div>

          <div v-if="authStore.isLoggedIn" class="nav-user-wrap">
            <div class="nav-user-dropdown-wrap">
              <n-dropdown
                trigger="click"
                placement="bottom-end"
                :options="userDropdownOptions"
                :show-arrow="false"
                :z-index="1300"
                :style="{ minWidth: '10rem' }"
                @select="onUserDropdownSelect"
              >
                <button
                  type="button"
                  class="nav-user-trigger"
                  aria-haspopup="menu"
                >
                  <UserAvatar
                    class="nav-avatar"
                    :src="authStore.user?.avatar"
                    :name="authStore.displayName || authStore.user?.username"
                    :size="28"
                  />
                  <span class="nav-username-short">{{ authStore.displayName }}</span>
                </button>
              </n-dropdown>
            </div>
          </div>

          <div v-if="authStore.isAdmin" class="nav-admin-li-desktop">
            <router-link to="/admin" class="nav-admin" @click="closeMenu">管理</router-link>
          </div>

        </div>
      </div>
    </nav>
    <transition name="backdrop-fade">
      <div
        v-if="isMenuOpen"
        class="nav-backdrop"
        aria-hidden="true"
        @click="closeMenu"
        @touchmove.prevent
      />
    </transition>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NMenu, NBadge, NDropdown, NIcon } from 'naive-ui';
import { NotificationsOutline, MenuOutline, CloseOutline } from '@vicons/ionicons5';
import { useAuthStore } from '../stores/auth';
import { useNotificationStore } from '../stores/notification';
import { useSiteStore } from '../stores/site';
import UserAvatar from './UserAvatar.vue';
import SearchSuggest from './SearchSuggest.vue';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const siteStore = useSiteStore();
const isMenuOpen = ref(false);
const isMobileNav = ref(false);
const isScrolled = ref(false);
const hideNav = ref(false);
const navbarRef = ref(null);
let lastY = 0;
let navResizeObserver = null;

function syncNavLayoutOffset() {
  if (!window.matchMedia('(min-width: 1024px)').matches) {
    document.documentElement.style.removeProperty('--layout-navbar-bottom');
    document.documentElement.style.removeProperty('--layout-main-pad-top');
    return;
  }
  const el = navbarRef.value?.querySelector('.nav-inner') || navbarRef.value;
  if (!el) return;
  const bottom = Math.ceil(el.getBoundingClientRect().height);
  const gap = 24;
  document.documentElement.style.setProperty('--layout-navbar-bottom', `${bottom}px`);
  document.documentElement.style.setProperty('--layout-main-pad-top', `${bottom + gap}px`);
}

function bindNavResizeObserver() {
  navResizeObserver?.disconnect();
  navResizeObserver = null;
  if (typeof ResizeObserver === 'undefined') return;
  const el = navbarRef.value?.querySelector('.nav-inner') || navbarRef.value;
  if (!el) return;
  navResizeObserver = new ResizeObserver(() => syncNavLayoutOffset());
  navResizeObserver.observe(el);
}

const unreadCount = computed(() => notificationStore.unreadCount);

const STATIC_NAV_KEYS = [
  '/',
  '/archive',
  '/tags',
  '/hot-search',
  '/search',
  '/links',
  '/share',
  '/reading-history',
  '/chat',
];

const navMenuActiveKey = computed(() => {
  const p = route.path;
  if (STATIC_NAV_KEYS.includes(p)) return p;
  if (p === '/user/me') return '/user/me';
  if (!authStore.isLoggedIn && (p === '/login' || p === '/register')) return p;
  return null;
});

const MAIN_NAV_OPTIONS = [
  { label: '首页', key: '/' },
  { label: '归档', key: '/archive' },
  { label: '知识星系', key: '/tags' },
  { label: '热搜', key: '/hot-search' },
  { label: '友链', key: '/links' },
  { label: '分享', key: '/share' },
  { label: '聊天室', key: '/chat' },
  { label: '阅读记录', key: '/reading-history' },
];

const navMenuOptions = computed(() => {
  const base = [...MAIN_NAV_OPTIONS];
  if (!authStore.isLoggedIn) {
    base.push({ label: '登录', key: '/login' });
    base.push({ label: '注册', key: '/register' });
  }
  return base;
});

const mobileNavMenuOptions = computed(() => {
  const base = [...MAIN_NAV_OPTIONS];
  if (authStore.isLoggedIn) {
    base.unshift({ label: '个人主页', key: '/user/me' });
  }
  return base;
});

function syncMobileNav() {
  isMobileNav.value = window.matchMedia('(max-width: 1023px)').matches;
}

const userDropdownOptions = computed(() => {
  const opts = [
    { label: '个人主页', key: 'profile' },
    { label: '我的文章', key: 'my-articles' },
    { label: '我的分享', key: 'my-shares' },
  ];
  if (authStore.isAdmin) {
    opts.push({ label: '管理后台', key: 'admin' });
  }
  opts.push({ label: '退出', key: 'logout' });
  return opts;
});

function onNavMenuUpdate(key) {
  if (typeof key !== 'string') return;
  router.push(key).catch(() => {});
  closeMenu();
}

async function refreshUnread() {
  if (!authStore.isLoggedIn) {
    notificationStore.clearUnread();
    return;
  }
  await notificationStore.refreshUnread();
}

function onUserDropdownSelect(key) {
  closeMenu();
  if (key === 'profile') router.push('/user/me').catch(() => {});
  else if (key === 'my-articles') router.push('/my-articles').catch(() => {});
  else if (key === 'my-shares') router.push('/my-shares').catch(() => {});
  else if (key === 'admin') router.push('/admin').catch(() => {});
  else if (key === 'logout') handleLogoutFromMenu();
}

const handleLogoutFromMenu = () => {
  authStore.logout();
  closeMenu();
  if (route.path.startsWith('/admin')) {
    router.push({ name: 'Home' });
  }
};

const closeMenu = () => {
  isMenuOpen.value = false;
};

const toggleMenu = () => {
  hideNav.value = false;
  isMenuOpen.value = !isMenuOpen.value;
};

const onScroll = () => {
  const y = window.scrollY || document.documentElement.scrollTop;
  isScrolled.value = y > 12;
  if (window.innerWidth <= 1023 || route.path.startsWith('/admin')) {
    hideNav.value = false;
    lastY = y;
    return;
  }
  const delta = y - lastY;
  if (y <= 96) {
    hideNav.value = false;
  } else if (delta > 10) {
    hideNav.value = true;
  } else if (delta < -10) {
    hideNav.value = false;
  }
  if (Math.abs(delta) >= 10) {
    lastY = y;
  }
};

onMounted(() => {
  syncMobileNav();
  nextTick(() => {
    syncNavLayoutOffset();
    bindNavResizeObserver();
  });
  window.addEventListener('resize', syncMobileNav, { passive: true });
  window.addEventListener('resize', syncNavLayoutOffset, { passive: true });
  lastY = window.scrollY || 0;
  window.addEventListener('scroll', onScroll, { passive: true });
  if (authStore.isLoggedIn) refreshUnread();
});

watch(
  () => authStore.isLoggedIn,
  () => nextTick(() => {
    syncNavLayoutOffset();
    bindNavResizeObserver();
  })
);

watch(
  () => route.path,
  () => nextTick(syncNavLayoutOffset)
);

watch(
  () => authStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) refreshUnread();
    else notificationStore.clearUnread();
  }
);

watch(
  () => route.path,
  (path) => {
    if (path === '/notifications' && authStore.isLoggedIn) {
      refreshUnread();
    }
  }
);

watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/admin')) {
      hideNav.value = false;
    }
    closeMenu();
    forceUnlockBodyScroll();
  }
);

let lockedScrollY = 0;
let preventTouchScroll = null;

function lockBodyScroll() {
  lockedScrollY = window.scrollY || document.documentElement.scrollTop || 0;
  document.documentElement.classList.add('nav-menu-scroll-lock');
  document.body.classList.add('nav-menu-scroll-lock');

  if (window.matchMedia('(max-width: 1023px)').matches) {
    preventTouchScroll = (e) => {
      const menu = document.getElementById('primary-nav');
      if (menu?.classList.contains('open') && menu.contains(e.target)) return;
      e.preventDefault();
    };
    document.addEventListener('touchmove', preventTouchScroll, { passive: false });
  }
}

function unlockBodyScroll() {
  document.documentElement.classList.remove('nav-menu-scroll-lock');
  document.body.classList.remove('nav-menu-scroll-lock');
  if (preventTouchScroll) {
    document.removeEventListener('touchmove', preventTouchScroll);
    preventTouchScroll = null;
  }
  window.scrollTo(0, lockedScrollY);
}

function forceUnlockBodyScroll() {
  if (document.body.classList.contains('nav-menu-scroll-lock')) {
    unlockBodyScroll();
  }
}

watch(isMenuOpen, (open) => {
  if (open) {
    hideNav.value = false;
    lockBodyScroll();
  } else {
    unlockBodyScroll();
  }
});

onUnmounted(() => {
  navResizeObserver?.disconnect();
  navResizeObserver = null;
  if (isMenuOpen.value) {
    unlockBodyScroll();
  }
  window.removeEventListener('resize', syncMobileNav);
  window.removeEventListener('resize', syncNavLayoutOffset);
  window.removeEventListener('scroll', onScroll);
  document.documentElement.style.removeProperty('--layout-navbar-bottom');
  document.documentElement.style.removeProperty('--layout-main-pad-top');
});
</script>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: var(--z-nav);
  transition: transform var(--transition-smooth), box-shadow var(--transition-fast);
}

.navbar.nav-hidden:not(.navbar-menu-open) {
  transform: translateY(-100%);
}

.navbar.navbar-menu-open .nav-inner {
  z-index: var(--z-nav-menu);
}

.navbar.scrolled .nav-inner {
  box-shadow: var(--shadow-nav);
}

.nav-inner {
  position: relative;
  min-height: var(--nav-height);
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-nav);
  transition: background var(--transition-fast), box-shadow var(--transition-fast);
}

.nav-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: var(--space-2);
}

@media (min-width: 1024px) {
  .nav-links {
    flex: 1;
    min-width: 0;
  }
}

.logo {
  font-family: var(--font-ui);
  font-weight: var(--weight-semibold);
  font-size: var(--text-lg);
  letter-spacing: 0;
  text-decoration: none;
  color: var(--color-text);
  transition: color var(--transition-fast);
}

.logo:hover {
  color: var(--color-primary);
}

.menu-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: var(--radius-md);
  color: var(--color-text);
}

.menu-toggle:focus-visible {
  outline: 2px solid var(--color-primary);
}

.nav-write-btn {
  display: inline-flex;
  align-items: center;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  background: var(--color-primary);
  color: #fff;
  border: 1px solid var(--color-primary);
  text-decoration: none;
  white-space: nowrap;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.nav-write-btn:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
}

.nav-links {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: var(--space-2);
}

.nav-links :deep(.n-menu) {
  background: transparent;
}

.nav-links :deep(.n-menu-item-content) {
  font-size: 0.92rem;
}

.nav-links :deep(.n-menu-item-content--selected) {
  font-weight: var(--weight-semibold);
}

.nav-search-wrap {
  flex: 0 1 200px;
  min-width: 140px;
  margin: 0 var(--space-2);
}

.nav-naive-menu--desktop {
  flex: 1 1 auto;
  min-width: 0;
}

.nav-naive-menu--desktop :deep(.n-menu--horizontal) {
  width: 100%;
}

.nav-naive-menu--desktop :deep(.n-menu-item-content) {
  padding: 0 var(--space-3);
  position: relative;
}

.nav-naive-menu--desktop :deep(.n-menu-item-content::after) {
  content: '';
  position: absolute;
  left: var(--space-3);
  right: var(--space-3);
  bottom: 4px;
  height: 2px;
  background: var(--color-primary);
  border-radius: 1px;
  transform: scaleX(0);
  transition: transform var(--transition-smooth);
}

.nav-naive-menu--desktop :deep(.n-menu-item-content:hover::after),
.nav-naive-menu--desktop :deep(.n-menu-item-content--selected::after) {
  transform: scaleX(1);
}

.nav-naive-menu--desktop :deep(.n-menu-item-content-header) {
  overflow: visible;
  text-overflow: clip;
  white-space: nowrap;
}

.nav-naive-menu--mobile {
  width: 100%;
}

.nav-naive-menu--mobile :deep(.n-menu-item) {
  border-bottom: 1px solid var(--color-border);
}

.nav-naive-menu--mobile :deep(.n-menu-item-content) {
  padding: var(--space-2) var(--space-4);
  min-height: 44px;
}

.nav-naive-menu--mobile :deep(.n-menu-item:last-child) {
  border-bottom: none;
}

.nav-auth-actions {
  display: none;
}

.nav-auth-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  text-decoration: none;
  transition: background var(--transition-fast), border-color var(--transition-fast), color var(--transition-fast);
}

.nav-auth-btn--primary {
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  color: #fff;
}

.nav-auth-btn--primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  color: #fff;
}

.nav-auth-btn--outline {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text);
}

.nav-auth-btn--outline:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

@media (min-width: 1024px) {
  .nav-naive-menu--mobile {
    display: none;
  }
}

@media (max-width: 1023px) {
  .nav-naive-menu--desktop {
    display: none;
  }

  .nav-auth-actions {
    display: flex;
    gap: var(--space-3);
    padding: 0 var(--space-4) var(--space-3);
    flex-shrink: 0;
  }

  .nav-naive-menu--mobile {
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
  }

  .nav-search-wrap {
    flex: none;
    width: 100%;
    min-width: 0;
    margin: 0;
    padding: var(--space-3) var(--space-4);
    border-top: 1px solid var(--color-border);
    flex-shrink: 0;
  }

  .nav-write-btn,
  .nav-notif-wrap,
  .nav-user-wrap {
    flex-shrink: 0;
    padding: 0 var(--space-4) var(--space-2);
  }
}

.nav-notif-wrap {
  display: flex;
  align-items: center;
}

.nav-notif-wrap :deep(.n-badge) {
  display: inline-flex;
}

.nav-bell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
  margin: 0;
  border-radius: var(--radius-pill);
  color: var(--color-text-muted);
  text-decoration: none;
  line-height: 0;
  transition: color var(--transition-fast), background var(--transition-fast);
}

.nav-bell:hover {
  color: var(--color-primary);
  background: var(--surface-muted);
}

.nav-user-wrap {
  position: relative;
}

.nav-user-dropdown-wrap {
  position: relative;
}

.nav-user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: var(--radius-pill);
  padding: var(--space-1) var(--space-2) var(--space-1) var(--space-1);
  cursor: pointer;
  font-family: inherit;
  color: var(--color-text);
}

.nav-user-trigger:hover {
  border-color: var(--color-text-muted);
}

.nav-avatar {
  flex-shrink: 0;
}

.nav-username-short {
  max-width: 6.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
}

@media (max-width: 1023px) {
  .nav-admin-li-desktop {
    display: none;
  }
}

.nav-admin-li-desktop {
  flex-shrink: 0;
}

.nav-links a.nav-admin {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: var(--space-2) var(--space-3);
  background: var(--color-text);
  color: #fff;
  text-decoration: none;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  line-height: 1;
  white-space: nowrap;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.nav-links a.nav-admin:hover {
  background: var(--color-admin-hover);
  color: #fff;
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
    position: fixed;
    top: var(--layout-navbar-bottom);
    left: 0;
    right: 0;
    bottom: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    padding: var(--space-3) 0 var(--space-4);
    background: var(--color-surface);
    border-bottom: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    max-height: none;
    overflow: hidden;
    overscroll-behavior: contain;
    -webkit-overflow-scrolling: touch;
    touch-action: pan-y;
    transform-origin: top;
    transform: translateY(-8px);
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transition: transform 0.32s var(--ease-out-soft), opacity 0.28s var(--ease-out-soft),
      visibility 0.32s;
    z-index: var(--z-nav-menu);
  }

  .nav-links.open {
    transform: translateY(0);
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }

  .nav-user-trigger {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 767px) {
  .nav-links {
    bottom: calc(var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
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
