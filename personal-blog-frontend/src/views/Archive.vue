<template>
  <div class="archive-page ds-page">
    <div class="container" style="max-width: 720px;">
      <header class="ds-page-hero" style="margin-bottom: 24px;">
        <h1 class="ds-page-title ds-page-title-md">文章归档</h1>
        <p class="ds-page-sub">按时间浏览全部文章</p>
      </header>

      <ListSkeleton v-if="loading" :count="3" />

      <div v-else-if="archives.length">
        <n-timeline>
          <n-timeline-item
            v-for="yearData in archives"
            :key="yearData.year"
            type="primary"
            :title="String(yearData.year) + ' 年'"
          >
            <div v-for="monthData in yearData.months" :key="`${yearData.year}-${monthData.month}`" style="margin: 16px 0;">
              <h3 style="margin-bottom: 12px; font-size: 1.1em; font-weight: 600;">
                {{ monthData.month }} 月
                <span style="font-weight: normal; font-size: 0.85em; color: var(--color-text-muted); margin-left: 8px;">
                  {{ monthData.articles.length }} 篇
                </span>
              </h3>
              <n-list bordered style="background-color: var(--color-surface);">
                <n-list-item v-for="article in monthData.articles" :key="article.id">
                  <n-space justify="space-between" align="center" style="width: 100%;">
                    <router-link :to="`/article/${article.id}`" style="text-decoration: none; color: inherit; font-weight: 500;">
                      {{ article.title }}
                    </router-link>
                    <span style="font-size: 0.85em; color: var(--color-text-muted);">
                      {{ formatDate(article.createTime || article.createdAt) }}
                    </span>
                  </n-space>
                </n-list-item>
              </n-list>
            </div>
          </n-timeline-item>
        </n-timeline>
      </div>

      <div v-else>
        <n-empty description="暂无归档文章" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { NCard, NEmpty, NList, NListItem, NSpace, NTimeline, NTimelineItem } from 'naive-ui';
import { getArticles } from '../api/article';
import ListSkeleton from '../components/ListSkeleton.vue';

const archives = ref([]);
const loading = ref(true);

const buildArchives = (articles) => {
  const tempArchives = {};
  articles.forEach((article) => {
    const t = article.createTime || article.createdAt;
    if (!t) return;
    const date = new Date(t);
    const year = date.getFullYear();
    const month = date.getMonth() + 1;

    if (!tempArchives[year]) {
      tempArchives[year] = { year, months: {} };
    }
    if (!tempArchives[year].months[month]) {
      tempArchives[year].months[month] = { month, articles: [] };
    }
    tempArchives[year].months[month].articles.push(article);
  });

  return Object.values(tempArchives)
    .map((yearData) => ({
      year: yearData.year,
      months: Object.values(yearData.months).sort((a, b) => b.month - a.month),
    }))
    .sort((a, b) => b.year - a.year);
};

const fetchArchives = async () => {
  loading.value = true;
  try {
    const pageSize = 500;
    let page = 1;
    const all = [];
    let total = Infinity;
    while (all.length < total) {
      const res = await getArticles({ page, size: pageSize });
      const d = res.data;
      const records = d.records || [];
      total = Number(d.total) || 0;
      all.push(...records);
      if (!records.length || all.length >= total) break;
      page++;
    }
    archives.value = buildArchives(all);
  } catch (e) {
    console.error(e);
    archives.value = [];
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateString) => {
  const date = new Date(dateString);
  return `${date.getMonth() + 1}月${date.getDate()}日`;
};

onMounted(() => {
  fetchArchives();
});
</script>

<style scoped>
</style>
