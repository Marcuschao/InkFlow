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
