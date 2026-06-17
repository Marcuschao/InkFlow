<template>
  <div class="auth-page">
    <div class="auth-page-bg" aria-hidden="true" />
    <div class="login-wrap">
      <n-card class="auth-card ds-brutal-surface" :bordered="false">
        <template #header>
          <div class="card-header-inner">
            <h1 class="card-title">重置密码</h1>
            <p class="card-hint">输入注册邮箱获取重置链接</p>
          </div>
        </template>
        <n-form class="login-form" @submit.prevent="handleSubmit">
          <n-form-item
            label="邮箱"
            label-placement="top"
            :validation-status="emailError ? 'error' : undefined"
            :feedback="emailError || undefined"
          >
            <n-input v-model:value="email" type="text" inputmode="email" autocomplete="email" @input="validateEmail" />
          </n-form-item>
          <n-form-item :show-label="false" :show-feedback="false">
            <n-button attr-type="submit" type="primary" block :loading="loading">{{ loading ? '提交中…' : '发送重置邮件' }}</n-button>
          </n-form-item>
          <p class="switch-link"><router-link to="/login">返回登录</router-link></p>
          <n-alert v-if="success" type="success" class="form-alert-tight">{{ success }}</n-alert>
          <n-alert v-if="error" :key="errorTick" type="error" class="form-alert-tight">{{ error }}</n-alert>
        </n-form>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { NAlert, NButton, NCard, NForm, NFormItem, NInput } from 'naive-ui';
import { ref } from 'vue';
import { requestPasswordReset } from '../api/auth';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SAFE_MESSAGE = '如该邮箱已注册，将收到重置邮件';

const email = ref('');
const emailError = ref('');
const loading = ref(false);
const success = ref('');
const error = ref('');
const errorTick = ref(0);

function validateEmail() {
  const v = email.value.trim();
  if (!v) {
    emailError.value = '邮箱不能为空';
  } else if (!EMAIL_RE.test(v)) {
    emailError.value = '邮箱格式不正确';
  } else {
    emailError.value = '';
  }
  return !emailError.value;
}

async function handleSubmit() {
  error.value = '';
  success.value = '';
  if (!validateEmail()) return;
  loading.value = true;
  try {
    await requestPasswordReset({ email: email.value.trim() });
    success.value = SAFE_MESSAGE;
  } catch (err) {
    if (err?.code === 429) {
      error.value = err.message;
      errorTick.value += 1;
    } else {
      success.value = SAFE_MESSAGE;
    }
  } finally {
    loading.value = false;
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

.card-hint {
  margin: var(--space-2) 0 0;
  color: var(--color-text-soft);
  font-size: var(--text-sm);
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

.form-alert-tight {
  margin-top: var(--space-3);
}

.switch-link {
  margin: var(--space-3) 0 0;
  text-align: center;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
</style>
