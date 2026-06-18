import request from '../utils/request';

export function uploadDiaryImage(file) {
  const fd = new FormData();
  fd.append('file', file);
  return request({
    url: '/upload/image',
    method: 'post',
    data: fd,
    timeout: 60000,
  });
}

function uploadAdminIcon(url, file) {
  const fd = new FormData();
  fd.append('file', file);
  return request({
    url,
    method: 'post',
    data: fd,
    timeout: 60000,
  });
}

export function uploadBadgeIcon(id, file) {
  return uploadAdminIcon(`/admin/badges/${id}/icon`, file);
}

export function uploadItemIcon(id, file) {
  return uploadAdminIcon(`/admin/items/${id}/icon`, file);
}
