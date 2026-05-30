import request from '../utils/request';

export function fetchArticleComments(articleId) {
  return request({
    url: `/articles/${articleId}/comments`,
    method: 'get',
    skipErrorToast: true,
  });
}

export function submitComment(data) {
  const payload = { ...data };
  if (!payload.clientRequestId) {
    payload.clientRequestId = `c-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }
  return request({ url: '/comments', method: 'post', data: payload });
}

export function deleteComment(id) {
  return request({ url: `/comments/${id}`, method: 'delete' });
}

export function fetchAdminComments(params) {
  return request({ url: '/admin/comments', method: 'get', params });
}

export function approveComment(id) {
  return request({ url: `/admin/comments/${id}/approve`, method: 'put' });
}

export function deleteAdminComment(id) {
  return request({ url: `/admin/comments/${id}`, method: 'delete' });
}
