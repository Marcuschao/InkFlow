import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('../views/ArticleDetail.vue'),
    props: true,
  },
  {
    path: '/archive',
    name: 'Archive',
    component: () => import('../views/Archive.vue'),
  },
  {
    path: '/tags',
    name: 'Tags',
    component: () => import('../views/Tags.vue'),
  },
  {
    path: '/discover/learning-path',
    name: 'LearningPath',
    component: () => import('../views/LearningPath.vue'),
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('../views/About.vue'),
  },
  {
    path: '/reading-history',
    name: 'ReadingHistory',
    component: () => import('../views/ReadingHistory.vue'),
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/Search.vue'),
  },
  {
    path: '/hot-search',
    name: 'HotSearch',
    component: () => import('../views/HotSearchPage.vue'),
  },
  {
    path: '/links',
    name: 'Links',
    component: () => import('../views/Links.vue'),
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue'),
  },
  {
    path: '/diary',
    redirect: '/share',
  },
  {
    path: '/diary/:id(\\d+)',
    redirect: (to) => `/share/${to.params.id}`,
  },
  {
    path: '/share',
    name: 'PublicShare',
    component: () => import('../views/PublicDiary.vue'),
  },
  {
    path: '/share/:id(\\d+)',
    name: 'PublicShareDetail',
    component: () => import('../views/PublicDiaryDetail.vue'),
    props: true,
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue'),
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('../views/ResetPassword.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/oauth/callback',
    name: 'OAuthCallback',
    component: () => import('../views/OAuthCallback.vue'),
  },
  {
    path: '/badges',
    name: 'Badges',
    component: () => import('../views/Badges.vue'),
  },
  {
    path: '/user/me',
    name: 'UserProfile',
    component: () => import('../views/UserProfile.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/Notifications.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/write',
    name: 'WriteArticle',
    component: () => import('../views/Admin/ArticleEditor.vue'),
    meta: { requiresAuth: true, authorMode: true },
  },
  {
    path: '/write/edit/:id',
    name: 'WriteArticleEdit',
    component: () => import('../views/Admin/ArticleEditor.vue'),
    props: true,
    meta: { requiresAuth: true, authorMode: true },
  },
  {
    path: '/my-articles',
    name: 'MyArticles',
    component: () => import('../views/MyArticles.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/user/:id(\\d+)',
    name: 'UserPublic',
    component: () => import('../views/UserPublic.vue'),
    props: true,
  },
  {
    path: '/my-shares',
    name: 'MyShares',
    component: () => import('../views/Admin/DiaryList.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/share-write/edit/:id',
    name: 'ShareWriteEdit',
    component: () => import('../views/Admin/DiaryEditor.vue'),
    props: true,
    meta: { requiresAuth: true },
  },
  {
    path: '/my-shares/:id(\\d+)',
    name: 'MyShareDetail',
    component: () => import('../views/Admin/DiaryDetail.vue'),
    props: true,
    meta: { requiresAuth: true },
  },
  {
    path: '/share-write',
    name: 'ShareWrite',
    component: () => import('../views/Admin/DiaryEditor.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/diary/list',
    redirect: '/my-shares',
  },
  {
    path: '/admin/diary/edit/:id',
    redirect: (to) => `/share-write/edit/${to.params.id}`,
  },
  {
    path: '/admin/diary/:id(\\d+)',
    redirect: (to) => `/my-shares/${to.params.id}`,
  },
  {
    path: '/admin/diary',
    redirect: '/share-write',
  },
  {
    path: '/admin/translations',
    name: 'AdminTranslations',
    component: () => import('../views/Admin/AdminTranslations.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/freshness',
    name: 'AdminFreshness',
    component: () => import('../views/Admin/AdminFreshness.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/stream',
    name: 'AdminStream',
    component: () => import('../views/Admin/AdminStream.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/monitor',
    name: 'AdminMonitor',
    component: () => import('../views/Admin/AdminMonitor.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/push',
    name: 'AdminPush',
    component: () => import('../views/Admin/AdminPush.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/dashboard',
    name: 'AdminAnalytics',
    component: () => import('../views/Admin/StatsDashboard.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/logs',
    name: 'AdminAuditLogs',
    component: () => import('../views/Admin/AdminAuditLogs.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/reports/weekly',
    name: 'AdminReportsWeekly',
    component: () => import('../views/Admin/AdminReportsWeekly.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/reports/freshness',
    name: 'AdminReportsFreshness',
    component: () => import('../views/Admin/AdminReportsFreshness.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/backup',
    name: 'AdminBackup',
    component: () => import('../views/Admin/AdminBackup.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/settings',
    name: 'AdminSiteSettings',
    component: () => import('../views/Admin/AdminSiteSettings.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/links',
    name: 'AdminFriendLinks',
    component: () => import('../views/Admin/AdminFriendLinks.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/comments',
    name: 'AdminComments',
    component: () => import('../views/Admin/AdminComments.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/sensitive',
    name: 'AdminSensitive',
    component: () => import('../views/Admin/AdminSensitive.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/chat/messages',
    name: 'AdminChatManage',
    component: () => import('../views/Admin/AdminChatManage.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/chat/online',
    name: 'AdminChatOnline',
    component: () => import('../views/Admin/AdminChatOnline.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/articles/review',
    name: 'AdminArticleReview',
    component: () => import('../views/Admin/AdminArticleReview.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/content-reports',
    name: 'AdminContentReports',
    component: () => import('../views/Admin/AdminContentReports.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/Admin/Dashboard.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/new',
    name: 'ArticleNew',
    component: () => import('../views/Admin/ArticleEditor.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/edit/:id',
    name: 'ArticleEdit',
    component: () => import('../views/Admin/ArticleEditor.vue'),
    props: true,
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/ai-weekly',
    name: 'AiWeekly',
    component: () => import('../views/Admin/AiWeekly.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' };
    }
    if (to.path !== from.path) {
      return { top: 0, left: 0 };
    }
    return false;
  },
});

router.beforeEach(async (to) => {
  if (to.fullPath.length > 1 && to.fullPath.endsWith('/')) {
    return {
      path: to.fullPath.replace(/\/+$/, ''),
      query: to.query,
      hash: to.hash,
      replace: true,
    };
  }
  const authStore = useAuthStore();
  if (authStore.isLoggedIn && !authStore.user) {
    try {
      await authStore.fetchMe();
    } catch {
      authStore.logout();
    }
  }
  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      return { name: 'Login', query: { redirect: to.fullPath } };
    }
  }
  if (to.meta.requiresAdmin) {
    if (!authStore.isLoggedIn) {
      return { name: 'Login', query: { redirect: to.fullPath } };
    }
    if (!authStore.isAdmin) {
      return { name: 'Home' };
    }
  }
});

export default router;
