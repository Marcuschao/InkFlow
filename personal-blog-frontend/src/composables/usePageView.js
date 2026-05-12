import { onMounted, watch } from 'vue';
import request from '../utils/request';

const VISITOR_KEY = 'blog-visitor-id';

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

function postView(body) {
  request({ url: '/stat/view', method: 'post', data: body }).catch(() => {});
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
