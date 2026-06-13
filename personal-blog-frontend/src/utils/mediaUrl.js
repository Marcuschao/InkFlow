const MINIO_HOST = '120.48.48.31:57890';

function minioOrigin() {
  if (typeof window !== 'undefined') return `${window.location.origin}/minio/`;
  return 'http://tdwqlc.top:52148/minio/';
}

function rewriteLegacyMediaUrl(url) {
  let next = url.replace(/^https?:\/\/120\.48\.48\.31:57890\//i, minioOrigin());
  next = next.replace(/^https?:\/\/8\.131\.98\.87(?::\d+)?\//i, (m) => {
    if (typeof window !== 'undefined') return `${window.location.origin}/`;
    return 'http://tdwqlc.top:52148/';
  });

  const absUploadAvatar = next.match(/^https?:\/\/[^/]+\/upload\/avatars\/(.+)$/i);
  if (absUploadAvatar) {
    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://tdwqlc.top:52148';
    return `${origin}/minio/blog-avatars/avatars/${absUploadAvatar[1]}`;
  }

  if (next.startsWith('/upload/avatars/')) {
    return `/minio/blog-avatars/avatars/${next.slice('/upload/avatars/'.length)}`;
  }

  const absUploadDiary = next.match(/^https?:\/\/[^/]+\/upload\/diary\/(.+)$/i);
  if (absUploadDiary) {
    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://tdwqlc.top:52148';
    return `${origin}/minio/blog-diary/diary/${absUploadDiary[1]}`;
  }

  if (next.startsWith('/upload/diary/')) {
    return `/minio/blog-diary/diary/${next.slice('/upload/diary/'.length)}`;
  }

  if (next.includes(MINIO_HOST)) {
    next = next.replace(`http://${MINIO_HOST}/`, minioOrigin());
    next = next.replace(`https://${MINIO_HOST}/`, minioOrigin());
  }

  return next;
}

export function resolveMediaUrl(url) {
  if (!url || typeof url !== 'string') return '';
  const trimmed = rewriteLegacyMediaUrl(url.trim());
  if (!trimmed) return '';
  if (/^(https?:|data:|blob:)/i.test(trimmed)) return trimmed;
  if (trimmed.startsWith('//')) {
    if (typeof window === 'undefined') return trimmed;
    return `${window.location.protocol}${trimmed}`;
  }
  if (typeof window === 'undefined') return trimmed;
  const path = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  return new URL(path, window.location.origin).href;
}
