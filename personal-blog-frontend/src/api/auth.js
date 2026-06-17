import request from '../utils/request';

export function fetchCaptcha() {
  return request({
    url: '/auth/captcha',
    method: 'get',
  }).then((res) => res.data);
}

export function login(body) {
  return request({
    url: '/auth/login',
    method: 'post',
    data: body,
  });
}

export function register(body) {
  return request({
    url: '/auth/register',
    method: 'post',
    data: body,
  });
}

export function requestPasswordReset(body) {
  return request({
    url: '/auth/password/reset-request',
    method: 'post',
    data: body,
    skipErrorToast: true,
  });
}

export function validatePasswordResetToken(token) {
  return request({
    url: '/auth/password/reset-validate',
    method: 'get',
    params: { token },
    skipErrorToast: true,
  }).then((res) => res.data);
}

export function resetPassword(body) {
  return request({
    url: '/auth/password/reset',
    method: 'post',
    data: body,
    skipErrorToast: true,
  });
}
