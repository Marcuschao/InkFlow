import request from '../utils/request';

export function fetchHotSearchSources() {
  return request({ url: '/hot-search/sources', method: 'get', skipErrorToast: true });
}

export function fetchHotSearchList(params) {
  return request({ url: '/hot-search/list', method: 'get', params, skipErrorToast: true });
}

export function fetchAllHotSearch(params) {
  return request({ url: '/hot-search/all', method: 'get', params, skipErrorToast: true });
}
