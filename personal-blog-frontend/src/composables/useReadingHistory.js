const KEY = 'blog-reading-history-v1';
const MAX = 80;

function readList() {
  try {
    const raw = localStorage.getItem(KEY);
    const arr = raw ? JSON.parse(raw) : [];
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}

function writeList(list) {
  try {
    localStorage.setItem(KEY, JSON.stringify(list.slice(0, MAX)));
  } catch {
    /* ignore */
  }
}

export function useReadingHistory() {
  function recordVisit(article) {
    if (!article?.id) return;
    const id = Number(article.id);
    if (!Number.isFinite(id)) return;
    const row = {
      id,
      title: article.title || '',
      cover: article.cover || '',
      visitedAt: Date.now(),
      scrollPercent: 0,
    };
    const rest = readList().filter((x) => x && x.id !== id);
    writeList([row, ...rest]);
  }

  function updateProgress(articleId, scrollPercent) {
    const id = Number(articleId);
    if (!Number.isFinite(id)) return;
    const pct = Math.max(0, Math.min(100, Math.round(scrollPercent)));
    const list = readList();
    const idx = list.findIndex((x) => x && x.id === id);
    if (idx < 0) return;
    list[idx] = { ...list[idx], scrollPercent: pct, visitedAt: Date.now() };
    writeList(list);
  }

  function getRecentArticleIds(limit = 16) {
    return readList()
      .slice(0, limit)
      .map((x) => x.id)
      .filter((id) => Number.isFinite(Number(id)));
  }

  function listEntries() {
    return readList();
  }

  return { recordVisit, updateProgress, getRecentArticleIds, listEntries };
}
