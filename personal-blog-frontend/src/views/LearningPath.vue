<template>
  <div class="learning-path-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title">学习路径</h1>
        <p class="ds-page-sub">{{ pathName }}</p>
      </header>
      <n-input v-model:value="goal" placeholder="输入学习目标" class="goal-input" @keyup.enter="generate" />
      <n-button type="primary" :loading="loading" @click="generate">生成路径</n-button>
      <n-timeline v-if="steps.length" class="path-timeline">
        <n-timeline-item v-for="(step, idx) in steps" :key="idx" :title="step.title" :content="step.description">
          <n-space v-if="step.articleIds?.length" vertical :size="8">
            <router-link v-for="aid in step.articleIds" :key="aid" :to="`/article/${aid}`" class="art-link">
              文章 #{{ aid }}
            </router-link>
          </n-space>
        </n-timeline-item>
      </n-timeline>
      <n-empty v-else-if="searched" description="暂无路径，请先生成" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { NButton, NEmpty, NInput, NSpace, NTimeline, NTimelineItem } from 'naive-ui';
import request from '../utils/request';

const route = useRoute();
const goal = ref((route.query.goal || '').toString());
const loading = ref(false);
const searched = ref(false);
const pathName = ref('');
const steps = ref([]);

function applyResult(data) {
  if (!data) return;
  pathName.value = data.name || goal.value;
  steps.value = data.steps || [];
}

async function generate() {
  const g = goal.value.trim();
  if (!g) return;
  loading.value = true;
  searched.value = true;
  try {
    const res = await request({
      url: '/agent/learning-path',
      method: 'post',
      data: { goal: g },
      timeout: 120000,
    });
    applyResult(res.data);
  } catch {
    steps.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  const state = history.state?.pathResult;
  if (state) {
    applyResult(state);
    searched.value = true;
  } else if (goal.value) {
    generate();
  }
});
</script>

<style scoped>
.goal-input {
  max-width: 420px;
  margin-bottom: var(--space-4);
}

.path-timeline {
  margin-top: var(--space-8);
  max-width: 640px;
}

.art-link {
  color: var(--color-primary);
  font-size: var(--text-sm);
}
</style>
