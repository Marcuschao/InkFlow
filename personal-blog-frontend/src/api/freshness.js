import request from '../utils/request';

export function getFreshnessSummary() {
  return request({ url: '/admin/freshness/summary', method: 'get' });
}

export function getFreshnessArticles(params) {
  return request({ url: '/admin/freshness/articles', method: 'get', params });
}

export function postFreshnessAiRefresh(id) {
  return request({ url: `/admin/freshness/${id}/ai-refresh`, method: 'post' });
}

export function postFreshnessForkDraft(id) {
  return request({ url: `/admin/freshness/${id}/fork-draft`, method: 'post' });
}

export function postFreshnessScanRun() {
  return request({ url: '/admin/freshness/scan-run', method: 'post' });
}
