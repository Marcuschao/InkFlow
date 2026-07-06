<template>
  <div class="ai-chat-page ds-page">
    <div v-if="!loaded" class="container">
      <n-skeleton height="480px" :sharp="false" />
    </div>
    <div v-else-if="!aiChatVisible" class="container ai-chat-disabled">
      <n-empty description="AI 问答入口已由管理员关闭">
        <template #extra>
          <router-link to="/">
            <n-button>返回首页</n-button>
          </router-link>
        </template>
      </n-empty>
    </div>
    <div v-else class="container ai-chat-page-inner">
      <header class="ds-page-hero ai-chat-page-hero">
        <h1 class="ds-page-title ds-page-title-md">AI 知识助手</h1>
        <p class="ds-page-sub">多轮对话 · 引用溯源 · 知识库检索</p>
        <n-button v-if="isMobile" size="small" @click="drawerOpen = true">会话列表</n-button>
      </header>
      <div class="ai-chat-layout">
        <AiSessionList
          v-if="!isMobile"
          :sessions="aiChat.sessions"
          :active-id="aiChat.sessionId"
          @select="onSelect"
          @new="aiChat.newSession()"
          @delete="aiChat.removeSession"
        />
        <AiChatWindow embedded :visible="true" />
      </div>
    </div>
    <n-drawer v-model:show="drawerOpen" placement="left" :width="280">
      <n-drawer-content title="历史会话">
        <AiSessionList
          :sessions="aiChat.sessions"
          :active-id="aiChat.sessionId"
          @select="onSelectMobile"
          @new="onNewMobile"
          @delete="aiChat.removeSession"
        />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { NButton, NDrawer, NDrawerContent, NEmpty, NSkeleton } from 'naive-ui';
import { useAiChatStore } from '../stores/aiChat';
import { useAiChatVisibility } from '../composables/useAiChatVisibility';
import AiSessionList from '../components/ai/AiSessionList.vue';
import AiChatWindow from '../components/ai/AiChatWindow.vue';

const aiChat = useAiChatStore();
const { visible: aiChatVisible, loaded } = useAiChatVisibility();
const drawerOpen = ref(false);
const isMobile = ref(typeof window !== 'undefined' && window.matchMedia('(max-width: 767px)').matches);

function syncMobile() {
  isMobile.value = window.matchMedia('(max-width: 767px)').matches;
}

async function onSelect(id) {
  await aiChat.selectSession(id);
}

async function onSelectMobile(id) {
  drawerOpen.value = false;
  await onSelect(id);
}

function onNewMobile() {
  aiChat.newSession();
  drawerOpen.value = false;
}

onMounted(async () => {
  syncMobile();
  window.addEventListener('resize', syncMobile);
  if (aiChatVisible.value) {
    await aiChat.hydrateFromBackend();
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile);
});
</script>

<style scoped>
.ai-chat-page-inner {
  max-width: 1100px;
}

.ai-chat-page-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.ai-chat-layout {
  display: flex;
  height: min(720px, calc(100vh - var(--layout-navbar-bottom) - 8rem));
  border: var(--border-brutal);
  border-radius: var(--radius-brutal-card);
  box-shadow: var(--shadow-brutal);
  overflow: hidden;
  background: var(--color-surface);
}

.ai-chat-layout :deep(.ai-session-list) {
  width: 260px;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .ai-chat-layout {
    height: calc(100vh - var(--layout-navbar-bottom) - 6rem);
  }
}

.ai-chat-disabled {
  padding: var(--space-16) 0;
}
</style>
