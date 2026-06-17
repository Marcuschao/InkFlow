import request from '../utils/request';

export function signIn() {
  return request({ url: '/sign/sign', method: 'post' });
}

export function getSignStatus() {
  return request({ url: '/sign/status', method: 'get' });
}

export function getSignCalendar(year, month) {
  return request({ url: '/sign/calendar', method: 'get', params: { year, month } });
}

export function getPoints() {
  return request({ url: '/user/points', method: 'get' });
}

export function getSocialCard(userId) {
  return request({ url: `/user/${userId}/social-card`, method: 'get' });
}

export function recordVisit(ownerId) {
  return request({ url: `/profile/visit/${ownerId}`, method: 'post' });
}

export function getTimeline(userId) {
  return request({ url: '/user/timeline', method: 'get', params: { type: 'interaction', userId } });
}

export function getBadges() {
  return request({ url: '/badges', method: 'get' });
}

export function getUserBadges(userId) {
  return request({ url: `/badges/user/${userId}`, method: 'get' });
}

export function getVisitors() {
  return request({ url: '/profile/visitors', method: 'get' });
}
