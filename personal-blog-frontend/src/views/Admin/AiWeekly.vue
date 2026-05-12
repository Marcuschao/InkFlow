<template>
  <div class="ai-weekly-page">
    <div class="container">
      <router-link to="/admin" class="weekly-back">← 返回管理</router-link>
      <header class="weekly-head">
        <div>
          <h1 class="page-title">AI 周报</h1>
          <p class="page-sub">本周热门文章摘要与写作建议</p>
        </div>
        <button type="button" class="weekly-run" :disabled="loading" @click="run">
          <span v-if="loading" class="weekly-spin" aria-hidden="true" />
          {{ loading ? '生成中…' : '一键生成' }}
        </button>
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
.ai-weekly-page {
  padding: 2.25rem 0 3.5rem;
}

.weekly-back {
  display: inline-flex;
  align-items: center;
  margin-bottom: 1rem;
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-primary);
  text-decoration: none;
}

.weekly-back:hover {
  text-decoration: underline;
}

.weekly-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.75rem;
}

.page-title {
  margin: 0;
  font-size: clamp(1.75rem, 3.5vw, 2.2rem);
  font-weight: 760;
  color: var(--color-text);
}

.page-sub {
  margin: 0.4rem 0 0;
  font-size: 0.9rem;
  color: var(--color-text-muted);
}

.weekly-run {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.15rem;
  border: none;
  border-radius: var(--radius-md);
  font-weight: 650;
  font-size: 0.92rem;
  cursor: pointer;
  color: #fff;
  background: var(--gradient-cta);
  font-family: inherit;
  box-shadow: var(--shadow-card-accent);
}

.weekly-run:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.weekly-spin {
  width: 0.95rem;
  height: 0.95rem;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  animation: spin 0.72s linear infinite;
}

.weekly-error {
  color: #b91c1c;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.weekly-body {
  background: var(--color-surface);
  padding: clamp(1.25rem, 3vw, 2rem);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-md);
}

.weekly-body :deep(.markdown-prose) {
  font-family: var(--font-prose);
}
</style>
