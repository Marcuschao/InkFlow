import request from '../utils/request';

export function fetchOAuthBindings() {
  return request({ url: '/user/me/oauth', method: 'get' });
}

export function bindGithub() {
  return request({ url: '/user/me/oauth/github/bind', method: 'post' });
}

export function unbindGithub() {
  return request({ url: '/user/me/oauth/github', method: 'delete' });
}

export function githubLoginUrl() {
  if (import.meta.env.DEV) {
    return 'http://localhost:8080/oauth2/authorization/github';
  }
  const base = import.meta.env.VITE_APP_WS_BASE_URL || '';
  if (base && base !== '/') {
    return `${base.replace(/\/$/, '')}/oauth2/authorization/github`;
  }
  return '/oauth2/authorization/github';
}
