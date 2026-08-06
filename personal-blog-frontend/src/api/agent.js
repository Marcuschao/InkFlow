import request from '../utils/request';
import { useAuthStore } from '../stores/auth';

const AGENT_TIMEOUT = 120000;

function unwrapText(payload) {
  if (payload == null) return '';
  if (typeof payload === 'string') return payload;
  if (typeof payload.answer === 'string') return payload.answer;
  if (typeof payload.text === 'string') return payload.text;
  if (typeof payload.content === 'string') return payload.content;
  if (typeof payload.reply === 'string') return payload.reply;
  if (typeof payload.summary === 'string') return payload.summary;
  if (typeof payload.result === 'string') return payload.result;
  return '';
}

function unwrapList(res) {
  const d = res.data;
  if (Array.isArray(d)) return d;
  if (d && Array.isArray(d.articles)) return d.articles;
  if (d && Array.isArray(d.items)) return d.items;
  return [];
}

export function agentEditorOutline(body) {
  return request({
    url: '/agent/editor/outline',
    method: 'post',
    data: body,
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapText(res.data));
}

export function agentEditorContinue(body) {
  return request({
    url: '/agent/editor/continue',
    method: 'post',
    data: body,
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapText(res.data));
}

export function agentEditorPolish(body) {
  return request({
    url: '/agent/editor/polish',
    method: 'post',
    data: body,
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapText(res.data));
}

export function agentSummary(body) {
  return request({
    url: '/agent/summary',
    method: 'post',
    data: body,
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapText(res.data));
}

export function agentSuggestTags(body) {
  return request({
    url: '/agent/tags',
    method: 'post',
    data: body,
    timeout: AGENT_TIMEOUT,
  }).then((res) => {
    const d = res.data;
    if (Array.isArray(d)) return d.map(String);
    if (d && Array.isArray(d.tags)) return d.tags.map(String);
    if (typeof d === 'string') {
      return d
        .split(/[,，]/)
        .map((t) => t.trim())
        .filter(Boolean);
    }
    return [];
  });
}

export function agentRecommend(articleId) {
  return request({
    url: '/agent/recommend',
    method: 'get',
    params: { articleId },
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapList(res));
}

export function agentRecommendContext(payload) {
  return request({
    url: '/agent/recommend/context',
    method: 'post',
    data: payload,
    timeout: AGENT_TIMEOUT,
    skipErrorToast: true,
  }).then((res) => unwrapList(res));
}

export function agentRecommendHome(payload) {
  return request({
    url: '/agent/recommend/home',
    method: 'post',
    data: payload || {},
    timeout: AGENT_TIMEOUT,
    skipErrorToast: true,
  }).then((res) => unwrapList(res));
}

export function agentWeeklyReport(body) {
  return request({
    url: '/agent/weekly-report',
    method: 'post',
    data: body || {},
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapText(res.data));
}

export function buildAgentChatQuestion(msgs) {
  const list = (msgs || []).filter((m) => m.role === 'user' || m.role === 'assistant');
  if (!list.length) return '';
  return list
    .map((m) => (m.role === 'user' ? `用户：${m.content}` : `助手：${m.content}`))
    .join('\n')
    .trim();
}

function chatRequestBody(questionPayload) {
  const question =
    typeof questionPayload === 'string'
      ? questionPayload
      : questionPayload?.question ?? buildAgentChatQuestion(questionPayload?.messages);
  const rawId = questionPayload?.articleId;
  const articleId =
    rawId != null && rawId !== '' && Number.isFinite(Number(rawId)) ? Number(rawId) : undefined;
  const rawSession = questionPayload?.sessionId;
  const sessionId =
    rawSession != null && rawSession !== '' && Number.isFinite(Number(rawSession))
      ? Number(rawSession)
      : undefined;
  const body = { question };
  if (articleId != null) body.articleId = articleId;
  if (sessionId != null) body.sessionId = sessionId;
  return body;
}

export function agentChatFull(questionPayload) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data: chatRequestBody(questionPayload),
    timeout: AGENT_TIMEOUT,
  }).then((res) => res.data);
}

export function agentChatRag(questionPayload) {
  return agentChatFull(questionPayload);
}

export function listAiSessions(params) {
  return request({
    url: '/agent/sessions',
    method: 'get',
    params: params || {},
    timeout: AGENT_TIMEOUT,
  }).then((res) => res.data);
}

export function listAiSessionMessages(sessionId, params) {
  return request({
    url: `/agent/sessions/${sessionId}/messages`,
    method: 'get',
    params: params || {},
    timeout: AGENT_TIMEOUT,
  }).then((res) => res.data);
}

export function deleteAiSession(sessionId, params) {
  return request({
    url: `/agent/sessions/${sessionId}`,
    method: 'delete',
    params: params || {},
    timeout: AGENT_TIMEOUT,
  });
}

export function agentChat(questionPayload) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data: chatRequestBody(questionPayload),
    timeout: AGENT_TIMEOUT,
  }).then((res) => unwrapText(res.data));
}

function apiBase() {
  const base = import.meta.env.VITE_APP_API_BASE_URL || '/api';
  return base.endsWith('/') ? base.slice(0, -1) : base;
}

function parseSseData(raw) {
  if (!raw || raw === '[DONE]') return null;
  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

function dispatchSseEvent(eventName, data, handlers) {
  const name = eventName || 'message';
  if (name === 'delta' || name === 'message') {
    const piece =
      typeof data === 'string'
        ? data
        : data?.delta || data?.content || data?.text || data?.chunk || data?.answer || '';
    if (piece && handlers.onDelta) handlers.onDelta(piece);
    return piece;
  }
  if (name === 'sources' && handlers.onSources) {
    const list = Array.isArray(data) ? data : data?.sources || [];
    handlers.onSources(list);
    return '';
  }
  if (name === 'session' && handlers.onSession) {
    const sid = typeof data === 'number' ? data : data?.sessionId ?? data?.id ?? data;
    if (sid != null) handlers.onSession(sid);
    return '';
  }
  if (name === 'message-id' && handlers.onMessageId) {
    const id = typeof data === 'number' ? data : data?.messageId ?? data?.id ?? data;
    if (id != null) handlers.onMessageId(Number(id));
    return '';
  }
  if (name === 'error' && handlers.onError) {
    const msg = typeof data === 'string' ? data : data?.message || 'Chat failed';
    handlers.onError(msg);
    return '';
  }
  return '';
}

export async function agentChatStream(questionPayload, handlers = {}) {
  const legacyOnDelta = typeof handlers === 'function' ? handlers : handlers.onDelta;
  const h =
    typeof handlers === 'function'
      ? { onDelta: legacyOnDelta }
      : {
          onDelta: handlers.onDelta,
          onSources: handlers.onSources,
          onSession: handlers.onSession,
          onMessageId: handlers.onMessageId,
          onError: handlers.onError,
        };
  const authStore = useAuthStore();
  const body = chatRequestBody(questionPayload);
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  };
  if (authStore.token) {
    headers.Authorization = `Bearer ${authStore.token}`;
  }
  const res = await fetch(`${apiBase()}/agent/chat/stream`, {
    method: 'POST',
    headers,
    credentials: 'same-origin',
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const t = await res.text();
    const err = new Error(t || res.statusText || 'Chat failed');
    if (h.onError) h.onError(err.message);
    throw err;
  }
  const ctype = res.headers.get('content-type') || '';
  if (!ctype.includes('text/event-stream') || !res.body) {
    const json = await res.json().catch(() => null);
    if (json && typeof json.code === 'number' && json.code !== 200) {
      const msg = json.message || 'Chat failed';
      if (h.onError) h.onError(msg);
      throw new Error(msg);
    }
    const inner = json && json.data !== undefined ? json.data : json;
    const text = unwrapText(inner);
    if (text && h.onDelta) h.onDelta(text);
    if (inner?.sources && h.onSources) h.onSources(inner.sources);
    if (inner?.sessionId != null && h.onSession) h.onSession(inner.sessionId);
    if (inner?.messageId != null && h.onMessageId) h.onMessageId(inner.messageId);
    return { answer: text, sources: inner?.sources || [], sessionId: inner?.sessionId ?? null, messageId: inner?.messageId ?? null };
  }
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let full = '';
  let sources = [];
  let sessionId = null;
  let messageId = null;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const parts = buffer.split('\n\n');
    buffer = parts.pop() || '';
    for (const block of parts) {
      if (!block.trim()) continue;
      const lines = block.split('\n');
      let eventName = 'message';
      const dataLines = [];
      for (const line of lines) {
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim());
        }
      }
      const raw = dataLines.join('\n');
      const parsed = parseSseData(raw);
      const piece = dispatchSseEvent(eventName, parsed, {
        onDelta: (p) => {
          full += p;
          if (h.onDelta) h.onDelta(p);
        },
        onSources: (list) => {
          sources = list;
          if (h.onSources) h.onSources(list);
        },
        onSession: (sid) => {
          sessionId = sid;
          if (h.onSession) h.onSession(sid);
        },
        onMessageId: (id) => {
          messageId = id;
          if (h.onMessageId) h.onMessageId(id);
        },
        onError: h.onError,
      });
      if (eventName === 'delta' && piece) full += piece;
    }
  }
  return { answer: full, sources, sessionId, messageId };
}
