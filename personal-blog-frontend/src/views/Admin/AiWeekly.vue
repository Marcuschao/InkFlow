<template>
  <div class="ai-weekly-page admin-page">
    <div class="container">
      <header class="weekly-head ds-admin-header">
        <div>
          <h1 class="ds-page-title">AI 周报</h1>
          <p class="ds-page-sub">本周热门文章摘要与写作建议</p>
        </div>
        <div class="dash-actions">
          <router-link to="/admin" class="admin-link-btn">返回管理</router-link>
          <button type="button" class="ds-btn ds-btn--primary ds-btn--pill" :disabled="loading" @click="run">
            <span v-if="loading" class="ds-spin-lg" aria-hidden="true" />
            {{ loading ? '生成中…' : '一键生成' }}
          </button>
        </div>
      </header>
      <p v-if="error" class="weekly-error">{{ error }}</p>
      <div v-if="body" class="weekly-body">
        <MarkdownRenderer :markdown="body" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import MarkdownRenderer from '../../components/MarkdownRenderer.vue';
import { agentWeeklyReport } from '../../api/agent';

const loading = ref(false);
const error = ref('');
const body = ref('');

async function run() {
  loading.value = true;
  error.value = '';
  try {
    body.value = await agentWeeklyReport({});
  } catch (e) {
    error.value = e?.message || '生成失败';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.weekly-error {
  color: var(--color-danger);
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.weekly-body {
  background: var(--admin-panel-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding: clamp(1.25rem, 3vw, 2rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: none;
}

.weekly-body :deep(.markdown-prose) {
  font-family: var(--font-prose);
}
</style>
