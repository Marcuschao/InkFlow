<template>
  <header
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
          <span class="bar"></span>
          <span class="bar"></span>
          <span class="bar"></span>
        </button>
        <div id="primary-nav" class="nav-links" :class="{ open: isMenuOpen }">
          <n-menu
            class="nav-naive-menu nav-naive-menu--desktop"
            mode="horizontal"
            :options="navMenuOptions"
            :value="navMenuActiveKey"
            accordion
            @update:value="onNavMenuUpdate"
          />
          <n-menu
            class="nav-naive-menu nav-naive-menu--mobile"
            mode="vertical"
            :options="navMenuOptions"
            :value="navMenuActiveKey"
            accordion
            @update:value="onNavMenuUpdate"
          />

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
            <div ref="navUserWrapRef" class="nav-user-dropdown-wrap">
              <n-dropdown
                trigger="click"
                placement="bottom-end"
                :options="userDropdownOptions"
                :show-arrow="false"
                :style="{ minWidth: '10rem' }"
                @select="onUserDropdownSelect"
                @clickoutside="closeUserMenu"
              >
                <button
                  type="button"
                  class="nav-user-trigger"
                  aria-haspopup="menu"
                  :aria-expanded="userMenuOpen"
                  @click.stop="toggleUserMenu"
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
      />
    </transition>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NMenu, NBadge, NDropdown, NSwitch, NIcon } from 'naive-ui';
import { NotificationsOutline, Moon, SunnyOutline } from '@vicons/ionicons5';
import { useAuthStore } from '../stores/auth';
import { useNotificationStore } from '../stores/notification';
import { useSiteStore } from '../stores/site';
import UserAvatar from './UserAvatar.vue';

const dark = defineModel('dark', { type: Boolean, default: false });

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const siteStore = useSiteStore();
const isMenuOpen = ref(false);
const isScrolled = ref(false);
const hideNav = ref(false);
const userMenuOpen = ref(false);
const navUserWrapRef = ref(null);
let lastY = 0;

const unreadCount = computed(() => notificationStore.unreadCount);

const STATIC_NAV_KEYS = [
  '/',
  '/archive',
  '/tags',
  '/search',
  '/links',
  '/diary',
  '/reading-history',
  '/chat',
];

const navMenuActiveKey = computed(() => {
  const p = route.path;
  if (STATIC_NAV_KEYS.includes(p)) return p;
  if (!authStore.isLoggedIn && (p === '/login' || p === '/register')) return p;
  return null;
});

const navMenuOptions = computed(() => {
  const base = [
    { label: '首页', key: '/' },
    { label: '归档', key: '/archive' },
    { label: '标签', key: '/tags' },
    { label: '搜索', key: '/search' },
    { label: '友链', key: '/links' },
    { label: '日记', key: '/diary' },
    { label: '聊天室', key: '/chat' },
    { label: '阅读记录', key: '/reading-history' },
  ];
  if (!authStore.isLoggedIn) {
    base.push({ label: '登录', key: '/login' });
    base.push({ label: '注册', key: '/register' });
  }
  return base;
});

const userDropdownOptions = computed(() => {
  const opts = [{ label: '个人主页', key: 'profile' }];
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

const toggleUserMenu = () => {
  userMenuOpen.value = !userMenuOpen.value;
};

const closeUserMenu = () => {
  userMenuOpen.value = false;
};

const onDocClick = (e) => {
  const el = navUserWrapRef.value;
  if (el && !el.contains(e.target)) {
    userMenuOpen.value = false;
  }
};

function onUserDropdownSelect(key) {
  if (key === 'profile') router.push('/user/me');
  else if (key === 'admin') router.push('/admin');
  else if (key === 'logout') handleLogoutFromMenu();
}

const handleLogoutFromMenu = () => {
  authStore.logout();
  userMenuOpen.value = false;
  closeMenu();
  if (route.path.startsWith('/admin')) {
    router.push({ name: 'Home' });
  }
};

const closeMenu = () => {
  isMenuOpen.value = false;
  userMenuOpen.value = false;
};

const toggleMenu = () => {
  userMenuOpen.value = false;
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
  hideNav.value = y > lastY && y > 96;
  lastY = y;
};

onMounted(() => {
  lastY = window.scrollY || 0;
  window.addEventListener('scroll', onScroll, { passive: true });
  document.addEventListener('click', onDocClick);
  if (authStore.isLoggedIn) refreshUnread();
});

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
    userMenuOpen.value = false;
  }
);

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll);
  document.removeEventListener('click', onDocClick);
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

.nav-naive-menu--desktop {
  flex: 1;
}

.nav-naive-menu--desktop :deep(.n-menu-bar) {
  width: fit-content;
  align-items: flex-end;
  margin-left: auto;
}

.nav-naive-menu--mobile {
  width: 100%;
}

.nav-naive-menu--mobile :deep(.n-menu-item) {
  border-bottom: 1px solid var(--color-border);
}

.nav-naive-menu--mobile :deep(.n-menu-item:last-child) {
  border-bottom: none;
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

.nav-dark-wrap {
  display: flex;
  align-items: center;
}

.nav-dark-switch {
  display: flex;
  align-items: center;
}

@media (max-width: 1023px) {
  .nav-admin-li-desktop {
    display: none;
  }
}

.nav-links a.nav-admin {
  display: block;
  padding: 0.5rem 0.85rem;
  background: var(--color-text);
  color: #fff;
  text-decoration: none;
  font-size: 0.92rem;
  font-weight: 500;
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

  .nav-user-trigger {
    width: 100%;
    justify-content: flex-start;
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
