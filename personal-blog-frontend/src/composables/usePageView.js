import { onMounted, watch } from 'vue';
import request from '../utils/request';

const VISITOR_KEY = 'blog-visitor-id';
const VIEW_KEY_PREFIX = 'blog-pv:';

function getVisitorId() {
  try {
    let id = localStorage.getItem(VISITOR_KEY);
    if (!id) {
      id =
        typeof crypto !== 'undefined' && crypto.randomUUID
          ? crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(36).slice(2)}`;
      localStorage.setItem(VISITOR_KEY, id);
    }
    return id;
  } catch {
    return `anon-${Date.now()}`;
  }
}

function viewDedupKey(body) {
  if (body.page === 'article') return `${body.page}:${body.articleId}`;
  return body.page || 'unknown';
}

function shouldTrack(body) {
  const key = VIEW_KEY_PREFIX + viewDedupKey(body);
  try {
    if (sessionStorage.getItem(key)) return false;
    sessionStorage.setItem(key, '1');
    return true;
  } catch {
    return true;
  }
}

function postView(body) {
  if (!shouldTrack(body)) return;
  request({
    url: '/stat/view',
    method: 'post',
    data: body,
    skipErrorToast: true,
  }).catch(() => {});
}

export function usePageViewHome() {
  onMounted(() => {
    postView({ page: 'home', visitorId: getVisitorId() });
  });
}

export function usePageViewArticle(getArticleId) {
  watch(
    () => getArticleId(),
    (id) => {
      const n = Number(id);
      if (Number.isFinite(n) && n > 0) {
        postView({ page: 'article', articleId: n, visitorId: getVisitorId() });
      }
    },
    { immediate: true }
  );
}
