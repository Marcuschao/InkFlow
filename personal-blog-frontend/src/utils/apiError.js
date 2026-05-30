const HTTP_HINTS = {
  400: '请求参数有误',
  401: '未授权，请重新登录',
  403: '无权访问',
  404: '资源不存在',
  429: '请求过于频繁，请稍后再试',
  500: '系统繁忙，请稍后再试',
};

function parseBody(data) {
  if (data == null) return null;
  if (typeof data === 'object') return data;
  if (typeof data !== 'string') return null;
  const t = data.trim();
  if (!t.startsWith('{') && !t.startsWith('[')) return null;
  try {
    return JSON.parse(t);
  } catch {
    return null;
  }
}

function isGarbledText(text) {
  if (!text || typeof text !== 'string') return true;
  if (text.includes('\uFFFD')) return true;
  const q = (text.match(/\?/g) || []).length;
  if (q >= 2 && q / text.length >= 0.25) return true;
  return /^[\s?]+$/.test(text);
}

function pickMessage(raw, code, httpStatus) {
  const biz = Number(code);
  const http = Number(httpStatus);
  if (biz === 429 || http === 429) return HTTP_HINTS[429];
  const hint = HTTP_HINTS[biz] || HTTP_HINTS[http];
  if (raw && !isGarbledText(raw)) return raw;
  if (hint) return hint;
  return raw && !isGarbledText(raw) ? raw : null;
}

export function resolveApiErrorMessage(error, fallback = '请求失败') {
  const res = error?.response;
  const body = parseBody(res?.data);
  const code = body?.code ?? error?.code;
  const httpStatus = res?.status ?? error?.responseStatus;
  const raw =
    (typeof body?.message === 'string' && body.message) ||
    (typeof body?.msg === 'string' && body.msg) ||
    (typeof error?.message === 'string' ? error.message : '');
  const msg = pickMessage(raw, code, httpStatus);
  if (msg) return msg;
  if (error?.code === 'ECONNABORTED') return '请求超时，请检查网络';
  if (!res) return '网络异常，请稍后重试';
  return fallback;
}

export function resolveApiErrorPayload(error) {
  const body = parseBody(error?.response?.data);
  return body?.data ?? error?.payload;
}

export function resolveApiErrorCode(error) {
  const body = parseBody(error?.response?.data);
  if (body?.code != null) return Number(body.code);
  if (error?.code != null && typeof error.code === 'number') return error.code;
  return error?.response?.status ?? error?.responseStatus;
}
