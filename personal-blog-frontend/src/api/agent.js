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
  }).then((res) => unwrapList(res));
}

export function agentRecommendHome(payload) {
  return request({
    url: '/agent/recommend/home',
    method: 'post',
    data: payload || {},
    timeout: AGENT_TIMEOUT,
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
  return articleId != null ? { question, articleId } : { question };
}

export function agentChatFull(questionPayload) {
  return request({
    url: '/agent/chat',
    method: 'post',
    data: chatRequestBody(questionPayload),
    timeout: AGENT_TIMEOUT,
  }).then((res) => res.data);
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

export async function agentChatStream(questionPayload, onDelta) {
  const authStore = useAuthStore();
  const body = chatRequestBody(questionPayload);
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
  };
  if (authStore.token) {
    headers.Authorization = `Bearer ${authStore.token}`;
  }
  const res = await fetch(`${apiBase()}/agent/chat`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ...body, stream: true }),
  });
  if (!res.ok) {
    const t = await res.text();
    throw new Error(t || res.statusText || 'Chat failed');
  }
  const ctype = res.headers.get('content-type') || '';
  if (!ctype.includes('text/event-stream') || !res.body) {
    const json = await res.json().catch(() => null);
    if (json && typeof json.code === 'number' && json.code !== 200) {
      throw new Error(json.message || 'Chat failed');
    }
    const inner = json && json.data !== undefined ? json.data : json;
    const text = unwrapText(inner);
    if (text) onDelta(text);
    return text;
  }
  const reader = res.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let full = '';
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const parts = buffer.split('\n\n');
    buffer = parts.pop() || '';
    for (const block of parts) {
      const lines = block.split('\n');
      for (const line of lines) {
        if (!line.startsWith('data:')) continue;
        const raw = line.slice(5).trim();
        if (!raw || raw === '[DONE]') continue;
        try {
          const parsed = JSON.parse(raw);
          const piece =
            typeof parsed === 'string'
              ? parsed
              : parsed.delta ||
                parsed.content ||
                parsed.text ||
                parsed.chunk ||
                parsed.answer ||
                '';
          if (piece) {
            full += piece;
            onDelta(piece);
          }
        } catch {
          full += raw;
          onDelta(raw);
        }
      }
    }
  }
  return full;
}
