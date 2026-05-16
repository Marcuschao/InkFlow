<template>
  <div class="login-page">
    <div class="login-bg" aria-hidden="true" />
    <div class="container login-wrap">
      <div class="login-card">
        <div class="login-card-glow" aria-hidden="true" />
        <h1 class="card-title">管理员登录</h1>
        <p class="card-hint">请输入凭据与验证码</p>
        <form class="login-form" @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="ds-form-label" for="username">用户名</label>
            <input id="username" v-model="username" class="ds-input" type="text" required autocomplete="username" />
          </div>
          <div class="form-group password-row">
            <label class="ds-form-label" for="password">密码</label>
            <div class="password-wrap">
              <input
                id="password"
                v-model="password"
                class="ds-input"
                :type="showPwd ? 'text' : 'password'"
                required
                autocomplete="current-password"
              />
              <button type="button" class="pwd-toggle" tabindex="-1" @click="showPwd = !showPwd">
                {{ showPwd ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>
          <div class="form-group captcha-row">
            <label class="ds-form-label" for="captchaCode">验证码</label>
            <div class="captcha-line">
              <input
                id="captchaCode"
                v-model="captchaCode"
                class="ds-input"
                type="text"
                autocomplete="off"
                maxlength="8"
                placeholder="右侧图形中的字符"
                required
              />
              <button type="button" class="captcha-img-btn" title="点击刷新" @click="loadCaptcha">
                <img v-if="captchaSrc" :src="captchaSrc" alt="验证码" class="captcha-img" />
                <span v-else class="captcha-placeholder">加载中…</span>
              </button>
            </div>
          </div>
          <label class="remember-row">
            <input v-model="rememberMe" type="checkbox" />
            <span>记住我（延长登录有效期）</span>
          </label>
          <button type="submit" class="submit-button ds-btn ds-btn--primary" :disabled="isLoading">
            <span v-if="isLoading" class="ds-spin-lg" aria-hidden="true" />
            <span>{{ isLoading ? '登录中…' : '登录' }}</span>
          </button>
          <p v-if="error" :key="errorTick" class="error-message ds-error-box">{{ error }}</p>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { login, fetchCaptcha } from '../api/auth';

const username = ref('');
const password = ref('');
const captchaKey = ref('');
const captchaCode = ref('');
const captchaSrc = ref('');
const rememberMe = ref(false);
const showPwd = ref(false);
const isLoading = ref(false);
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
    router.replace({ name: 'AdminDashboard' });
    return;
  }
  await loadCaptcha();
});

const handleLogin = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const res = await login({
      username: username.value,
      password: password.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value,
      rememberMe: rememberMe.value,
    });
    const token = res?.data?.token;
    if (token) {
      authStore.setToken(token);
      router.push({ name: 'AdminDashboard' });
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
  padding: 2.5rem 0;
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
  z-index: 1;
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  margin: 0 auto;
  padding: 2.65rem 2.25rem 2.35rem;
  text-align: center;
  background: var(--color-surface-glass);
  backdrop-filter: blur(20px) saturate(165%);
  -webkit-backdrop-filter: blur(20px) saturate(165%);
  border-radius: var(--radius-xl);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md), 0 0 0 1px rgba(255, 255, 255, 0.5) inset;
}

.login-card-glow {
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  background: linear-gradient(
    135deg,
    rgba(37, 99, 235, 0.35),
    rgba(63, 63, 70, 0.12),
    transparent 55%
  );
  opacity: 0.45;
  z-index: -1;
  filter: blur(28px);
}

.card-title {
  margin: 0;
  font-size: var(--text-display-sm);
  font-weight: 720;
  letter-spacing: -0.03em;
  color: var(--color-text);
}

.card-hint {
  margin: var(--space-2) 0 var(--space-8);
  font-size: var(--text-base);
  color: var(--color-text-muted);
}

.login-form .form-group {
  margin-bottom: var(--space-5);
  text-align: left;
}

.login-form .password-wrap .ds-input {
  flex: 1;
  min-width: 0;
}

.login-form .ds-input {
  background: rgba(255, 255, 255, 0.9);
}

.password-wrap {
  display: flex;
  gap: var(--space-2);
  align-items: stretch;
}

.pwd-toggle {
  flex-shrink: 0;
  padding: 0 var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  cursor: pointer;
  color: var(--color-text-muted);
  font-family: inherit;
}

.captcha-line {
  display: flex;
  gap: var(--space-2);
  align-items: stretch;
}

.captcha-line .ds-input {
  flex: 1;
  min-width: 0;
}

.captcha-img-btn {
  flex-shrink: 0;
  padding: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  overflow: hidden;
  height: 2.85rem;
  width: 7.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.login-form .form-group:focus-within .ds-input {
  border-color: var(--border-focus-input);
  box-shadow: 0 0 0 4px var(--color-primary-soft);
}

.remember-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin: calc(var(--space-2) * -1) 0 var(--space-5);
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--color-text-muted);
  text-transform: none;
  cursor: pointer;
}

.remember-row input {
  width: auto;
  accent-color: var(--color-primary);
}

.submit-button {
  width: 100%;
  margin-top: var(--space-2);
  padding: 0.95rem var(--space-5);
  font-size: var(--text-lg);
}

.ds-error-box.error-message {
  margin-top: var(--space-5);
  animation: shake-soft 0.45s var(--ease-out-soft);
}

@media (prefers-reduced-motion: reduce) {
  .ds-error-box.error-message {
    animation: none;
  }
}
</style>
