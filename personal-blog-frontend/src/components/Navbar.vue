<template>
  <header
    ref="navbarRef"
    class="navbar"
    :class="{ scrolled: isScrolled, 'nav-hidden': hideNav, 'navbar-menu-open': isMenuOpen }"
  >
    <nav class="nav-inner" aria-label="主导航">
      <div class="container nav-row">
        <router-link to="/" class="logo" @click="closeMenu">
          <img :src="brandIcon" alt="" width="30" height="30" />
          <span>{{ siteStore.siteTitle }}</span>
        </router-link>
        <div id="primary-nav" class="nav-links" :class="{ open: isMenuOpen }">
          <div v-if="!authStore.isLoggedIn && isMobileNav && isMenuOpen" class="nav-auth-actions">
            <router-link to="/login" class="nav-auth-btn nav-auth-btn--primary" @click="closeMenu">登录</router-link>
            <router-link to="/register" class="nav-auth-btn nav-auth-btn--outline" @click="closeMenu">注册</router-link>
          </div>

          <div v-if="!isMobileNav" class="nav-primary-group">
            <n-menu
              class="nav-naive-menu nav-naive-menu--desktop"
              mode="horizontal"
              :options="navMenuOptions"
              :value="navMenuActiveKey"
              accordion
              @update:value="onNavMenuUpdate"
            />
            <n-dropdown
              v-if="overflowNavOptions.length"
              trigger="click"
              placement="bottom-start"
              :options="overflowNavOptions"
              :show-arrow="false"
              @select="onNavMenuUpdate"
            >
              <button type="button" class="nav-more-trigger" aria-label="更多导航">
                <n-icon :component="EllipsisHorizontalOutline" :size="17" />
                <span>更多</span>
              </button>
            </n-dropdown>
          </div>
          <n-menu
            v-else
            class="nav-naive-menu nav-naive-menu--mobile"
            mode="vertical"
            :options="mobileNavMenuOptions"
            :value="navMenuActiveKey"
            accordion
            @update:value="onNavMenuUpdate"
          />

          <div class="nav-actions" :class="{ 'nav-actions--logged-in': authStore.isLoggedIn }">
            <div class="nav-search-shell" :class="{ 'is-expanded': isSearchExpanded }">
              <button
                type="button"
                class="nav-search-compact-toggle"
                aria-label="展开文章搜索"
                :aria-expanded="isSearchExpanded"
                @click="isSearchExpanded = !isSearchExpanded"
              >
                <n-icon :component="SearchOutline" :size="19" />
              </button>
              <div class="nav-search-wrap">
                <SearchSuggest />
              </div>
            </div>

            <button
              type="button"
              class="nav-theme-toggle"
              :aria-label="isDark ? '切换亮色模式' : '切换暗色模式'"
              @click="toggleDark()"
            >
              <n-icon :class="{ 'theme-icon--spin': true }" :component="isDark ? SunnyOutline : MoonOutline" :size="20" />
            </button>

            <router-link
              v-if="authStore.isLoggedIn"
              to="/write"
              class="nav-write-btn"
              @click="closeMenu"
            >
              <n-icon :component="CreateOutline" :size="16" />
              <span>写文章</span>
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

            <div v-if="authStore.isAdmin && !route.path.startsWith('/admin')" class="nav-admin-li-desktop">
              <router-link to="/admin" class="nav-admin" @click="closeMenu">管理</router-link>
            </div>
          </div>

        </div>
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
import { h, ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NMenu, NBadge, NDropdown, NIcon } from 'naive-ui';
import {
  ArchiveOutline,
  BagHandleOutline,
  ChatbubblesOutline,
  CloseOutline,
  CreateOutline,
  EllipsisHorizontalOutline,
  FlameOutline,
  HomeOutline,
  LinkOutline,
  MenuOutline,
  MoonOutline,
  NotificationsOutline,
  PricetagsOutline,
  SearchOutline,
  ShareSocialOutline,
  SparklesOutline,
  SunnyOutline,
  TimeOutline,
} from '@vicons/ionicons5';
import { useAuthStore } from '../stores/auth';
import { useNotificationStore } from '../stores/notification';
import { useSiteStore } from '../stores/site';
import { useTheme } from '../composables/useTheme';
import { useAiChatVisibility } from '../composables/useAiChatVisibility';
import UserAvatar from './UserAvatar.vue';
import SearchSuggest from './SearchSuggest.vue';

const route = useRoute();
const brandIcon = `${import.meta.env.BASE_URL}favicon.svg`;
const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const siteStore = useSiteStore();
const { isDark, toggleDark } = useTheme();
const { visible: aiChatVisible } = useAiChatVisibility();
const isMenuOpen = ref(false);
const isMobileNav = ref(
  typeof window !== 'undefined' && window.matchMedia('(max-width: 1023px)').matches
);
const isScrolled = ref(false);
const hideNav = ref(false);
const isSearchExpanded = ref(false);
const navbarRef = ref(null);
let lastY = 0;
let navResizeObserver = null;

function syncNavLayoutOffset() {
  const el = navbarRef.value?.querySelector('.nav-inner') || navbarRef.value;
  if (!el) return;
  const bottom = Math.ceil(el.getBoundingClientRect().bottom);
  document.documentElement.style.setProperty('--layout-navbar-bottom', `${bottom}px`);
  if (window.matchMedia('(min-width: 1024px)').matches) {
    const gap = 24;
    document.documentElement.style.setProperty('--layout-main-pad-top', `${bottom + gap}px`);
  } else {
    document.documentElement.style.removeProperty('--layout-main-pad-top');
  }
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
  '/ai-chat',
  '/links',
  '/share',
  '/reading-history',
  '/chat',
  '/shop',
];

const navMenuActiveKey = computed(() => {
  const p = route.path;
  if (STATIC_NAV_KEYS.includes(p)) return p;
  if (p === '/user/me') return '/user/me';
  if (!authStore.isLoggedIn && (p === '/login' || p === '/register')) return p;
  return null;
});

const menuIcon = (component) => () => h(NIcon, { component, size: 15 });

const MAIN_NAV_OPTIONS = [
  { label: '首页', key: '/', icon: menuIcon(HomeOutline) },
  { label: '归档', key: '/archive', icon: menuIcon(ArchiveOutline) },
  { label: '知识星系', key: '/tags', icon: menuIcon(PricetagsOutline) },
  { label: 'AI 问答', key: '/ai-chat', icon: menuIcon(SparklesOutline) },
  { label: '聊天室', key: '/chat', icon: menuIcon(ChatbubblesOutline) },
  { label: '热搜', key: '/hot-search', icon: menuIcon(FlameOutline) },
  { label: '友链', key: '/links', icon: menuIcon(LinkOutline) },
  { label: '分享', key: '/share', icon: menuIcon(ShareSocialOutline) },
  { label: '商城', key: '/shop', icon: menuIcon(BagHandleOutline) },
  { label: '阅读记录', key: '/reading-history', icon: menuIcon(TimeOutline) },
];

const LOGGED_IN_OVERFLOW_KEYS = ['/links', '/share', '/shop', '/reading-history'];

const navMenuOptions = computed(() => {
  let base = MAIN_NAV_OPTIONS.filter((opt) => opt.key !== '/ai-chat' || aiChatVisible.value);
  base = base.filter((opt) => !LOGGED_IN_OVERFLOW_KEYS.includes(opt.key));
  if (!authStore.isLoggedIn) {
    base.push({ label: '登录', key: '/login' });
    base.push({ label: '注册', key: '/register' });
  }
  return base;
});

const overflowNavOptions = computed(() =>
  MAIN_NAV_OPTIONS.filter(
    (opt) => LOGGED_IN_OVERFLOW_KEYS.includes(opt.key) && (opt.key !== '/ai-chat' || aiChatVisible.value)
  )
);

const mobileNavMenuOptions = computed(() => {
  const base = MAIN_NAV_OPTIONS.filter((opt) => opt.key !== '/ai-chat' || aiChatVisible.value);
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
  if (typeof key !== 'string' || key === 'more') return;
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
  } else if (delta > 16) {
    hideNav.value = true;
  } else if (delta < -16) {
    hideNav.value = false;
  }
  if (Math.abs(delta) >= 16) {
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
    isSearchExpanded.value = false;
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
  box-shadow: var(--shadow-nav), var(--shadow-sm);
}

.nav-inner {
  position: relative;
  min-height: var(--nav-height);
  display: flex;
  align-items: center;
  background: color-mix(in srgb, var(--color-page) 96%, transparent);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(10px);
  box-shadow: none;
  transition: box-shadow var(--transition-fast);
}

.nav-inner::after {
  display: none;
}

.nav-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: var(--space-2);
}

@media (min-width: 1024px) {
  .nav-row.container {
    max-width: 1520px;
    padding-left: 24px;
    padding-right: 24px;
  }

  .nav-links {
    flex: 1;
    min-width: 0;
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: var(--space-2);
  }
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 1.08rem;
  letter-spacing: 0;
  text-decoration: none;
  color: var(--color-text);
  transition: color var(--transition-fast);
}

.logo img {
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  border-radius: 3px;
}

.logo:hover {
  color: var(--color-text);
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
  padding: var(--space-2) var(--space-4);
  gap: 7px;
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  background: var(--color-primary);
  color: var(--color-on-primary);
  border: 1px solid transparent;
  box-shadow: none;
  text-decoration: none;
  white-space: nowrap;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.nav-write-btn:hover {
  transform: translateY(-1px);
  box-shadow: none;
  color: var(--color-on-primary);
}

.nav-theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: 50%;
  background: transparent;
  border-color: transparent;
  color: var(--color-text);
  cursor: pointer;
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.nav-theme-toggle:hover {
  transform: translateY(-1px);
  background: var(--surface-primary-tint);
  box-shadow: var(--shadow-sm);
}

.nav-theme-toggle:active {
  transform: translate(4px, 4px);
  box-shadow: none;
}

.theme-icon--spin {
  transition: transform 0.35s var(--ease-out-soft);
}

.nav-theme-toggle:hover .theme-icon--spin {
  transform: rotate(180deg);
}

.nav-links {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: var(--space-2);
}

@media (min-width: 1024px) {
  .nav-links {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    width: 100%;
  }
}

.nav-links :deep(.n-menu) {
  background: transparent;
}

.nav-links :deep(.n-menu-item-content) {
  font: 500 11px/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.nav-links :deep(.n-menu-item-content--selected) {
  font-weight: var(--weight-semibold);
}

.nav-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--space-2);
  margin-left: 0;
  min-width: 0;
}

.nav-primary-group {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 2px;
  overflow: hidden;
}

.nav-more-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 66px;
  height: 2.5rem;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--color-text-muted);
  font: 500 11px/1 var(--font-mono);
  letter-spacing: .06em;
  white-space: nowrap;
  cursor: pointer;
  flex: 0 0 auto;
  transition: color var(--transition-fast), background var(--transition-fast), border-color var(--transition-fast);
}

.nav-more-trigger:hover,
.nav-more-trigger:focus-visible {
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-color: color-mix(in srgb, var(--color-primary) 28%, transparent);
}

.nav-search-wrap {
  flex: 0 1 200px;
  min-width: 140px;
}

.nav-search-shell {
  position: relative;
  flex: 0 1 200px;
  min-width: 140px;
}

.nav-search-compact-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text);
  cursor: pointer;
}

.nav-search-compact-toggle:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.nav-search-wrap :deep(.n-input) {
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--color-surface) 64%, transparent);
  overflow: visible;
}

.nav-search-wrap :deep(.n-input__border),
.nav-search-wrap :deep(.n-input__state-border) {
  border-radius: var(--radius-pill);
}

.nav-search-wrap :deep(input:focus-visible) {
  outline: none !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

.nav-search-wrap :deep(.n-input.n-input--focus) {
  border-radius: var(--radius-pill) !important;
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-primary) 20%, transparent) !important;
}

.nav-search-wrap :deep(.n-input.n-input--focus .n-input__border),
.nav-search-wrap :deep(.n-input.n-input--focus .n-input__state-border) {
  border-radius: var(--radius-pill) !important;
  box-shadow: none !important;
}

.nav-actions--logged-in .nav-search-wrap {
  flex: 0 1 160px;
  min-width: 120px;
}

@media (min-width: 1024px) and (max-width: 1240px) {
  .nav-search-shell {
    flex: 0 0 2.5rem;
    min-width: 2.5rem;
  }

  .nav-search-compact-toggle {
    display: inline-flex;
  }

  .nav-search-shell .nav-search-wrap {
    position: absolute;
    top: calc(100% + 10px);
    right: 0;
    width: 260px;
    min-width: 260px;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transform: translateY(-5px);
    transition: opacity var(--transition-fast), transform var(--transition-fast), visibility var(--transition-fast);
  }

  .nav-search-shell.is-expanded .nav-search-wrap {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
    transform: translateY(0);
  }
}

.nav-naive-menu--desktop {
  flex: 0 1 auto;
  min-width: 0;
  width: auto;
  overflow: hidden;
}

.nav-naive-menu--desktop :deep(.n-menu--horizontal) {
  width: auto;
}

.nav-naive-menu--desktop :deep(.n-menu-item-content) {
  padding: 0 var(--space-2);
  position: relative;
}

.nav-naive-menu--desktop :deep(.n-menu-item-content::after) {
  display: block;
  content: '';
  position: absolute;
  left: var(--space-2);
  right: var(--space-2);
  bottom: 2px;
  height: 1px;
  background: var(--color-primary);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--transition-fast);
}

.nav-naive-menu--desktop :deep(.n-menu-item-content--selected) {
  background: transparent !important;
  color: var(--color-primary) !important;
  border-radius: 0 !important;
  font-weight: 500 !important;
}

html:not(.dark) .nav-naive-menu--desktop :deep(.n-menu-item-content--selected) {
  background: transparent !important;
  color: var(--color-primary) !important;
}

html.dark .nav-naive-menu--desktop :deep(.n-menu-item-content--selected) {
  background: transparent !important;
  color: var(--color-primary) !important;
}

.nav-naive-menu--desktop :deep(.n-menu-item-content:hover) {
  color: var(--color-text) !important;
}

.nav-naive-menu--desktop :deep(.n-menu-item-content:hover::after),
.nav-naive-menu--desktop :deep(.n-menu-item-content--selected::after) {
  display: block;
  height: 2px;
  bottom: 2px;
  background: var(--color-primary);
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
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  text-decoration: none;
  border: var(--border-brutal);
  box-shadow: var(--shadow-brutal-sm);
  transition:
    transform var(--transition-fast),
    box-shadow var(--transition-fast);
}

.nav-auth-btn--primary {
  background: var(--color-accent);
  color: var(--color-on-primary);
}

.nav-auth-btn--primary:hover {
  transform: translate(2px, 2px);
  box-shadow: none;
  color: var(--color-on-primary);
}

.nav-auth-btn--outline {
  background: var(--color-surface);
  color: var(--color-text);
}

.nav-auth-btn--outline:hover {
  transform: translate(2px, 2px);
  box-shadow: none;
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

  .nav-actions {
    flex-direction: column;
    align-items: stretch;
    width: 100%;
    margin-left: 0;
  }

  .nav-search-shell {
    flex: 0 0 auto;
    width: 100%;
    min-width: 0;
  }

  .nav-search-wrap {
    flex: none;
    width: 100%;
    min-width: 0;
    padding: var(--space-3) var(--space-4);
    border-top: 1px solid var(--color-border);
    flex-shrink: 0;
  }

  .nav-theme-toggle {
    flex-shrink: 0;
    margin: var(--space-1) var(--space-4) 0;
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

.nav-notif-wrap :deep(.n-badge-sup) {
  background-color: #ef4444 !important;
  box-shadow: 0 0 0 1px #ffffff;
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

.nav-actions--logged-in .nav-user-trigger {
  width: 44px;
  height: 44px;
  padding: 4px;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
}

.nav-user-trigger:hover {
  border-color: var(--color-text-muted);
}

.nav-avatar {
  flex-shrink: 0;
  display: inline-grid;
  place-items: center;
}

.nav-avatar :deep(.n-avatar),
.nav-avatar :deep(.n-avatar img) {
  display: block;
  object-fit: cover;
  object-position: center;
}

.nav-username-short {
  max-width: 6.5rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
}

@media (min-width: 1024px) {
  .nav-actions--logged-in .nav-username-short {
    display: none;
  }
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
  border-radius: var(--radius-pill);
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
    position: relative;
    z-index: calc(var(--z-nav-menu) + 2);
    touch-action: manipulation;
  }

  .navbar.nav-hidden:not(.navbar-menu-open) {
    transform: none;
  }

  .nav-links {
    position: fixed;
    top: var(--layout-navbar-bottom);
    left: 0;
    right: 0;
    bottom: auto;
    height: calc(
      100dvh - var(--layout-navbar-bottom) - var(--mobile-dock-height) -
        env(safe-area-inset-bottom, 0px)
    );
    min-height: 0;
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

  .nav-links:not(.open),
  .nav-links:not(.open) * {
    pointer-events: none !important;
  }

  .nav-links.open {
    transform: translateY(0);
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }

  .nav-links.open * {
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
