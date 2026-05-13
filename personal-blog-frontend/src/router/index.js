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
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/admin/translations',
    name: 'AdminTranslations',
    component: () => import('../views/Admin/AdminTranslations.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/freshness',
    name: 'AdminFreshness',
    component: () => import('../views/Admin/AdminFreshness.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/dashboard',
    name: 'AdminAnalytics',
    component: () => import('../views/Admin/StatsDashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/Admin/Dashboard.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/new',
    name: 'ArticleNew',
    component: () => import('../views/Admin/ArticleEditor.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/edit/:id',
    name: 'ArticleEdit',
    component: () => import('../views/Admin/ArticleEditor.vue'),
    props: true,
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/ai-weekly',
    name: 'AiWeekly',
    component: () => import('../views/Admin/AiWeekly.vue'),
    meta: { requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'Login' };
  }
});

export default router;
