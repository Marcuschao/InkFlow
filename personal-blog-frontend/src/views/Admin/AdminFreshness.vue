<template>
  <div class="fresh-page admin-page">
    <div class="container">
      <header class="fresh-head ds-admin-header">
        <div>
          <h1 class="ds-page-title">内容保鲜</h1>
          <p class="ds-page-sub">陈旧文章扫描与 AI 草稿</p>
        </div>
        <div class="fresh-head-actions">
          <router-link to="/admin" class="ds-btn ds-btn--secondary ds-btn--pill">返回管理</router-link>
          <button type="button" class="scan-btn ds-btn ds-btn--primary ds-btn--pill" :disabled="scanBusy" @click="runScan">
            {{ scanBusy ? '扫描中…' : '立即全量扫描' }}
          </button>
        </div>
      </header>

      <div v-if="summary" class="sum-row">
        <span class="chip ok">健康 {{ summary.healthyCount }}</span>
        <span class="chip warn">预警 {{ summary.warnCount }}</span>
        <span class="chip bad">严重 {{ summary.severeCount }}</span>
        <span v-if="summary.lastFullScanAt" class="last">上次：{{ summary.lastFullScanAt }}</span>
      </div>

      <div class="filter-row">
        <label class="fil-label">状态</label>
        <select v-model="statusFilter" class="fil-select" @change="reload">
          <option value="">全部</option>
          <option value="0">正常</option>
          <option value="1">预警</option>
          <option value="2">严重</option>
        </select>
      </div>

      <div v-if="draftBox" class="draft-box">
        <div class="draft-h">AI 草稿 · {{ draftBox.id }}</div>
        <p class="draft-t">{{ draftBox.title }}</p>
        <p class="draft-s">{{ draftBox.summary }}</p>
        <pre class="draft-c">{{ draftBox.content }}</pre>
        <button type="button" class="draft-x" @click="draftBox = null">关闭</button>
      </div>

      <div class="table-wrap ds-table-shell">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>保鲜</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in rows" :key="a.id">
              <td>{{ a.id }}</td>
              <td class="td-t">{{ a.title }}</td>
              <td>{{ a.freshnessStatus }}</td>
              <td class="td-act">
                <button type="button" class="btn-sm ds-btn ds-btn--secondary ds-btn--pill" :disabled="busyId === a.id" @click="aiRow(a.id)">
                  AI 草稿
                </button>
                <button type="button" class="btn-sm sec ds-btn ds-btn--ghost ds-btn--pill" :disabled="busyId === a.id" @click="forkRow(a.id)">
                  复制为草稿
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pager">
        <button type="button" :disabled="page <= 1" @click="page--; reload()">上一页</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button type="button" :disabled="page >= totalPages" @click="page++; reload()">下一页</button>
      </div>
      <p v-if="err" class="err">{{ err }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import {
  getFreshnessSummary,
  getFreshnessArticles,
  postFreshnessScanRun,
  postFreshnessAiRefresh,
  postFreshnessForkDraft,
} from '../../api/freshness';

const router = useRouter();
const summary = ref(null);
const rows = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(12);
const statusFilter = ref('');
const scanBusy = ref(false);
const busyId = ref(null);
const draftBox = ref(null);
const err = ref('');

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value) || 1));

async function loadSummary() {
  try {
    const res = await getFreshnessSummary();
    summary.value = res.data ?? null;
  } catch {
    summary.value = null;
  }
}

async function reload() {
  err.value = '';
  try {
    const params = { page: page.value, size: pageSize.value };
    if (statusFilter.value !== '') params.freshnessStatus = parseInt(statusFilter.value, 10);
    const res = await getFreshnessArticles(params);
    const pr = res.data;
    rows.value = pr?.records || [];
    total.value = pr?.total ?? 0;
  } catch (e) {
    err.value = e?.message || '加载失败';
    rows.value = [];
  }
}

async function runScan() {
  scanBusy.value = true;
  err.value = '';
  try {
    await postFreshnessScanRun();
    await loadSummary();
    await reload();
  } catch (e) {
    err.value = e?.message || '扫描失败';
  } finally {
    scanBusy.value = false;
  }
}

async function aiRow(id) {
  busyId.value = id;
  err.value = '';
  try {
    const res = await postFreshnessAiRefresh(id);
    draftBox.value = { id, ...(res.data || {}) };
  } catch (e) {
    err.value = e?.message || 'AI 失败';
  } finally {
    busyId.value = null;
  }
}

async function forkRow(id) {
  busyId.value = id;
  err.value = '';
  try {
    const res = await postFreshnessForkDraft(id);
    const nid = res.data;
    if (nid != null) router.push(`/admin/edit/${nid}`);
  } catch (e) {
    err.value = e?.message || '复制失败';
  } finally {
    busyId.value = null;
  }
}

onMounted(async () => {
  await loadSummary();
  await reload();
});
</script>

<style scoped>
.fresh-head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
}

.scan-btn {
  flex-shrink: 0;
}

.sum-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  align-items: center;
  margin-bottom: 1rem;
}

.chip {
  font-size: 0.78rem;
  font-weight: 700;
  padding: 0.35rem 0.65rem;
  border-radius: var(--radius-pill);
}

.chip.ok {
  background: rgba(16, 185, 129, 0.15);
  color: #047857;
}

.chip.warn {
  background: var(--color-warn-soft);
  color: var(--color-warn);
}

.chip.bad {
  background: rgba(239, 68, 68, 0.15);
  color: var(--color-danger);
}

.last {
  font-size: 0.82rem;
  color: var(--color-text-muted);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.fil-label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.fil-select {
  padding: 0.45rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.draft-box {
  margin-bottom: var(--space-5);
  padding: var(--space-4) 1.15rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--admin-panel-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.draft-h {
  font-weight: 700;
  margin-bottom: 0.5rem;
}

.draft-t {
  font-weight: 650;
  margin: 0 0 0.35rem;
}

.draft-s {
  margin: 0 0 0.75rem;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.draft-c {
  margin: 0 0 0.75rem;
  white-space: pre-wrap;
  font-size: 0.78rem;
  max-height: 240px;
  overflow: auto;
}

.draft-x {
  padding: 0.35rem 0.75rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
}

.table-wrap {
  overflow-x: auto;
}

.table-wrap table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  text-align: left;
  font-size: var(--text-88);
}

th {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-text-soft);
}

.td-t {
  font-weight: var(--weight-semibold);
  max-width: 280px;
}

.td-act {
  white-space: nowrap;
}

.btn-sm {
  padding: 0.35rem 0.65rem;
  margin-right: var(--space-2);
  font-size: var(--text-sm);
}

.pager {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 1rem;
}

.pager button {
  padding: 0.4rem 0.85rem;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
}

.err {
  color: var(--color-danger);
  margin-top: 0.75rem;
}
</style>
