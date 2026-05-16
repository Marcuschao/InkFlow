<template>
  <div class="tr-page admin-page">
    <div class="container">
      <header class="ds-admin-header">
        <div>
          <h1 class="ds-page-title">批量翻译</h1>
          <p class="ds-page-sub">机翻任务 · en / ja / ko</p>
        </div>
        <router-link to="/admin" class="admin-link-btn">返回管理</router-link>
      </header>

      <div class="panel ds-surface-card">
        <label class="ds-form-label lb-block" for="ids-raw">文章 ID（逗号或空格分隔）</label>
        <textarea id="ids-raw" v-model="idsRaw" class="ta ds-textarea" rows="3" placeholder="1, 2, 3" />

        <div class="loc-row">
          <span class="ds-form-label lb-inline">语种</span>
          <label v-for="loc in locales" :key="loc" class="ck">
            <input v-model="picked" type="checkbox" :value="loc" />
            {{ loc }}
          </label>
        </div>

        <button type="button" class="go ds-btn ds-btn--primary" :disabled="starting" @click="start">
          {{ starting ? '提交中…' : '启动批量机翻' }}
        </button>
      </div>

      <div v-if="jobId" class="job-panel">
        <div class="job-id">任务 {{ jobId }}</div>
        <div v-if="job" class="job-st">
          <span>状态 {{ job.state }}</span>
          <span>{{ job.processed }} / {{ job.total }}</span>
        </div>
        <p v-if="job?.errorMessage" class="job-err">{{ job.errorMessage }}</p>
      </div>

      <p v-if="err" class="err">{{ err }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onUnmounted } from 'vue';
import { batchTranslate, getTranslationJob } from '../../api/translation';

const locales = ['en', 'ja', 'ko'];
const idsRaw = ref('');
const picked = ref(['en']);
const starting = ref(false);
const jobId = ref('');
const job = ref(null);
const err = ref('');
let timer = null;

function parseIds() {
  return idsRaw.value
    .split(/[,，\s]+/)
    .map((s) => parseInt(s.trim(), 10))
    .filter((n) => Number.isFinite(n));
}

async function pollOnce() {
  if (!jobId.value) return;
  try {
    const res = await getTranslationJob(jobId.value);
    job.value = res.data ?? null;
    const st = job.value?.state;
    if (st === 'DONE' || st === 'FAILED') stopPoll();
  } catch {
    stopPoll();
  }
}

function stopPoll() {
  if (timer) clearInterval(timer);
  timer = null;
}

async function start() {
  err.value = '';
  stopPoll();
  job.value = null;
  const articleIds = parseIds();
  const locs = picked.value.slice();
  if (!articleIds.length || !locs.length) {
    err.value = '请填写文章 ID 并选择语种';
    return;
  }
  starting.value = true;
  try {
    const res = await batchTranslate({ articleIds, locales: locs });
    jobId.value = res.data || '';
    await pollOnce();
    timer = setInterval(pollOnce, 2000);
  } catch (e) {
    err.value = e?.message || '启动失败';
  } finally {
    starting.value = false;
  }
}

onUnmounted(stopPoll);
</script>

<style scoped>
.lb-block {
  margin-bottom: var(--space-2);
}

.lb-inline {
  display: inline-block;
  margin-bottom: 0;
  margin-right: var(--space-2);
}

.ta {
  min-height: 0;
  margin-bottom: var(--space-4);
}

.panel {
  padding: var(--space-5) 1.35rem;
  max-width: 560px;
  box-shadow: none;
}

.go {
  width: 100%;
  margin-top: var(--space-2);
}

.loc-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
}

.ck {
  font-size: var(--text-base);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.job-panel {
  margin-top: 1.35rem;
  padding: 1rem 1.15rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--admin-panel-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.job-id {
  font-size: 0.78rem;
  font-weight: 700;
  word-break: break-all;
}

.job-st {
  margin-top: 0.5rem;
  display: flex;
  gap: 1rem;
  font-size: 0.88rem;
}

.job-err {
  margin: 0.5rem 0 0;
  color: var(--color-danger);
  font-size: 0.85rem;
}

.err {
  margin-top: 1rem;
  color: var(--color-danger);
}
</style>
