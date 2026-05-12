import request from '../utils/request';

export function fetchStatsSummary() {
  return request({ url: '/admin/stats/summary', method: 'get' }).then((res) => res.data);
}

export function fetchPvTrend(days) {
  return request({ url: '/admin/stats/pv-trend', method: 'get', params: { days } }).then((res) => res.data);
}

export function fetchTopArticles(limit = 5) {
  return request({ url: '/admin/stats/top-articles', method: 'get', params: { limit } }).then((res) => res.data);
}

export function fetchAiUsage(period = 'week') {
  return request({ url: '/admin/stats/ai-usage', method: 'get', params: { period } }).then((res) => res.data);
}
