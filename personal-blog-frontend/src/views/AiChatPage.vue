<template>
  <div class="ai-workspace">
    <header class="ai-workspace-bar">
      <div class="ai-workspace-brand">
        <router-link to="/" class="ai-back" aria-label="返回首页" title="返回首页"><ArrowLeft :size="18" /></router-link>
        <span class="ai-brand-mark"><Sparkles :size="16" /></span>
        <span class="ai-brand-name">InkFlow <strong>AI</strong></span>
      </div>
      <div class="ai-workspace-actions">
        <button type="button" class="ai-tool-btn" title="历史会话" aria-label="历史会话" @click="drawerOpen = true"><History :size="17" /></button>
        <button type="button" class="ai-tool-btn" title="新建对话" aria-label="新建对话" @click="startNew"><SquarePen :size="17" /></button>
        <button type="button" class="ai-tool-btn" :title="isDark ? '切换浅色模式' : '切换深色模式'" :aria-label="isDark ? '切换浅色模式' : '切换深色模式'" @click="toggleDark()"><Sun v-if="isDark" :size="17" /><Moon v-else :size="17" /></button>
        <router-link v-if="authStore.isLoggedIn" to="/user/me" class="ai-account" title="个人中心"><UserAvatar :src="authStore.user?.avatar" :name="authStore.displayName || authStore.user?.username || '我'" :size="28" /></router-link>
        <router-link v-else to="/login" class="ai-login">登录</router-link>
      </div>
    </header>

    <main class="ai-workspace-main">
      <div v-if="!loaded" class="ai-loading"><n-skeleton height="320px" :sharp="false" /></div>
      <div v-else-if="!aiChatVisible" class="ai-disabled"><n-empty description="AI 问答入口已由管理员关闭"><template #extra><router-link to="/"><n-button>返回首页</n-button></router-link></template></n-empty></div>
      <AiChatWindow v-else embedded :visible="true" />
    </main>

    <n-drawer v-model:show="drawerOpen" placement="left" :width="300">
      <n-drawer-content title="历史会话">
        <AiSessionList :sessions="aiChat.sessions" :active-id="aiChat.sessionId" @select="selectSession" @new="startNew" @delete="aiChat.removeSession" />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { NButton, NDrawer, NDrawerContent, NEmpty, NSkeleton } from 'naive-ui';
import { ArrowLeft, History, Moon, Sparkles, SquarePen, Sun } from 'lucide-vue-next';
import { useAiChatStore } from '../stores/aiChat';
import { useAuthStore } from '../stores/auth';
import { useTheme } from '../composables/useTheme';
import { useAiChatVisibility } from '../composables/useAiChatVisibility';
import AiSessionList from '../components/ai/AiSessionList.vue';
import AiChatWindow from '../components/ai/AiChatWindow.vue';
import UserAvatar from '../components/UserAvatar.vue';

const aiChat = useAiChatStore();
const authStore = useAuthStore();
const { isDark, toggleDark } = useTheme();
const { visible: aiChatVisible, loaded } = useAiChatVisibility();
const drawerOpen = ref(false);

async function selectSession(id) {
  drawerOpen.value = false;
  await aiChat.selectSession(id);
}
function startNew() {
  aiChat.newSession();
  drawerOpen.value = false;
}
onMounted(() => {
  if (aiChatVisible.value) aiChat.hydrateFromBackend();
});
</script>

<style scoped>
.ai-workspace { min-height: 100dvh; color: var(--color-text); background: var(--color-page); }
.ai-workspace-bar { position: sticky; top: 0; z-index: 20; display: flex; flex-wrap: nowrap; align-items: center; justify-content: space-between; height: 64px; padding: 0 clamp(16px, 4vw, 48px); border-bottom: 1px solid var(--color-border); background: color-mix(in srgb, var(--color-page) 94%, transparent); backdrop-filter: blur(14px); }
.ai-workspace-brand, .ai-workspace-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 10px; white-space: nowrap; }
.ai-workspace-actions { margin-left: auto; }
.ai-back, .ai-tool-btn { appearance: none; display: grid; place-items: center; width: 34px; height: 34px; padding: 0; border: 0 !important; border-radius: 8px; color: var(--color-text-muted); background: transparent !important; cursor: pointer; }
.ai-back:hover, .ai-tool-btn:hover { color: var(--color-accent); background: var(--color-primary-soft); }
.ai-brand-mark { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; color: var(--color-on-primary); background: var(--color-accent); }
.ai-brand-name { font-size: 15px; letter-spacing: .01em; }
.ai-brand-name strong { font-weight: 700; color: var(--color-accent); }
.ai-account { display: grid; place-items: center; margin-left: 4px; }
.ai-login { padding: 7px 14px; border-radius: 999px; color: var(--color-on-primary); background: var(--color-accent); font-size: 12px; text-decoration: none; }
.ai-workspace-main { width: min(100%, 1120px); min-height: calc(100dvh - 64px); margin: 0 auto; padding: 0 24px 180px; }
.ai-loading { padding-top: 20vh; }
.ai-disabled { padding-top: 20vh; }
@media (max-width: 767px) {
  .ai-workspace-bar { height: 56px; padding: 0 12px; }
  .ai-brand-name { font-size: 14px; }
  .ai-workspace-actions { gap: 2px; }
  .ai-workspace-main { min-height: calc(100dvh - 56px); padding: 0 16px calc(150px + env(safe-area-inset-bottom, 0px)); }
  .ai-login { padding: 6px 10px; }
}
</style>
