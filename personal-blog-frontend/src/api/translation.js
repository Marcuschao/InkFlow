import request from '../utils/request';

export function getTranslation(articleId, locale) {
  return request({
    url: `/admin/translations/article/${articleId}`,
    method: 'get',
    params: { locale },
  });
}

export function saveTranslation(articleId, body) {
  return request({
    url: `/admin/translations/article/${articleId}`,
    method: 'put',
    data: body,
  });
}

export function machineTranslateArticle(articleId, locale) {
  return request({
    url: `/admin/translations/article/${articleId}/machine-translate`,
    method: 'post',
    params: { locale },
  });
}

export function translationSeoAi(articleId, locale) {
  return request({
    url: `/admin/translations/article/${articleId}/seo-ai`,
    method: 'post',
    params: { locale },
  });
}

export function batchTranslate(body) {
  return request({ url: '/admin/translations/batch', method: 'post', data: body });
}

export function getTranslationJob(jobId) {
  return request({ url: `/admin/translations/jobs/${jobId}`, method: 'get' });
}

export function articleSeoAi(articleId) {
  return request({ url: `/admin/articles/${articleId}/seo-ai`, method: 'post' });
}
