import { defineStore } from 'pinia';
import { getArticles, getArticleDetail } from '../api/article';
import { getTags } from '../api/tag';

export const useArticleStore = defineStore('article', {
  state: () => ({
    articles: [],
    currentArticle: null,
    pagination: {
      total: 0,
      pageSize: 9,
      currentPage: 1,
    },
    tags: [],
    listError: null,
    homeFeed: {
      records: [],
      currentPage: 0,
      total: 0,
      hasMore: true,
      loading: false,
      error: '',
      initialized: false,
      scrollY: 0,
      restorePending: false,
    },
  }),
  actions: {
    async fetchArticles(page = 1, pageSize = 10, tagId = null) {
      this.listError = null;
      try {
        const response = await getArticles({ page, size: pageSize, tagId });
        const pageData = response.data;
        this.articles = pageData.records || [];
        this.pagination.total = pageData.total || 0;
        this.pagination.currentPage = page;
      } catch (error) {
        console.error('Failed to fetch articles:', error);
        this.listError = error?.message || '加载失败';
        this.articles = [];
      }
    },
    async fetchArticleDetail(id, lang) {
      this.currentArticle = null;
      try {
        const response = await getArticleDetail(id, lang);
        this.currentArticle = response.data;
      } catch (error) {
        console.error(`Failed to fetch article ${id}:`, error);
        this.currentArticle = null;
      }
    },
    async fetchTags() {
      try {
        const response = await getTags();
        const list = response.data;
        this.tags = Array.isArray(list) ? list : [];
      } catch (error) {
        console.error('Failed to fetch tags:', error);
      }
    },
    async loadHomeNextPage(pageSize = 10) {
      const feed = this.homeFeed;
      if (feed.loading || !feed.hasMore) return;

      feed.loading = true;
      feed.error = '';
      const nextPage = feed.currentPage + 1;
      try {
        const response = await getArticles({ page: nextPage, size: pageSize });
        const pageData = response.data || {};
        const records = Array.isArray(pageData.records) ? pageData.records : [];
        const existingIds = new Set(feed.records.map((article) => article.id));
        feed.records.push(...records.filter((article) => !existingIds.has(article.id)));
        feed.currentPage = Number(pageData.current) || nextPage;

        const total = Number(pageData.total);
        const pages = Number(pageData.pages);
        if (Number.isFinite(total) && total >= 0) {
          feed.total = total;
          feed.hasMore = feed.records.length < total;
        } else if (Number.isFinite(pages) && pages > 0) {
          feed.hasMore = feed.currentPage < pages;
        } else {
          feed.hasMore = records.length === pageSize;
        }
        feed.initialized = true;
      } catch (error) {
        feed.error = nextPage === 1 ? '文章暂时无法加载，请稍后重试。' : '后续文章加载失败，请重试。';
      } finally {
        feed.loading = false;
      }
    },
    rememberHomePosition(scrollY, restorePending = true) {
      this.homeFeed.scrollY = Math.max(0, Number(scrollY) || 0);
      this.homeFeed.restorePending = restorePending;
    },
    consumeHomePosition() {
      const position = this.homeFeed.restorePending ? this.homeFeed.scrollY : 0;
      this.homeFeed.restorePending = false;
      return position;
    },
    resetHomeFeed() {
      Object.assign(this.homeFeed, {
        records: [],
        currentPage: 0,
        total: 0,
        hasMore: true,
        loading: false,
        error: '',
        initialized: false,
        scrollY: 0,
        restorePending: false,
      });
    },
  },
});
