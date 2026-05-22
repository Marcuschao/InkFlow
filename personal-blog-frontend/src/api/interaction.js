import request from '../utils/request';

export function toggleLike(articleId) {
  return request({ url: `/articles/${articleId}/like`, method: 'post' });
}

export function getLikeStatus(articleId) {
  return request({ url: `/articles/${articleId}/like/status`, method: 'get' });
}

export function getLikeCount(articleId) {
  return request({ url: `/articles/${articleId}/likes/count`, method: 'get', skipErrorToast: true });
}

export function toggleFavorite(articleId) {
  return request({ url: `/articles/${articleId}/favorite`, method: 'post' });
}

export function getFavoriteStatus(articleId) {
  return request({ url: `/articles/${articleId}/favorite/status`, method: 'get' });
}

export function fetchMyFavorites(params) {
  return request({ url: '/user/me/favorites', method: 'get', params });
}

export function toggleFollow(userId) {
  return request({ url: `/user/${userId}/follow`, method: 'post' });
}

export function getFollowStatus(userId) {
  return request({ url: `/user/${userId}/follow/status`, method: 'get', skipErrorToast: true });
}

export function fetchFollowers(userId, params) {
  return request({ url: `/user/${userId}/followers`, method: 'get', params });
}

export function fetchFollowing(userId, params) {
  return request({ url: `/user/${userId}/following`, method: 'get', params });
}

export function fetchFeed(params) {
  return request({ url: '/user/feed', method: 'get', params });
}
