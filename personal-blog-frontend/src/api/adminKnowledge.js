import request from '../utils/request';

export function fetchKnowledgeDocuments(params) {
  return request({
    url: '/admin/knowledge/documents',
    method: 'get',
    params,
  }).then((res) => res.data);
}

export function uploadKnowledgeDocument(file) {
  const fd = new FormData();
  fd.append('file', file);
  return request({
    url: '/admin/knowledge/documents',
    method: 'post',
    data: fd,
    timeout: 120000,
  }).then((res) => res.data);
}

export function deleteKnowledgeDocument(id) {
  return request({
    url: `/admin/knowledge/documents/${id}`,
    method: 'delete',
  });
}
