import request from '../utils/request';

export function fetchMonitorCache() {
  return request({ url: '/admin/monitor/cache', method: 'get' }).then((res) => res.data);
}

export function fetchMonitorSlow() {
  return request({ url: '/admin/monitor/slow', method: 'get' }).then((res) => res.data);
}

export function fetchMonitorJvm() {
  return request({ url: '/admin/monitor/jvm', method: 'get' }).then((res) => res.data);
}

export function fetchMonitorSystem() {
  return request({ url: '/admin/monitor/system', method: 'get' }).then((res) => res.data);
}

export function fetchMonitorRateLimit() {
  return request({ url: '/admin/monitor/rate-limit', method: 'get' }).then((res) => res.data);
}

export function fetchMonitorChat() {
  return request({ url: '/admin/monitor/chat', method: 'get' }).then((res) => res.data);
}
