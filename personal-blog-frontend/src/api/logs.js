import request from '../utils/request';

export function fetchAdminLogs(page = 1, size = 10) {
  return request({
    url: '/admin/logs',
    method: 'get',
    params: { page, size },
  }).then((res) => res.data);
}
