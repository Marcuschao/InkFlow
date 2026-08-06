<template>
  <button
    v-if="showFab"
    type="button"
    class="ai-fab ai-fab-glow"
    aria-label="打开 AI 知识助手"
    @click="openAiWorkspace"
  >
    <n-icon :size="24" :component="SparklesOutline" />
  </button>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NIcon } from 'naive-ui';
import { SparklesOutline } from '@vicons/ionicons5';
import { useAiChatStore } from '../../stores/aiChat';
import { useAiChatVisibility } from '../../composables/useAiChatVisibility';

const route = useRoute();
const router = useRouter();
const { visible: aiChatVisible } = useAiChatVisibility();
const showFab = computed(() => route.name !== 'AiChat' && route.name !== 'ArticleDetail' && aiChatVisible.value);
function openAiWorkspace() {
  router.push({ name: 'AiChat' });
}
</script>

<style scoped>
.ai-fab {
  position: fixed;
  right: var(--space-4);
  bottom: calc(var(--space-4) + env(safe-area-inset-bottom, 0px));
  z-index: 1300;
  width: var(--ai-fab-size);
  height: var(--ai-fab-size);
  border-radius: 50%;
  border: var(--border-brutal);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-primary);
  background: var(--color-accent);
  box-shadow: var(--shadow-brutal-lg);
  transition: transform var(--transition-fast);
}

@media (max-width: 767px) {
  .ai-fab {
    bottom: var(--layout-fab-bottom);
  }
}
</style>
