import request from '../utils/request';

export function searchArticles(params) {
  return request({ url: '/search', method: 'get', params, skipErrorToast: true });
}

export function searchSuggest(params) {
  return request({ url: '/search/suggest', method: 'get', params, skipErrorToast: true });
}
