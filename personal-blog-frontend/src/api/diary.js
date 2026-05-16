import request from '../utils/request';

export function listDiaries(params) {
  return request({ url: '/diary', method: 'get', params });
}

export function getDiary(id) {
  return request({ url: `/diary/${id}`, method: 'get' });
}

export function createDiary(data) {
  return request({ url: '/diary', method: 'post', data });
}

export function updateDiary(id, data) {
  return request({ url: `/diary/${id}`, method: 'put', data });
}

export function deleteDiary(id) {
  return request({ url: `/diary/${id}`, method: 'delete' });
}

export function listPublicDiaries(params) {
  return request({ url: '/diary/public', method: 'get', params, skipErrorToast: true });
}

export function getPublicDiary(id) {
  return request({ url: `/diary/public/${id}`, method: 'get', skipErrorToast: true });
}

export function listDiaryVersions(diaryId) {
  return request({ url: `/diary/${diaryId}/versions`, method: 'get' });
}

export function getDiaryVersion(diaryId, versionId) {
  return request({ url: `/diary/${diaryId}/versions/${versionId}`, method: 'get' });
}

export function restoreDiaryVersion(diaryId, versionId) {
  return request({ url: `/diary/${diaryId}/versions/${versionId}/restore`, method: 'post' });
}

export function diffDiaryVersions(diaryId, v1, v2) {
  return request({ url: `/diary/${diaryId}/versions/${v1}/diff/${v2}`, method: 'get' });
}
