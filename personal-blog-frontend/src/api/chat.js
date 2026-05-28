import request from '../utils/request';

export function fetchChatHistory(params = {}) {
  return request({ url: '/chat/history', method: 'get', params, skipErrorToast: true });
}

export function fetchOnlineUsers() {
  return request({ url: '/chat/online-users', method: 'get', skipErrorToast: true });
}

export function sendChatMessage(content) {
  return request({ url: '/chat/send', method: 'post', data: { content } });
}

export function pingPresence() {
  return request({ url: '/chat/presence', method: 'post', skipErrorToast: true });
}

export function pingOffline() {
  return request({ url: '/chat/offline', method: 'post', skipErrorToast: true });
}
