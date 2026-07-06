import { defineStore } from 'pinia';
import {
  agentChatStream,
  listAiSessions,
  listAiSessionMessages,
  deleteAiSession,
} from '../api/agent';
import { stripCitationMarkers } from '../utils/aiChatFormat';
import { copyTextToClipboard } from '../utils/clipboard';
import { useAuthStore } from './auth';
import { useToastStore } from './toast';

const STORAGE_KEY = 'aiChat.session';
const SESSION_IDS_KEY = 'aiChat.knownSessionIds';
const MAX_MESSAGES = 80;
const MAX_KNOWN_SESSIONS = 100;

function loadPersisted() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return { sessionId: null, messages: [] };
    const parsed = JSON.parse(raw);
    return {
      sessionId: parsed.sessionId ?? null,
      messages: Array.isArray(parsed.messages) ? parsed.messages : [],
    };
  } catch {
    return { sessionId: null, messages: [] };
  }
}

function loadKnownSessionIds() {
  try {
    const raw = localStorage.getItem(SESSION_IDS_KEY);
    const ids = raw ? JSON.parse(raw) : [];
    return Array.isArray(ids) ? ids.map(Number).filter(Boolean) : [];
  } catch {
    return [];
  }
}

function saveKnownSessionIds(ids) {
  try {
    localStorage.setItem(SESSION_IDS_KEY, JSON.stringify(ids.slice(0, MAX_KNOWN_SESSIONS)));
  } catch {
    /* ignore */
  }
}

function normalizeSource(s, idx) {
  if (!s) return null;
  return {
    id: s.id ?? s.docId ?? null,
    title: s.title ?? s.docTitle ?? '',
    chunkId: s.chunkId ?? null,
    ordinal: s.ordinal ?? idx + 1,
    snippet: s.snippet ?? '',
    score: s.score ?? null,
    link: s.link ?? (s.id ? `/article/${s.id}` : ''),
  };
}

export const useAiChatStore = defineStore('aiChat', {
  state: () => {
    const persisted = loadPersisted();
    return {
      open: false,
      sessionId: persisted.sessionId,
      messages: persisted.messages.slice(-MAX_MESSAGES),
      streaming: false,
      draftQuestion: '',
      sessions: [],
      sessionsLoaded: false,
      lastError: '',
      hasNew: false,
      context: {
        articleId: null,
        keyword: null,
        tagLabel: null,
      },
      _abortController: null,
    };
  },
  getters: {
    currentSessionTitle(state) {
      if (!state.sessionId) return '新对话';
      const s = state.sessions.find((x) => x.id === state.sessionId);
      return s?.title || '对话';
    },
  },
  actions: {
    guestSessionParams() {
      const ids = loadKnownSessionIds();
      return ids.length ? { ids: ids.join(',') } : {};
    },
    addKnownSessionId(id) {
      const sid = Number(id);
      if (!sid) return;
      const ids = loadKnownSessionIds().filter((x) => x !== sid);
      ids.unshift(sid);
      saveKnownSessionIds(ids);
    },
    removeKnownSessionId(id) {
      saveKnownSessionIds(loadKnownSessionIds().filter((x) => x !== Number(id)));
    },
    persist() {
      try {
        localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({
            sessionId: this.sessionId,
            messages: this.messages.slice(-MAX_MESSAGES).map(({ role, content, sources }) => ({
              role,
              content,
              ...(sources?.length ? { sources } : {}),
            })),
          })
        );
      } catch {
        /* ignore */
      }
    },
    toggleWindow() {
      if (this.open) this.closeWindow();
      else this.openWindow();
    },
    openWindow(ctx) {
      if (ctx) {
        if (ctx.articleId != null) this.context.articleId = Number(ctx.articleId);
        if (ctx.keyword) this.context.keyword = ctx.keyword;
        if (ctx.tagLabel) this.context.tagLabel = ctx.tagLabel;
        if (ctx.draftQuestion) this.draftQuestion = ctx.draftQuestion;
      }
      this.open = true;
      this.hasNew = false;
      this.loadSessions();
    },
    closeWindow() {
      this.open = false;
    },
    setDraft(text) {
      this.draftQuestion = text || '';
    },
    clearContext() {
      this.context = { articleId: null, keyword: null, tagLabel: null };
    },
    buildPayload(question) {
      const payload = { question, sessionId: this.sessionId ?? undefined };
      if (this.context.articleId != null) payload.articleId = this.context.articleId;
      return payload;
    },
    async send(question, options = {}) {
      const text = (question || this.draftQuestion || '').trim();
      if (!text || this.streaming) return;
      this.draftQuestion = '';
      this.lastError = '';
      if (!options.regenerate) {
        this.messages.push({ role: 'user', content: text });
        this.messages = this.messages.slice(-MAX_MESSAGES);
      }
      this.messages.push({ role: 'assistant', content: '', sources: [], streaming: true });
      const assistantIdx = this.messages.length - 1;
      this.streaming = true;
      this._abortController = new AbortController();

      try {
        await agentChatStream(this.buildPayload(text), {
          onDelta: (piece) => {
            this.messages[assistantIdx].content += piece;
          },
          onSources: (list) => {
            this.messages[assistantIdx].sources = (list || [])
              .map((s, i) => normalizeSource(s, i))
              .filter(Boolean);
          },
          onSession: (sid) => {
            this.sessionId = Number(sid);
            this.addKnownSessionId(sid);
            this.persist();
          },
          onError: (msg) => {
            this.lastError = msg;
          },
        });
      } catch (e) {
        this.messages[assistantIdx].content =
          this.messages[assistantIdx].content || e?.message || '请求失败';
        this.lastError = e?.message || '请求失败';
      } finally {
        const msg = this.messages[assistantIdx];
        if (msg?.content) {
          msg.content = stripCitationMarkers(msg.content);
        }
        msg.streaming = false;
        this.streaming = false;
        this.messages = [...this.messages.slice(-MAX_MESSAGES)];
        this.persist();
        this.clearContext();
        if (!this.open) this.hasNew = true;
        await this.loadSessions();
      }
    },
    async loadSessions() {
      const authStore = useAuthStore();
      const params = { page: 1, size: 50, ...this.guestSessionParams() };
      if (!authStore.isLoggedIn && !params.ids) {
        this.sessions = [];
        this.sessionsLoaded = true;
        return;
      }
      try {
        const data = await listAiSessions(params);
        this.sessions = data?.records || [];
        this.sessionsLoaded = true;
      } catch {
        this.sessions = [];
      }
    },
    async selectSession(id) {
      if (!id) return;
      this.sessionId = Number(id);
      try {
        const rows = await listAiSessionMessages(id, this.guestSessionParams());
        this.messages = (rows || []).map((m) => ({
          role: m.role,
          content: m.role === 'assistant' ? stripCitationMarkers(m.content || '') : (m.content || ''),
          sources: (() => {
            if (!m.sources) return [];
            if (typeof m.sources === 'string') {
              try {
                const parsed = JSON.parse(m.sources);
                return Array.isArray(parsed) ? parsed.map((s, i) => normalizeSource(s, i)).filter(Boolean) : [];
              } catch {
                return [];
              }
            }
            return Array.isArray(m.sources)
              ? m.sources.map((s, i) => normalizeSource(s, i)).filter(Boolean)
              : [];
          })(),
        }));
        this.persist();
      } catch (e) {
        this.lastError = e?.message || '加载消息失败';
      }
    },
    newSession() {
      this.sessionId = null;
      this.messages = [];
      this.draftQuestion = '';
      this.persist();
    },
    async removeSession(id) {
      try {
        await deleteAiSession(id, this.guestSessionParams());
        this.removeKnownSessionId(id);
        this.sessions = this.sessions.filter((s) => s.id !== id);
        if (this.sessionId === id) this.newSession();
      } catch (e) {
        this.lastError = e?.message || '删除失败';
      }
    },
    async regenerate() {
      if (this.streaming) return;
      let lastUserIdx = -1;
      for (let i = this.messages.length - 1; i >= 0; i--) {
        if (this.messages[i].role === 'user') {
          lastUserIdx = i;
          break;
        }
      }
      if (lastUserIdx < 0) return;
      const text = this.messages[lastUserIdx].content;
      this.messages = this.messages.slice(0, lastUserIdx + 1);
      await this.send(text, { regenerate: true });
    },
    async hydrateFromBackend() {
      if (this.sessionId) {
        this.addKnownSessionId(this.sessionId);
      }
      await this.loadSessions();
      if (this.sessionId) {
        await this.selectSession(this.sessionId);
      }
    },
    async copyMessage(content) {
      if (!content) return;
      const ok = await copyTextToClipboard(content);
      const toast = useToastStore();
      toast.push(ok ? '已复制' : '复制失败', ok ? 'success' : 'error');
    },
  },
});
