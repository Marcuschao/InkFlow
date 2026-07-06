import request from '../utils/request';

export function fetchAdminUsers(params) {
  return request({ url: '/admin/users', method: 'get', params });
}
