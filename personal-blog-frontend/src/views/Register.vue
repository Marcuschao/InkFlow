<template>
  <div class="auth-page">
    <div class="auth-page-bg" aria-hidden="true" />
    <div class="login-wrap">
      <n-card class="auth-card ds-brutal-surface" :bordered="false">
        <template #header>
          <div class="card-header-inner">
            <h1 class="card-title">注册账号</h1>
            <p class="card-hint">创建账号后可登录发表评论</p>
          </div>
        </template>
        <n-form novalidate class="login-form" @submit.prevent="handleRegister">
          <n-form-item
            label="用户名"
            label-placement="top"
            :validation-status="showErr('username') ? 'error' : undefined"
            :feedback="showErr('username') ? fieldErrors.username : undefined"
          >
            <n-input
              v-model:value="username"
              autocomplete="username"
              @blur="touch('username')"
              @input="touch('username')"
            />
          </n-form-item>
          <n-form-item
            label="邮箱"
            label-placement="top"
            :validation-status="showErr('email') ? 'error' : undefined"
            :feedback="showErr('email') ? fieldErrors.email : undefined"
          >
            <n-input v-model:value="email" type="text" inputmode="email" autocomplete="email" @blur="touch('email')" @input="touch('email')" />
          </n-form-item>
          <n-form-item
            label="密码"
            label-placement="top"
            :validation-status="showErr('password') ? 'error' : undefined"
            :feedback="showErr('password') ? fieldErrors.password : undefined"
          >
            <n-input
              v-model:value="password"
              type="password"
              autocomplete="new-password"
              @blur="touch('password')"
              @input="onPasswordInput"
            />
          </n-form-item>
          <n-form-item
            label="确认密码"
            label-placement="top"
            :validation-status="showErr('confirmPassword') ? 'error' : undefined"
            :feedback="showErr('confirmPassword') ? fieldErrors.confirmPassword : undefined"
          >
            <n-input
              v-model:value="confirmPassword"
              type="password"
              autocomplete="new-password"
              @blur="touch('confirmPassword')"
              @input="touch('confirmPassword')"
            />
          </n-form-item>
          <n-form-item
            label="验证码"
            label-placement="top"
            :validation-status="showErr('captchaCode') ? 'error' : undefined"
            :feedback="showErr('captchaCode') ? fieldErrors.captchaCode : undefined"
          >
            <div class="captcha-line">
              <n-input
                v-model:value="captchaCode"
                autocomplete="off"
                maxlength="8"
                placeholder="右侧图形中的字符"
                @blur="touch('captchaCode')"
                @input="touch('captchaCode')"
              />
              <n-button tertiary type="default" @click.prevent="loadCaptcha">
                <n-image v-if="captchaSrc" :src="captchaSrc" alt="验证码" width="120" height="40" preview-disabled object-fit="cover" />
                <span v-else class="captcha-placeholder">加载中…</span>
                <template #icon>
                  <n-icon><reload-outline /></n-icon>
                </template>
              </n-button>
            </div>
          </n-form-item>
          <n-form-item :show-label="false" :show-feedback="false">
            <n-button attr-type="submit" type="primary" block :loading="isLoading">{{ isLoading ? '注册中…' : '注册' }}</n-button>
          </n-form-item>
          <p class="switch-link">已有账号？<router-link to="/login">去登录</router-link></p>
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
  NForm,
  NFormItem,
  NIcon,
  NImage,
  NInput,
} from 'naive-ui';
import { reactive, ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { register, fetchCaptcha } from '../api/auth';
import { applyServerFieldErrors, validateRegisterField } from '../utils/formValidation';

const username = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const captchaKey = ref('');
const captchaCode = ref('');
const captchaSrc = ref('');
const isLoading = ref(false);
const error = ref(null);
const errorTick = ref(0);
const router = useRouter();
const authStore = useAuthStore();

const touched = reactive({
  username: false,
  email: false,
  password: false,
  confirmPassword: false,
  captchaCode: false,
});

const fieldErrors = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
});

const FIELDS = ['username', 'email', 'password', 'confirmPassword', 'captchaCode'];

function formValues() {
  return {
    username: username.value,
    email: email.value,
    password: password.value,
    confirmPassword: confirmPassword.value,
    captchaCode: captchaCode.value,
  };
}

function validateOne(field) {
  fieldErrors[field] = validateRegisterField(field, formValues());
}

function touch(field) {
  touched[field] = true;
  validateOne(field);
  if (field === 'password' && touched.confirmPassword) {
    validateOne('confirmPassword');
  }
}

function onPasswordInput() {
  touch('password');
}

function showErr(field) {
  return touched[field] && fieldErrors[field];
}

function validateAll() {
  FIELDS.forEach((f) => {
    touched[f] = true;
    validateOne(f);
  });
  return FIELDS.every((f) => !fieldErrors[f]);
}

async function loadCaptcha() {
  captchaCode.value = '';
  touched.captchaCode = false;
  fieldErrors.captchaCode = '';
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
    router.replace({ name: 'Home' });
    return;
  }
  await loadCaptcha();
});

async function handleRegister() {
  error.value = null;
  if (!validateAll()) return;
  isLoading.value = true;
  try {
    const res = await register({
      username: username.value.trim(),
      email: email.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value,
      captchaKey: captchaKey.value,
      captchaCode: captchaCode.value,
    });
    const data = res?.data;
    if (data?.token) {
      authStore.loginSuccess(data.token, data.role);
      try {
        await authStore.fetchMe();
      } catch {
        /* ignore */
      }
    }
    router.push({ name: 'Home' });
  } catch (err) {
    if (err?.payload && typeof err.payload === 'object') {
      Object.assign(fieldErrors, applyServerFieldErrors(fieldErrors, err.payload));
      FIELDS.forEach((f) => {
        if (fieldErrors[f]) touched[f] = true;
      });
    }
    error.value = err?.message || '注册失败';
    errorTick.value += 1;
    await loadCaptcha();
  } finally {
    isLoading.value = false;
  }
}
</script>

<style scoped>
.login-wrap {
  position: relative;
  z-index: 1;
}

.card-header-inner {
  text-align: center;
}

.card-title {
  margin: 0;
  font-size: var(--text-xl);
  font-weight: var(--weight-black);
  letter-spacing: -0.04em;
  color: var(--color-text);
}

.login-form :deep(.n-input) {
  border-radius: var(--radius-brutal-btn);
  border: var(--border-brutal) !important;
  background: var(--surface-input) !important;
  box-shadow: var(--shadow-brutal-sm);
}

.login-form :deep(.n-input--focus) {
  border-color: var(--border-focus-input) !important;
  box-shadow: var(--shadow-brutal-sm) !important;
}

.login-form :deep(.n-button--primary-type) {
  background: var(--color-accent) !important;
  color: var(--color-on-primary) !important;
  border: var(--border-brutal) !important;
  box-shadow: var(--shadow-brutal) !important;
  font-weight: var(--weight-bold) !important;
}

.login-form :deep(.n-button--primary-type:hover) {
  transform: translate(2px, 2px);
  box-shadow: var(--shadow-brutal-sm) !important;
}

.card-hint {
  margin: var(--space-2) 0 0;
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.login-form {
  text-align: left;
}

.captcha-line {
  display: flex;
  gap: var(--space-2);
  align-items: stretch;
  width: 100%;
}

.captcha-line :deep(.n-input) {
  flex: 1;
  min-width: 0;
}

.captcha-placeholder {
  font-size: var(--text-xs);
  color: var(--color-text-soft);
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
