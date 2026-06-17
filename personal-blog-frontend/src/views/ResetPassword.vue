<template>
  <div class="auth-page">
    <div class="auth-page-bg" aria-hidden="true" />
    <div class="login-wrap">
      <n-card class="auth-card ds-brutal-surface" :bordered="false">
        <template #header>
          <div class="card-header-inner">
            <h1 class="card-title">设置新密码</h1>
            <p class="card-hint">新密码需至少 8 位且包含字母和数字</p>
          </div>
        </template>
        <n-spin :show="validating">
          <n-alert v-if="!validating && !valid" type="error" class="form-alert-tight">链接已过期或无效</n-alert>
          <n-form v-else class="login-form" @submit.prevent="handleSubmit">
            <n-form-item
              label="新密码"
              label-placement="top"
              :validation-status="passwordError ? 'error' : undefined"
              :feedback="passwordError || undefined"
            >
              <n-input v-model:value="password" type="password" autocomplete="new-password" @input="validatePassword" />
            </n-form-item>
            <n-form-item
              label="确认新密码"
              label-placement="top"
              :validation-status="confirmError ? 'error' : undefined"
              :feedback="confirmError || undefined"
            >
              <n-input v-model:value="confirmPassword" type="password" autocomplete="new-password" @input="validateConfirm" />
            </n-form-item>
            <n-form-item :show-label="false" :show-feedback="false">
              <n-button attr-type="submit" type="primary" block :loading="loading">{{ loading ? '提交中…' : '确认重置' }}</n-button>
            </n-form-item>
            <n-alert v-if="success" type="success" class="form-alert-tight">{{ success }}</n-alert>
            <n-alert v-if="error" :key="errorTick" type="error" class="form-alert-tight">{{ error }}</n-alert>
          </n-form>
          <p class="switch-link"><router-link to="/login">返回登录</router-link></p>
        </n-spin>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { NAlert, NButton, NCard, NForm, NFormItem, NInput, NSpin } from 'naive-ui';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { resetPassword, validatePasswordResetToken } from '../api/auth';

const route = useRoute();
const router = useRouter();
const token = ref('');
const valid = ref(false);
const validating = ref(true);
const loading = ref(false);
const password = ref('');
const confirmPassword = ref('');
const passwordError = ref('');
const confirmError = ref('');
const success = ref('');
const error = ref('');
const errorTick = ref(0);

function validatePassword() {
  const v = password.value;
  if (!v) {
    passwordError.value = '请输入新密码';
  } else if (v.length < 8 || !/[A-Za-z]/.test(v) || !/\d/.test(v)) {
    passwordError.value = '密码需至少 8 位且包含字母和数字';
  } else {
    passwordError.value = '';
  }
  validateConfirm();
  return !passwordError.value;
}

function validateConfirm() {
  if (!confirmPassword.value) {
    confirmError.value = '请再次输入新密码';
  } else if (confirmPassword.value !== password.value) {
    confirmError.value = '两次输入的密码不一致';
  } else {
    confirmError.value = '';
  }
  return !confirmError.value;
}

async function handleSubmit() {
  error.value = '';
  success.value = '';
  if (!validatePassword() || !validateConfirm()) return;
  loading.value = true;
  try {
    await resetPassword({ token: token.value, newPassword: password.value });
    success.value = '密码已重置，请重新登录';
    setTimeout(() => router.replace('/login'), 1200);
  } catch (err) {
    error.value = err?.message || '密码重置失败';
    errorTick.value += 1;
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  token.value = typeof route.query.token === 'string' ? route.query.token : '';
  if (!token.value) {
    validating.value = false;
    return;
  }
  try {
    const data = await validatePasswordResetToken(token.value);
    valid.value = Boolean(data?.valid);
  } catch {
    valid.value = false;
  } finally {
    validating.value = false;
  }
});
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
