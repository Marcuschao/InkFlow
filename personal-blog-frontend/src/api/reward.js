import request from '../utils/request';

export function rewardArticle(articleId, points) {
  return request({ url: `/articles/${articleId}/reward`, method: 'post', data: { points } });
}

export function fetchArticleRewards(articleId) {
  return request({ url: `/articles/${articleId}/rewards`, method: 'get' });
}

export function fetchRewardHistory() {
  return request({ url: '/user/reward-history', method: 'get' });
}
