import request from '../utils/request';

export function fetchBackupStatus() {
  return request({
    url: '/admin/backup/status',
    method: 'get',
  }).then((res) => res.data);
}

export function triggerBackup() {
  return request({
    url: '/admin/backup/trigger',
    method: 'post',
  }).then((res) => res.data);
}
