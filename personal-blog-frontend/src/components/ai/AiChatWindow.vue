<template>
  <div
    v-if="visible"
    class="ai-chat-window"
    :class="{ 'ai-chat-window--embedded': embedded, 'ai-chat-window--floating': !embedded }"
    :style="floatingStyle"
    role="dialog"
    aria-label="AI 知识助手"
  >
    <header v-if="!embedded" class="ai-chat-header" @mousedown="onDragStart">
      <div class="ai-chat-identity">
        <div class="ai-chat-avatar"><Sparkles :size="17" /></div>
        <div>
          <div class="ai-chat-brand">InkFlow AI</div>
          <div class="ai-chat-status"><span></span> 站内知识助手</div>
        </div>
      </div>
      <div class="ai-chat-header-actions">
        <span v-if="aiChat.currentSessionTitle !== '新对话'" class="ai-chat-session-name">{{ aiChat.currentSessionTitle }}</span>
        <button v-if="!embedded" type="button" class="ai-chat-icon-btn" title="历史会话" aria-label="历史会话" @click="historyOpen = true"><History :size="17" /></button>
        <button type="button" class="ai-chat-icon-btn" title="新建对话" aria-label="新建对话" @click="aiChat.newSession()"><SquarePen :size="17" /></button>
        <button v-if="!embedded" type="button" class="ai-chat-icon-btn" title="关闭" aria-label="关闭" @click="aiChat.closeWindow()"><X :size="18" /></button>
      </div>
    </header>

    <div v-if="aiChat.lastError" class="ai-chat-error" role="status">{{ aiChat.lastError }}</div>

    <div ref="scrollRef" class="ai-chat-messages">
      <div v-if="showEmpty" class="ai-chat-empty">
        <span class="ai-empty-kicker">INKFLOW AI</span>
        <h2>今天想从 InkFlow 里找到什么？</h2>
        <p>从站内文章与知识文档中检索答案，并保留可追溯的依据。</p>
        <div class="ai-chat-samples">
          <button v-for="q in sampleQuestions" :key="q.text" type="button" class="ai-chat-sample-btn" @click="onSample(q.text)">
            <component :is="q.icon" :size="16" />
            <span>{{ q.text }}</span>
            <ArrowUpRight :size="14" />
          </button>
        </div>
      </div>

      <div v-for="(m, idx) in aiChat.messages" :key="m.id || idx" class="ai-msg-row" :class="m.role">
        <div v-if="m.role === 'user'" class="ai-user-label">你</div>
        <div v-if="m.role === 'user'" class="ai-bubble user">{{ m.content || '' }}</div>
        <div v-else class="ai-assistant-message">
          <div class="ai-assistant-meta"><div class="ai-mini-avatar"><Sparkles :size="13" /></div><span>InkFlow AI</span><span v-if="m.streaming" class="ai-thinking"><i></i> 正在查阅知识库</span></div>
          <div v-if="m.streaming && !m.content" class="ai-loading-line"><span></span><span></span><span></span></div>
          <MarkdownRenderer v-else :markdown="displayContent(m)" :decorate-citations="true" class="ai-answer-markdown" />
          <div v-if="m.role === 'assistant' && m.sources?.length && !m.streaming" class="ai-evidence">
            <button type="button" class="ai-evidence-toggle" :aria-expanded="isSourcesOpen(idx)" @click="toggleSources(idx)">
              <span>[{{ m.sources.length }}] 查看回答依据</span><ChevronDown :size="15" :class="{ rotated: isSourcesOpen(idx) }" />
            </button>
            <div v-if="isSourcesOpen(idx)" class="ai-evidence-grid">
              <AiSourceCard v-for="(source, sourceIdx) in m.sources.slice(0, 4)" :key="source.chunkId || source.id || sourceIdx" :source="source" :index="sourceIdx + 1" />
            </div>
          </div>
          <div v-if="m.content && !m.streaming" class="ai-msg-actions">
            <AiFeedbackButtons :message="m" />
            <span class="ai-action-spacer"></span>
            <button type="button" class="ai-action-btn" :title="copiedMessageKey === (m.id || idx) ? '已复制' : '复制回答'" @click="copyAnswer(m, m.id || idx)"><Copy :size="14" />{{ copiedMessageKey === (m.id || idx) ? '已复制' : '复制' }}</button>
            <button v-if="idx === aiChat.messages.length - 1" type="button" class="ai-action-btn" title="重新生成" @click="aiChat.regenerate()"><RotateCcw :size="14" />重试</button>
          </div>
        </div>
      </div>
    </div>

    <form class="ai-chat-composer" @submit.prevent="submit">
      <div class="ai-composer-shell">
        <textarea ref="inputRef" v-model="draft" class="ai-chat-input" rows="1" maxlength="4000" placeholder="向 InkFlow 提问……" @input="autoResizeInput" @keydown="onKeydown" />
        <div class="ai-composer-bottom">
          <span class="ai-composer-note"><LockKeyhole :size="12" /> 仅检索站内公开内容</span>
          <button class="ai-send-btn" type="submit" aria-label="发送问题" title="发送" :disabled="!draft.trim() || aiChat.streaming"><ArrowUp :size="18" /></button>
        </div>
      </div>
    </form>

    <n-drawer v-if="!embedded" v-model:show="historyOpen" placement="left" :width="286">
      <n-drawer-content title="历史会话"><AiSessionList :sessions="aiChat.sessions" :active-id="aiChat.sessionId" @select="onSelectHistory" @new="onNewHistory" @delete="aiChat.removeSession" /></n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, defineAsyncComponent } from 'vue';
import { NDrawer, NDrawerContent } from 'naive-ui';
import { ArrowUp, ArrowUpRight, BookOpen, ChevronDown, Copy, FileSearch, History, Lightbulb, LockKeyhole, RotateCcw, Sparkles, SquarePen, X } from 'lucide-vue-next';
import { useAiChatStore } from '../../stores/aiChat';
import AiSessionList from './AiSessionList.vue';
import AiSourceCard from './AiSourceCard.vue';
import AiFeedbackButtons from './AiFeedbackButtons.vue';

const props = defineProps({ embedded: { type: Boolean, default: false }, visible: { type: Boolean, default: true } });
const MarkdownRenderer = defineAsyncComponent(() => import('../MarkdownRenderer.vue'));
const aiChat = useAiChatStore();
const scrollRef = ref(null);
const inputRef = ref(null);
const draft = ref('');
const copiedMessageKey = ref(null);
const historyOpen = ref(false);
const openSourceIndexes = ref(new Set());
const sampleQuestions = [
  { text: '博客有哪些技术主题？', icon: BookOpen },
  { text: '如何开始学习微服务？', icon: Lightbulb },
  { text: '最近更新了哪些内容？', icon: FileSearch },
];
const showEmpty = computed(() => !aiChat.messages.length && !aiChat.streaming);

watch(() => aiChat.draftQuestion, (v) => { if (v) { draft.value = v; nextTick(autoResizeInput); } }, { immediate: true });
watch(() => aiChat.messages, () => nextTick(scrollToBottom), { deep: true });
watch(() => props.visible, (v) => { if (v) nextTick(scrollToBottom); });

function displayContent(m) { return m.content || ''; }
function isSourcesOpen(index) { return openSourceIndexes.value.has(index); }
function toggleSources(index) {
  const next = new Set(openSourceIndexes.value);
  if (next.has(index)) next.delete(index); else next.add(index);
  openSourceIndexes.value = next;
}
function isNearPageBottom() {
  const root = document.documentElement;
  return root.scrollHeight - (window.scrollY + window.innerHeight) < 260;
}
function scrollToBottom() {
  if (!isNearPageBottom()) return;
  window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'smooth' });
}
function autoResizeInput() {
  const input = inputRef.value;
  if (!input) return;
  const maxHeight = window.innerWidth <= 767 ? 160 : 190;
  input.style.height = 'auto';
  const nextHeight = Math.min(Math.max(input.scrollHeight, 52), maxHeight);
  input.style.height = `${nextHeight}px`;
  input.style.overflowY = input.scrollHeight > maxHeight ? 'auto' : 'hidden';
}
function onSample(q) { draft.value = q; nextTick(autoResizeInput); submit(); }
function onKeydown(e) { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit(); } }
async function submit() { const text = draft.value.trim(); if (!text || aiChat.streaming) return; draft.value = ''; nextTick(autoResizeInput); await aiChat.send(text); }
async function copyAnswer(message, key) {
  const ok = await aiChat.copyMessage(displayContent(message));
  if (!ok) return;
  copiedMessageKey.value = key;
  window.setTimeout(() => {
    if (copiedMessageKey.value === key) copiedMessageKey.value = null;
  }, 1800);
}
async function onSelectHistory(id) { historyOpen.value = false; await aiChat.selectSession(id); }
function onNewHistory() { aiChat.newSession(); historyOpen.value = false; }

const pos = ref({ x: null, y: null });
const dragging = ref(false);
const dragOffset = { x: 0, y: 0 };
const floatingStyle = computed(() => props.embedded || pos.value.x == null ? {} : { left: `${pos.value.x}px`, top: `${pos.value.y}px`, right: 'auto', bottom: 'auto' });
function onDragStart(e) {
  if (props.embedded || e.target.closest('button')) return;
  dragging.value = true;
  const rect = e.currentTarget.parentElement.getBoundingClientRect();
  dragOffset.x = e.clientX - rect.left; dragOffset.y = e.clientY - rect.top;
  document.addEventListener('mousemove', onDragMove); document.addEventListener('mouseup', onDragEnd);
}
function onDragMove(e) { if (dragging.value) pos.value = { x: Math.max(8, e.clientX - dragOffset.x), y: Math.max(8, e.clientY - dragOffset.y) }; }
function onDragEnd() { dragging.value = false; document.removeEventListener('mousemove', onDragMove); document.removeEventListener('mouseup', onDragEnd); }
onMounted(() => { if (props.embedded) aiChat.hydrateFromBackend(); });
</script>

<style scoped>
.ai-chat-window { display:flex; flex-direction:column; min-width:0; overflow:visible; background:transparent; font-family:var(--font-ui); }
.ai-chat-window--embedded { flex: none; height:auto; min-height:0; }
.ai-chat-window--floating { position:fixed; right:var(--space-4); bottom:calc(var(--ai-fab-size) + var(--space-6) + env(safe-area-inset-bottom, 0px)); z-index:1295; width:min(760px, calc(100vw - var(--space-8))); height:min(760px, calc(100vh - 8rem)); overflow:hidden; border-radius:var(--radius-brutal-card); box-shadow:var(--shadow-brutal-lg); animation:ai-slide-in .25s var(--ease-out-soft); }
.ai-chat-window--floating .ai-chat-messages { flex:1; min-height:0; overflow-y:auto; padding:32px 24px 28px; }
.ai-chat-header { display:flex; align-items:center; justify-content:space-between; gap:var(--space-3); padding:0 0 18px; border-bottom:1px solid var(--color-border-strong); background:transparent; cursor:grab; user-select:none; }
.ai-chat-identity,.ai-chat-header-actions,.ai-chat-status,.ai-assistant-meta,.ai-evidence-head,.ai-composer-bottom,.ai-msg-actions { display:flex; align-items:center; }
.ai-chat-identity { gap:10px; min-width:0; }
.ai-chat-avatar,.ai-mini-avatar,.ai-empty-mark { display:grid; place-items:center; flex-shrink:0; color:var(--color-on-primary); background:var(--color-accent); }
.ai-chat-avatar { width:34px; height:34px; border-radius:var(--radius-sm); }
.ai-chat-brand { color:var(--color-text); font-size:var(--text-sm); font-weight:var(--weight-bold); }
.ai-chat-status { gap:5px; margin-top:2px; color:var(--color-text-soft); font-size:11px; }
.ai-chat-status span { width:6px; height:6px; border-radius:50%; background:var(--color-success); }
.ai-chat-session-name { max-width:180px; overflow:hidden; color:var(--color-text-muted); font-size:var(--text-xs); text-overflow:ellipsis; white-space:nowrap; }
.ai-chat-header-actions { gap:3px; }
.ai-chat-icon-btn { display:grid; place-items:center; width:32px; height:32px; border:0; border-radius:var(--radius-sm); color:var(--color-text-muted); background:transparent; cursor:pointer; }
.ai-chat-icon-btn:hover { color:var(--color-accent); background:var(--color-primary-soft); }
.ai-chat-error { padding:9px 20px; border-bottom:1px solid var(--color-danger); color:var(--color-danger); background:var(--color-danger-soft); font-size:var(--text-xs); }
.ai-chat-messages { padding:72px 48px 28px; background:transparent; }
.ai-chat-empty { max-width:680px; margin:12vh auto 0; text-align:center; }
.ai-empty-kicker { color:var(--color-accent); font-family:var(--font-mono); font-size:10px; font-weight:var(--weight-bold); letter-spacing:.08em; }
.ai-chat-empty h2 { margin:10px 0 8px; color:var(--color-text); font-family:var(--font-display); font-size:clamp(25px,3vw,36px); font-weight:600; }
.ai-chat-empty p { max-width:430px; margin:0 auto 30px; color:var(--color-text-muted); font-size:var(--text-sm); line-height:1.7; }
.ai-chat-samples { display:grid; grid-template-columns:1fr; gap:0; max-width:560px; margin:0 auto; }
.ai-chat-sample-btn { display:flex; align-items:center; gap:10px; min-height:52px; padding:12px 4px; border:0; border-bottom:1px solid var(--color-border); border-radius:0; color:var(--color-text); background:transparent; font:inherit; font-size:var(--text-xs); text-align:left; cursor:pointer; transition:color var(--transition-fast),padding-left var(--transition-fast); }
.ai-chat-sample-btn svg:last-child { margin-left:auto; color:var(--color-text-soft); }
.ai-chat-sample-btn:hover { color:var(--color-accent); padding-left:8px; }
.ai-msg-row { display:flex; width:100%; max-width:900px; margin:0 auto 34px; }
.ai-msg-row.user { flex-direction:column; align-items:flex-end; }
.ai-user-label { margin-bottom:7px; color:var(--color-text-soft); font-size:13px; font-weight:var(--weight-semibold); }
.ai-bubble.user { max-width:min(78%,560px); padding:11px 15px; border:1px solid var(--color-accent); border-radius:12px 12px 3px 12px; color:var(--color-on-primary); background:var(--color-accent); line-height:1.6; white-space:pre-wrap; word-break:break-word; }
.ai-assistant-message { width:100%; max-width:760px; }
.ai-assistant-meta { gap:8px; margin-bottom:10px; color:var(--color-text-muted); font-size:13px; font-weight:var(--weight-semibold); }
.ai-mini-avatar { width:26px; height:26px; border-radius:50%; }
.ai-thinking { display:inline-flex; align-items:center; gap:5px; margin-left:6px; color:var(--color-accent); font-weight:400; }
.ai-thinking i { width:6px; height:6px; border-radius:50%; background:var(--color-accent); animation:ai-pulse 1s ease-in-out infinite; }
.ai-answer-markdown { padding:0 0 1px; }
.ai-answer-markdown :deep(.markdown-prose) { font-size:15px; line-height:1.75; color:var(--color-text); }
.ai-answer-markdown :deep(.markdown-prose p) { margin:0 0 12px; }
.ai-answer-markdown :deep(.markdown-prose p:last-child) { margin-bottom:0; }
.ai-answer-markdown :deep(.markdown-prose h1),.ai-answer-markdown :deep(.markdown-prose h2),.ai-answer-markdown :deep(.markdown-prose h3) { margin-top:18px; margin-bottom:8px; font-size:1.08em; }
.ai-answer-markdown :deep(.markdown-prose pre) { margin:12px 0; }
.ai-loading-line { display:flex; gap:6px; padding:12px 0; }
.ai-loading-line span { width:7px; height:7px; border-radius:50%; background:var(--color-accent); animation:ai-bounce 1s infinite; }
.ai-loading-line span:nth-child(2) { animation-delay:.15s; }.ai-loading-line span:nth-child(3) { animation-delay:.3s; }
.ai-evidence { margin-top:28px; padding-top:16px; border-top:1px solid var(--color-border); }
.ai-evidence-toggle { display:inline-flex; align-items:center; gap:7px; padding:0; border:0; color:var(--color-text-muted); background:transparent; font:inherit; font-size:11px; cursor:pointer; }
.ai-evidence-toggle:hover { color:var(--color-accent); }
.ai-evidence-toggle svg { transition:transform var(--transition-fast); }
.ai-evidence-toggle svg.rotated { transform:rotate(180deg); }
.ai-evidence-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px 28px; margin-top:14px; }
.ai-evidence-grid :deep(.ai-source-card) { min-width:0; padding:0 0 12px; border:0; border-bottom:1px solid var(--color-border); border-radius:0; background:transparent; }
.ai-evidence-grid :deep(.ai-source-snippet) { -webkit-line-clamp:2; }
.ai-msg-actions { gap:12px; margin-top:12px; }
.ai-action-spacer { flex:1; }
.ai-action-btn { display:inline-flex; align-items:center; gap:5px; padding:0; border:0; color:var(--color-text-soft); background:transparent; font:inherit; font-size:11px; cursor:pointer; }
.ai-action-btn:hover { color:var(--color-accent); }
.ai-chat-composer { position:sticky; bottom:0; z-index:30; width:auto; transform:none; padding:14px 0 max(18px, env(safe-area-inset-bottom, 0px)); border-top:0; background:linear-gradient(to bottom, transparent, var(--color-page) 28%); }
.ai-chat-window--embedded .ai-chat-composer { position:fixed; left:50%; bottom:0; width:min(860px, calc(100vw - 48px)); transform:translateX(-50%); }
.ai-composer-shell { border:1px solid var(--color-border-strong); border-radius:var(--radius-md); background:var(--surface-input); transition:border-color var(--transition-fast),box-shadow var(--transition-fast); }
.ai-composer-shell:focus-within { border-color:var(--color-accent); box-shadow:0 0 0 3px var(--color-primary-soft); }
.ai-chat-input { display:block; width:100%; min-height:52px; max-height:190px; padding:14px 15px 4px; resize:none; overflow-y:hidden; border:0; outline:0; color:var(--color-text); background:transparent; font:inherit; font-size:14px; line-height:1.5; transition:height .16s ease; }
.ai-chat-input::placeholder { color:var(--color-text-soft); }
.ai-composer-bottom { justify-content:space-between; padding:4px 8px 8px 14px; }
.ai-composer-note { display:inline-flex; align-items:center; gap:5px; color:var(--color-text-soft); font-size:10px; }
.ai-send-btn { display:grid; place-items:center; width:32px; height:32px; border:0; border-radius:var(--radius-sm); color:var(--color-on-primary); background:var(--color-accent); cursor:pointer; }
.ai-send-btn:disabled { opacity:.35; cursor:not-allowed; }
@keyframes ai-slide-in { from { opacity:0; transform:translateY(12px); } to { opacity:1; transform:translateY(0); } }
@keyframes ai-pulse { 50% { opacity:.35; transform:scale(.7); } }
@keyframes ai-bounce { 0%,60%,100% { transform:translateY(0); opacity:.5; } 30% { transform:translateY(-5px); opacity:1; } }
@media (max-width:767px) {
  .ai-chat-window--floating { right:0; left:0; bottom:0; width:100%; height:calc(100dvh - var(--layout-navbar-bottom)); border-radius:var(--radius-brutal-card) var(--radius-brutal-card) 0 0; }
  .ai-chat-header { padding:0 0 14px; }.ai-chat-session-name { display:none; }
  .ai-chat-messages { padding:46px 8px 30px; }.ai-chat-empty { margin-top:8vh; }.ai-chat-samples { grid-template-columns:1fr; }.ai-chat-sample-btn { min-height:52px; }
  .ai-bubble.user { max-width:88%; }.ai-evidence-grid { grid-template-columns:1fr; }.ai-chat-composer { padding:10px 0 max(12px, env(safe-area-inset-bottom, 0px)); }.ai-chat-window--embedded .ai-chat-composer { width:calc(100vw - 32px); }.ai-chat-input { max-height:160px; }
}
@media (prefers-reduced-motion:reduce) { .ai-chat-window,.ai-chat-sample-btn,.ai-thinking i,.ai-loading-line span { animation:none; transition:none; } }
</style>
