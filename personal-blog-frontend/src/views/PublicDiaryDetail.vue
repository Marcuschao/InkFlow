<template>
  <div class="pub-diary-detail ds-page">
    <div class="container narrow" style="max-width: 720px;">
      <p v-if="loading" style="color: var(--color-text-muted);">加载中…</p>
      <n-alert v-else-if="err" type="error">{{ err }}</n-alert>
      <n-card v-else-if="diary" class="detail-card">
        <header class="dh" style="margin-bottom: 24px;">
          <h1 class="title" style="margin: 0 0 12px; font-size: 1.75em;">{{ diary.title || '未命名' }}</h1>
          <n-space class="meta" :size="12" align="center">
            <time style="color: var(--color-text-muted); font-size: 0.9em;">{{ diary.diaryDate }}</time>
            <n-tag v-if="diary.tags" type="info" :bordered="false" size="small">{{ diary.tags }}</n-tag>
          </n-space>
        </header>
        <div v-if="Number(diary.contentType) === 1" class="body markdown-renderer markdown-prose" style="margin-bottom: 24px;">
          <MarkdownRenderer :markdown="diary.content || ''" />
        </div>
        <pre v-else class="body plain" style="white-space: pre-wrap; font-family: monospace; line-height: 1.65; margin: 0 0 24px; background: var(--surface-muted); padding: 16px; border-radius: 6px;">{{ diary.content }}</pre>
        <footer class="foot">
          <router-link to="/share">
            <n-button secondary>← 返回列表</n-button>
          </router-link>
        </footer>
      </n-card>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { NAlert, NButton, NCard, NSpace, NTag } from 'naive-ui';
import MarkdownRenderer from '../components/MarkdownRenderer.vue';
import { getPublicDiary } from '../api/diary';

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
    const res = await getPublicDiary(Number(props.id));
    diary.value = res.data || null;
    if (!diary.value) err.value = '未找到或未公开';
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
</style>
