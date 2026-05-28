import request from '../utils/request';

export function fetchMe() {
  return request({
    url: '/user/me',
    method: 'get',
  });
}

export function updateProfile(data) {
  return request({
    url: '/user/profile',
    method: 'put',
    data,
  });
}

export function uploadAvatar(file) {
  const form = new FormData();
  form.append('file', file);
  return request({
    url: '/user/avatar',
    method: 'post',
    data: form,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export function fetchPublicUser(id) {
  return request({
    url: `/user/${id}`,
    method: 'get',
  });
}
