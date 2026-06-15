import request from '../utils/request';

export function getKnowledgeGraph() {
  return request({ url: '/knowledge/graph', method: 'get' });
}

export function getKnowledgeNode(tagId, limit = 10) {
  return request({ url: '/knowledge/node', method: 'get', params: { tagId, limit } });
}

export function getKnowledgeSubgraph(articleId) {
  return request({ url: '/knowledge/subgraph', method: 'get', params: { articleId } });
}

export function getHotTags(limit = 10) {
  return request({ url: '/knowledge/hot-tags', method: 'get', params: { limit } });
}

export function getUserLandscape(userId, recentArticleIds = []) {
  return request({
    url: '/knowledge/user-landscape',
    method: 'post',
    params: { userId },
    data: { recentArticleIds },
  });
}

export function subscribeTag(tagId) {
  return request({ url: '/knowledge/subscribe', method: 'post', params: { tagId } });
}

export function unsubscribeTag(tagId) {
  return request({ url: '/knowledge/subscribe', method: 'delete', params: { tagId } });
}
