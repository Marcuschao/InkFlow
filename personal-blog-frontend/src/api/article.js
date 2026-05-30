import request from '../utils/request';

export function getArticles(params) {
  const { page, size, tagId, categoryId, keyword } = params || {};
  return request({
    url: '/articles',
    method: 'get',
    params: { page, size, tagId, categoryId, keyword },
  });
}

export function getArticleDetail(id, lang) {
  return request({
    url: `/articles/${id}`,
    method: 'get',
    params: lang ? { lang } : {},
  });
}

export function createArticle(data, tagNames) {
  return request({
    url: '/articles',
    method: 'post',
    data,
    params: tagNames != null && String(tagNames).trim() !== '' ? { tagNames } : {},
  });
}

export function updateArticle(id, data, tagNames) {
  return request({
    url: `/articles/${id}`,
    method: 'put',
    data,
    params: tagNames != null && String(tagNames).trim() !== '' ? { tagNames } : {},
  });
}

export function deleteArticle(id) {
  return request({
    url: `/articles/${id}`,
    method: 'delete',
  });
}

export function listArticleVersions(articleId) {
  return request({ url: `/articles/${articleId}/versions`, method: 'get' });
}

export function getArticleVersion(articleId, versionId) {
  return request({ url: `/articles/${articleId}/versions/${versionId}`, method: 'get' });
}

export function restoreArticleVersion(articleId, versionId) {
  return request({ url: `/articles/${articleId}/versions/${versionId}/restore`, method: 'post' });
}

export function diffArticleVersions(articleId, v1, v2) {
  return request({ url: `/articles/${articleId}/versions/${v1}/diff/${v2}`, method: 'get' });
}

export function getMyArticles(params) {
  const { page, size, status } = params || {};
  return request({
    url: '/articles/mine',
    method: 'get',
    params: { page, size, status },
  });
}

export function getReviewArticles(params) {
  const { page, size } = params || {};
  return request({
    url: '/admin/articles/review',
    method: 'get',
    params: { page, size },
  });
}

export function approveArticle(id) {
  return request({ url: `/admin/articles/${id}/approve`, method: 'put' });
}

export function rejectArticle(id, reason) {
  return request({
    url: `/admin/articles/${id}/reject`,
    method: 'put',
    data: reason ? { reason } : {},
  });
}

export function reportArticle(id, reason) {
  return request({
    url: `/articles/${id}/report`,
    method: 'post',
    data: { reason },
  });
}

export function getContentReports(params) {
  const { page, size, status } = params || {};
  return request({
    url: '/admin/content-reports',
    method: 'get',
    params: { page, size, status },
  });
}

export function handleContentReport(id, status, note) {
  return request({
    url: `/admin/content-reports/${id}/handle`,
    method: 'put',
    data: { status, note },
  });
}

export const ARTICLE_STATUS = {
  DRAFT: 0,
  PUBLISHED: 1,
  PENDING: 2,
  REJECTED: 3,
};

export function articleStatusLabel(status) {
  switch (status) {
    case ARTICLE_STATUS.DRAFT:
      return '草稿';
    case ARTICLE_STATUS.PUBLISHED:
      return '已发布';
    case ARTICLE_STATUS.PENDING:
      return '待审核';
    case ARTICLE_STATUS.REJECTED:
      return '已驳回';
    default:
      return '未知';
  }
}
