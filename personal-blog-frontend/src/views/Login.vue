<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true" />
    <div class="container login-wrap">
      <n-card class="login-card" :bordered="true">
        <template #header>
          <div class="card-header-inner">
            <h1 class="card-title">登录</h1>
            <!-- <p class="card-hint">管理员与普通用户统一登录</p> -->
          </div>
        </template>
        <n-form class="login-form" @submit.prevent="handleLogin">
          <n-form-item label="用户名" label-placement="top">
            <n-input v-model:value="username" autocomplete="username" />
          </n-form-item>
          <n-form-item label="密码" label-placement="top">
            <n-input v-model:value="password" type="password" show-password-on="click"
              autocomplete="current-password" />
          </n-form-item>
          <n-form-item label="验证码" label-placement="top">
            <div class="captcha-line">
              <n-input v-model:value="captchaCode" autocomplete="off" maxlength="8" placeholder="右侧图形中的字符" />
              <n-button tertiary type="default" @click.prevent="loadCaptcha">
                <n-image v-if="captchaSrc" :src="captchaSrc" alt="验证码" width="120" height="40" preview-disabled
                  object-fit="cover" />
                <span v-else class="captcha-placeholder">加载中…</span>
                <template #icon>
                  <n-icon><reload-outline /></n-icon>
                </template>
              </n-button>
            </div>
          </n-form-item>
          <div class="remember-row">
            <n-checkbox v-model:checked="rememberMe">记住我（延长登录有效期）</n-checkbox>
          </div>
          <n-form-item :show-label="false" :show-feedback="false">
            <n-button attr-type="submit" type="primary" block :loading="isLoading">{{ isLoading ? '登录中…' : '登录'
            }}</n-button>
          </n-form-item>
          <n-form-item :show-label="false" :show-feedback="false">
            <n-button type="default" block @click.prevent="loginWithGithub">使用 GitHub 登录</n-button>
          </n-form-item>
          <p class="switch-link">没有账号？<router-link to="/register">去注册</router-link></p>
          <n-alert v-if="success" type="success" class="form-alert-tight">{{ success }}</n-alert>
          <n-alert v-if="error" :key="errorTick" type="error" class="form-alert-tight">{{ error }}</n-alert>
        </n-form>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ReloadOutline } from '@vicons/ionicons5';
import {
  NAlert,
  NButton,
  NCard,
  NCheckbox,
  NForm,
  NFormItem,
  NIcon,
  NImage,
  NInput,
} from 'naive-ui';
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { login, fetchCaptcha } from '../api/auth';
import { githubLoginUrl } from '../api/oauth';

const route = useRoute();
const username = ref('');
const password = ref('');
const captchaKey = ref('');
const captchaCode = ref('');
const captchaSrc = ref('');
const rememberMe = ref(false);
const showPwd = ref(false);
const isLoading = ref(false);
const success = ref(null);
const error = ref(null);
const errorTick = ref(0);
const router = useRouter();
const authStore = useAuthStore();

async function loadCaptcha() {
  captchaCode.value = '';
  try {
    const d = await fetchCaptcha();
    captchaKey.value = d?.captchaKey || '';
    const raw = d?.imageBase64 || '';
    captchaSrc.value = raw.startsWith('data:') ? raw : `data:image/png;base64,${raw}`;
  } catch {
    captchaSrc.value = '';
    captchaKey.value = '';
    error.value = '验证码加载失败';
    errorTick.value += 1;
  }
}

onMounted(async () => {
  if (authStore.isLoggedIn) {
    const redirect = route.query.redirect;
    if (typeof redirect === 'string' && redirect.startsWith('/')) {
      router.replace(redirect);
    } else if (authStore.isAdmin) {
      router.replace({ name: 'AdminDashboard' });
    } else {
      router.replace({ name: 'Home' });
    }
    return;
  }
  if (route.query.registered === '1') {
    success.value = '注册成功，请登录';
  }
  await loadCaptcha();
});

function resolveRedirect() {
  const redirect = route.query.redirect;
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect;
  }
  return authStore.isAdmin ? '/admin' : '/';
}

function loginWithGithub() {
  window.location.href = githubLoginUrl();
}

const handleLogin = async () => {
  isLoading.value = true;
  success.value = null;
  error.value = null;
  try {
    const res = await login({
      username: username.value,
      password: password.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value,
      rememberMe: rememberMe.value,
    });
    const data = res?.data;
    const token = data?.token;
    const role = data?.role;
    if (token) {
      authStore.loginSuccess(token, role);
      try {
        await authStore.fetchMe();
      } catch {
        /* ignore */
      }
      router.push(resolveRedirect());
      return;
    }
    error.value = '登录失败：未获取到有效的 Token。';
    errorTick.value += 1;
    await loadCaptcha();
  } catch (err) {
    let msg = err?.message || '登录失败';
    const ra = err?.payload?.remainingAttempts;
    if (ra != null && Number.isFinite(Number(ra))) {
      msg += `（还剩 ${ra} 次尝试机会）`;
    }
    error.value = msg;
    errorTick.value += 1;
    await loadCaptcha();
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--layout-main-pad-top) - 140px);
  padding: var(--space-10) 0;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 55% at 20% 30%, rgba(37, 99, 235, 0.14), transparent),
    radial-gradient(ellipse 60% 45% at 85% 65%, rgba(63, 63, 70, 0.08), transparent);
  pointer-events: none;
}

.login-wrap {
  position: relative;
  z-index: var(--space-1);
}

.login-card {
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  text-align: center;
}

.card-header-inner {
  text-align: center;
}

.card-title {
  margin: 0;
  font-size: var(--text-2xl);
  font-weight: var(--weight-semibold);
  letter-spacing: -0.03em;
  color: var(--color-text);
}

.card-hint {
  margin: var(--space-2) 0 0;
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.login-form {
  text-align: left;
}

.captcha-line {
  display: flex;
  gap: var(--space-2);
  align-items: stretch;
}

.captcha-line :deep(.n-input) {
  flex: 1;
  min-width: 0;
}

.captcha-placeholder {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.remember-row {
  margin: calc(var(--space-2) * -1) 0 var(--space-5);
}

.form-alert-tight {
  margin-top: var(--space-5);
}

.switch-link {
  margin-top: var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  text-align: center;
}

.switch-link a {
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
}
</style>
