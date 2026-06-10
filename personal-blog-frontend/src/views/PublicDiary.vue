<template>
  <div class="pub-diary-page ds-page">
    <div class="container">
      <header class="ds-page-hero" style="margin-bottom: 24px;">
        <h1 class="ds-page-title ds-page-title-md">分享</h1>
        <p class="ds-page-sub">大家公开的笔记与心得</p>
      </header>
      <n-alert v-if="err" type="error" style="margin-bottom: 16px;">{{ err }}</n-alert>

      <div v-if="loading">
        <n-space vertical :size="16">
          <n-skeleton v-for="n in 5" :key="n" height="100px" :sharp="false" />
        </n-space>
      </div>

      <div v-else-if="!records.length">
        <n-empty description="暂无公开分享" />
      </div>

      <div v-else>
        <n-space vertical :size="16">
          <n-card v-for="row in records" :key="row.id" class="hit-card" hoverable>
            <router-link :to="'/share/' + row.id" class="hit-link" style="text-decoration: none; color: inherit;">
              <h2 class="hit-title" style="margin: 0 0 8px; font-size: 1.25em;">{{ row.title }}</h2>
              <p class="hit-sum" style="color: var(--color-text-muted); margin: 0 0 12px; line-height: 1.55;">
                {{ row.excerpt || '—' }}
              </p>
              <time class="hit-time" style="font-size: 0.85em; color: var(--color-text-muted);">{{ row.diaryDate }}</time>
            </router-link>
          </n-card>
        </n-space>
      </div>

      <Pagination
        v-if="total > 0"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @changePage="onPage"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { NAlert, NCard, NEmpty, NSkeleton, NSpace } from 'naive-ui';
import Pagination from '../components/Pagination.vue';
import { listPublicDiaries } from '../api/diary';

const records = ref([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);
const loading = ref(true);
const err = ref('');

async function load() {
  loading.value = true;
  err.value = '';
  try {
    const res = await listPublicDiaries({ page: page.value, size: pageSize.value });
    const data = res.data;
    records.value = data?.records || [];
    total.value = Number(data?.total) || 0;
  } catch (e) {
    err.value = e?.message || '加载失败';
    records.value = [];
  } finally {
    loading.value = false;
  }
}

function onPage(p) {
  page.value = p;
  load();
}

onMounted(load);
</script>

<style scoped>
</style>
