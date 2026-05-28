import request from '../utils/request';

export function fetchWeeklyReports(page = 1, size = 10) {
  return request({
    url: '/admin/reports/weekly',
    method: 'get',
    params: { page, size },
  }).then((res) => res.data);
}

export function fetchFreshnessReports(articleId, page = 1, size = 10) {
  return request({
    url: '/admin/reports/freshness',
    method: 'get',
    params: { articleId, page, size },
  }).then((res) => res.data);
}

export function fetchReportContent(id) {
  return request({
    url: `/admin/reports/${id}`,
    method: 'get',
  }).then((res) => res.data);
}
