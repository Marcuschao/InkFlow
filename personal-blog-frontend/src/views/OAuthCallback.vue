<template>
  <div class="oauth-callback-page ds-page">
    <div class="container">
      <n-card>
        <n-spin v-if="loading" size="large" />
        <n-alert v-else-if="error" type="error">{{ error }}</n-alert>
        <p v-else class="muted">登录成功，正在跳转…</p>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NAlert, NCard, NSpin } from 'naive-ui';
import { useAuthStore } from '../stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const loading = ref(true);
const error = ref('');

onMounted(async () => {
  const err = route.query.error;
  if (err) {
    error.value = typeof err === 'string' ? err : 'GitHub 登录失败';
    loading.value = false;
    return;
  }
  const token = route.query.token;
  const role = route.query.role;
  if (!token || typeof token !== 'string') {
    error.value = '未收到登录凭证';
    loading.value = false;
    return;
  }
  authStore.loginSuccess(token, typeof role === 'string' ? role : 'USER');
  try {
    await authStore.fetchMe();
  } catch {
    /* ignore */
  }
  const redirect = route.query.redirect;
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    router.replace(redirect);
  } else if (authStore.isAdmin) {
    router.replace({ name: 'AdminDashboard' });
  } else {
    router.replace({ name: 'Home' });
  }
});
</script>

<style scoped>
.oauth-callback-page {
  padding: var(--space-16) 0;
}
</style>
