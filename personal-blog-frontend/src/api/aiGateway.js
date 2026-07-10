import request from '../utils/request';

export function fetchAiOverview() {
  return request({ url: '/admin/ai/stats/overview', method: 'get' }).then(res => res.data);
}

export function fetchAiTrend(days = 7) {
  return request({ url: '/admin/ai/stats/trend', method: 'get', params: { days } }).then(res => res.data);
}

export function fetchAiByModel() {
  return request({ url: '/admin/ai/stats/by-model', method: 'get' }).then(res => res.data);
}

export function fetchAiByUser(limit = 10) {
  return request({ url: '/admin/ai/stats/by-user', method: 'get', params: { limit } }).then(res => res.data);
}

export function fetchAiLogs(page = 1, size = 20) {
  return request({ url: '/admin/ai/logs', method: 'get', params: { page, size } }).then(res => res.data);
}

export function fetchAiModels() {
  return request({ url: '/admin/ai/models', method: 'get' }).then(res => res.data);
}

export function fetchAiModelHealth() {
  return request({ url: '/admin/ai/models/health', method: 'get' }).then(res => res.data);
}

export function updateAiModelEnabled(id, enabled) {
  return request({ url: `/admin/ai/models/${id}/enabled`, method: 'put', data: { enabled } });
}

export function fetchAiQuota() {
  return request({ url: '/admin/ai/quota', method: 'get' }).then(res => res.data);
}

export function saveAiQuota(data) {
  return request({ url: '/admin/ai/quota', method: 'put', data });
}

export function fetchAiWhitelist() {
  return request({ url: '/admin/ai/quota/whitelist', method: 'get' }).then(res => res.data);
}

export function addAiWhitelist(data) {
  return request({ url: '/admin/ai/quota/whitelist', method: 'post', data });
}

export function removeAiWhitelist(id) {
  return request({ url: `/admin/ai/quota/whitelist/${id}`, method: 'delete' });
}
