import { defineStore } from 'pinia';
import { getArticles, getArticleDetail } from '../api/article';
import { getTags } from '../api/tag';

export const useArticleStore = defineStore('article', {
  state: () => ({
    articles: [],
    currentArticle: null,
    pagination: {
      total: 0,
      pageSize: 10,
      currentPage: 1,
    },
    tags: [],
  }),
  actions: {
    async fetchArticles(page = 1, pageSize = 10, tagId = null) {
      try {
        const response = await getArticles({ page, size: pageSize, tagId });
        const pageData = response.data;
        this.articles = pageData.records || [];
        this.pagination.total = pageData.total || 0;
        this.pagination.currentPage = page;
      } catch (error) {
        console.error('Failed to fetch articles:', error);
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
  },
});
