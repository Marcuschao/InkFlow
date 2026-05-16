<template>
  <div class="diary-detail-page admin-page">
    <div class="container narrow">
      <p v-if="loading" class="muted">加载中…</p>
      <p v-else-if="err" class="err">{{ err }}</p>
      <article v-else-if="diary" class="detail-card ds-surface-card">
        <header class="dh ds-admin-header ds-admin-header--stack">
          <div class="dh-inner">
            <h1 class="ds-page-title">{{ diary.title || '未命名' }}</h1>
            <div class="meta">
              <time>{{ diary.diaryDate }}</time>
              <span v-if="diary.tags" class="tags">{{ diary.tags }}</span>
              <span v-if="Number(diary.isPublic) === 1" class="pub">公开</span>
            </div>
          </div>
        </header>
        <div v-if="Number(diary.contentType) === 1" class="body markdown-renderer markdown-prose">
          <MarkdownRenderer :markdown="diary.content || ''" />
        </div>
        <pre v-else class="body plain">{{ diary.content }}</pre>
        <footer class="foot-act">
          <router-link :to="'/admin/diary/edit/' + diary.id" class="ds-btn ds-btn--primary ds-btn--pill">编辑</router-link>
          <router-link to="/admin/diary/list" class="ds-btn ds-btn--secondary ds-btn--pill">返回列表</router-link>
        </footer>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import MarkdownRenderer from '../../components/MarkdownRenderer.vue';
import { getDiary } from '../../api/diary';

const props = defineProps({
  id: { type: [String, Number], required: true },
});

const diary = ref(null);
const loading = ref(true);
const err = ref('');

async function load() {
  loading.value = true;
  err.value = '';
  diary.value = null;
  try {
    const res = await getDiary(Number(props.id));
    diary.value = res.data || null;
    if (!diary.value) err.value = '未找到';
  } catch (e) {
    err.value = e?.message || '加载失败';
  } finally {
    loading.value = false;
  }
}

watch(
  () => props.id,
  () => load(),
  { immediate: true }
);
</script>

<style scoped>
.narrow {
  max-width: 720px;
}

.detail-card {
  padding: var(--space-8);
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.tags {
  color: var(--color-text-soft);
}

.pub {
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-pill);
  background: var(--surface-primary-tint);
  color: var(--color-primary);
}

.body.plain {
  white-space: pre-wrap;
  font-family: inherit;
  line-height: 1.65;
  margin: 0;
}

.foot-act {
  margin-top: var(--space-8);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.err {
  color: var(--color-danger);
}

.muted {
  color: var(--color-text-muted);
}
</style>
