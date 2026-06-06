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
const subscriptions = [];

const chatHandlers = new Set();
const ackHandlers = new Set();
const offlineHandlers = new Set();
const recallHandlers = new Set();
const profileHandlers = new Set();
const notificationHandlers = new Set();
const statusHandlers = new Set();

function wsUrl() {
  const base = import.meta.env.VITE_APP_WS_BASE_URL;
  if (base) {
    if (base.startsWith('http://') || base.startsWith('https://')) {
      return `${base.replace(/\/$/, '')}${WS_PATH}`;
    }
    if (typeof window !== 'undefined') {
      const path = (base.startsWith('/') ? base : `/${base}`).replace(/\/$/, '');
      return `${window.location.origin}${path}${WS_PATH}`;
    }
    return `${base}${WS_PATH}`;
  }
  if (typeof window !== 'undefined') {
    const apiBase = import.meta.env.VITE_APP_API_BASE_URL || '';
    if (apiBase.startsWith('/')) {
      const prefix = apiBase.replace(/\/api\/?$/, '');
      if (prefix) {
        return `${window.location.origin}${prefix}${WS_PATH}`;
      }
    }
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

function unsubscribeAll() {
  while (subscriptions.length) {
    const sub = subscriptions.pop();
    try {
      sub.unsubscribe();
    } catch {
      /* ignore */
    }
  }
}

function subscribeTopics() {
  if (!client?.connected) return;
  unsubscribeAll();
  subscriptions.push(
    client.subscribe('/topic/chat', (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        chatHandlers.forEach((fn) => fn(payload));
      } catch {
        /* ignore */
      }
    })
  );
  subscriptions.push(
    client.subscribe('/topic/chat/recall', (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        recallHandlers.forEach((fn) => fn(payload));
      } catch {
        /* ignore */
      }
    })
  );
  subscriptions.push(
    client.subscribe('/topic/chat/profile', (frame) => {
      try {
        const payload = JSON.parse(frame.body);
        profileHandlers.forEach((fn) => fn(payload));
      } catch {
        /* ignore */
      }
    })
  );
  if (currentToken) {
    subscriptions.push(
      client.subscribe('/user/queue/notifications', (frame) => {
        try {
          const payload = JSON.parse(frame.body);
          notificationHandlers.forEach((fn) => fn(payload));
        } catch {
          /* ignore */
        }
      })
    );
    subscriptions.push(
      client.subscribe('/user/queue/chat/ack', (frame) => {
        try {
          const payload = JSON.parse(frame.body);
          ackHandlers.forEach((fn) => fn(payload));
        } catch {
          /* ignore */
        }
      })
    );
    subscriptions.push(
      client.subscribe('/user/queue/chat/offline', (frame) => {
        try {
          const payload = JSON.parse(frame.body);
          offlineHandlers.forEach((fn) => fn(payload));
        } catch {
          /* ignore */
        }
      })
    );
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
    subscribeTopics();
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
  unsubscribeAll();
  if (client) {
    client.deactivate();
    client = null;
  }
  notifyStatus(false);
}

export function sendRecall(messageId) {
  if (!client?.connected) {
    throw new Error('未连接');
  }
  if (!currentToken) {
    throw new Error('未登录');
  }
  client.publish({
    destination: '/app/chat/recall',
    body: JSON.stringify({ messageId }),
    headers: { 'content-type': 'application/json' },
  });
}

export function onChatMessage(handler) {
  chatHandlers.add(handler);
  return () => chatHandlers.delete(handler);
}

export function onChatAck(handler) {
  ackHandlers.add(handler);
  return () => ackHandlers.delete(handler);
}

export function onChatOffline(handler) {
  offlineHandlers.add(handler);
  return () => offlineHandlers.delete(handler);
}

export function onChatRecall(handler) {
  recallHandlers.add(handler);
  return () => recallHandlers.delete(handler);
}

export function onChatProfileUpdate(handler) {
  profileHandlers.add(handler);
  return () => profileHandlers.delete(handler);
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

export async function ensureSubscribed() {
  if (!client?.connected) {
    await connect(currentToken);
    return;
  }
  subscribeTopics();
}
