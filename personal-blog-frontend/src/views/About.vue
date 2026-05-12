<template>
  <div class="about-page ds-page">
    <div class="container">
      <header class="ds-page-hero">
        <h1 class="ds-page-title ds-page-title-md">关于我</h1>
      </header>

      <div v-if="loading" class="about-skeleton ds-surface-card">
        <div class="ui-skeleton sk-line" />
        <div class="ui-skeleton sk-line" />
        <div class="ui-skeleton sk-line short" />
        <div class="ui-skeleton sk-line" />
        <div class="ui-skeleton sk-line medium" />
      </div>
      <div v-else class="about-content-wrapper ds-surface-card">
        <MarkdownRenderer :markdown="aboutContent" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getAboutContent } from '../api/about';
import MarkdownRenderer from '../components/MarkdownRenderer.vue';

const aboutContent = ref('');
const loading = ref(true);

const fetchAboutContent = async () => {
  loading.value = true;
  try {
    const response = await getAboutContent();
    aboutContent.value = response.data.content;
  } catch (error) {
    console.error('Failed to fetch about content:', error);
    aboutContent.value = '加载失败，请稍后再试。';
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchAboutContent();
});
</script>

<style scoped>
.about-skeleton {
  max-width: 42rem;
  margin: 0 auto;
  padding: var(--space-8);
  box-shadow: var(--shadow-sm);
}

.about-skeleton .sk-line {
  height: 0.85rem;
  margin-bottom: 0.65rem;
}

.about-skeleton .sk-line.short {
  width: 55%;
}

.about-skeleton .sk-line.medium {
  width: 78%;
}

.about-content-wrapper {
  max-width: 42rem;
  margin: 0 auto;
  padding: clamp(1.75rem, 4vw, 2.75rem);
}

.about-content-wrapper :deep(.markdown-prose) {
  font-size: 1.06rem;
}
</style>
