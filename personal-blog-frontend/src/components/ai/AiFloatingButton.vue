<template>
  <button
    v-if="showFab"
    type="button"
    class="ai-fab ai-fab-glow"
    :class="{ 'ai-fab--open': aiChat.open }"
    :aria-label="aiChat.open ? '关闭 AI 助手' : '打开 AI 知识助手'"
    @click="aiChat.toggleWindow()"
  >
    <n-icon :size="24" :component="aiChat.open ? CloseOutline : SparklesOutline" />
    <span v-if="aiChat.hasNew && !aiChat.open" class="ai-fab-dot" aria-hidden="true" />
  </button>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { NIcon } from 'naive-ui';
import { CloseOutline, SparklesOutline } from '@vicons/ionicons5';
import { useAiChatStore } from '../../stores/aiChat';
import { useAiChatVisibility } from '../../composables/useAiChatVisibility';

const route = useRoute();
const aiChat = useAiChatStore();
const { visible: aiChatVisible } = useAiChatVisibility();
const showFab = computed(() => route.name !== 'AiChat' && route.name !== 'ArticleDetail' && aiChatVisible.value);
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

.ai-fab--open {
  background: var(--color-surface-raised);
  color: var(--color-text);
}

.ai-fab-dot {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-danger);
  border: 1.5px solid var(--color-surface);
}

@media (max-width: 767px) {
  .ai-fab {
    bottom: var(--layout-fab-bottom);
  }
}
</style>
