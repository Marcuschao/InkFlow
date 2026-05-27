import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_PATH = '/ws';
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000];
const MAX_RECONNECT = 5;

let client = null;
let currentToken = null;
let reconnectAttempt = 0;
let reconnectTimer = null;
let intentionalDisconnect = false;
let starting = false;

const chatHandlers = new Set();
const notificationHandlers = new Set();
const statusHandlers = new Set();

function wsUrl() {
  const base = import.meta.env.VITE_APP_WS_BASE_URL;
  if (base) return `${base}${WS_PATH}`;
  if (typeof window !== 'undefined') {
    return `${window.location.origin}${WS_PATH}`;
  }
  return WS_PATH;
}

function notifyStatus(connected) {
  statusHandlers.forEach((fn) => fn(connected));
}

function clearReconnectTimer() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
}

function scheduleReconnect() {
  if (intentionalDisconnect || reconnectAttempt >= MAX_RECONNECT) {
    return;
  }
  const delay = RECONNECT_DELAYS[Math.min(reconnectAttempt, RECONNECT_DELAYS.length - 1)];
  reconnectAttempt += 1;
  clearReconnectTimer();
  reconnectTimer = setTimeout(() => {
    if (!intentionalDisconnect) {
      startClient();
    }
  }, delay);
}

function subscribeTopics() {
  if (!client?.connected) return;
  client.subscribe('/topic/chat', (frame) => {
    try {
      const payload = JSON.parse(frame.body);
      chatHandlers.forEach((fn) => fn(payload));
    } catch {
      /* ignore */
    }
  });
  if (currentToken) {
    client.subscribe('/user/queue/notifications', (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        notificationHandlers.forEach((fn) => fn(payload));
      } catch {
        /* ignore */
      }
    });
  }
}

async function startClient() {
  if (starting) return;
  starting = true;
  try {
    if (client?.active) {
      intentionalDisconnect = true;
      await client.deactivate();
      intentionalDisconnect = false;
      client = null;
    }
    const headers = {};
    if (currentToken) {
      headers.Authorization = `Bearer ${currentToken}`;
      headers.token = currentToken;
    }
    client = new Client({
      webSocketFactory: () => new SockJS(wsUrl()),
      connectHeaders: headers,
      reconnectDelay: 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        reconnectAttempt = 0;
        notifyStatus(true);
        subscribeTopics();
      },
      onStompError: () => {
        notifyStatus(false);
        scheduleReconnect();
      },
      onWebSocketClose: () => {
        notifyStatus(false);
        if (!intentionalDisconnect) {
          scheduleReconnect();
        }
      },
    });
    client.activate();
  } finally {
    starting = false;
  }
}

export async function connect(token) {
  const nextToken = token || null;
  if (nextToken === currentToken && client?.connected) {
    return;
  }
  currentToken = nextToken;
  reconnectAttempt = 0;
  intentionalDisconnect = false;
  clearReconnectTimer();
  await startClient();
}

export function disconnect() {
  intentionalDisconnect = true;
  reconnectAttempt = MAX_RECONNECT;
  clearReconnectTimer();
  currentToken = null;
  if (client) {
    client.deactivate();
    client = null;
  }
  notifyStatus(false);
}

export function sendChat(content) {
  if (!client?.connected) {
    throw new Error('未连接');
  }
  if (!currentToken) {
    throw new Error('未登录');
  }
  client.publish({
    destination: '/app/chat',
    body: JSON.stringify({ content }),
    headers: { 'content-type': 'application/json' },
  });
}

export function onChatMessage(handler) {
  chatHandlers.add(handler);
  return () => chatHandlers.delete(handler);
}

export function onNotification(handler) {
  notificationHandlers.add(handler);
  return () => notificationHandlers.delete(handler);
}

export function onStatusChange(handler) {
  statusHandlers.add(handler);
  return () => statusHandlers.delete(handler);
}

export function isConnected() {
  return !!client?.connected;
}
