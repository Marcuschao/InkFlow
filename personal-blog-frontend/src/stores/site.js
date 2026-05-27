import { defineStore } from 'pinia';
import { fetchPublicConfig } from '../api/site';

export const useSiteStore = defineStore('site', {
  state: () => ({
    chatbotVisibility: 'NONE',
    siteTitle: '晓晓博客',
    siteDescription: '',
    siteUrl: '',
    launchTime: null,
    loaded: false,
  }),
  actions: {
    async loadPublicConfig() {
      try {
        const res = await fetchPublicConfig();
        const data = res?.data ?? res;
        const mode = data?.chatbotVisibility || 'NONE';
        this.chatbotVisibility = ['NONE', 'GUEST', 'AUTH'].includes(mode) ? mode : 'NONE';
        if (data?.siteTitle) this.siteTitle = data.siteTitle;
        if (data?.siteDescription) this.siteDescription = data.siteDescription;
        if (data?.siteUrl) this.siteUrl = data.siteUrl;
        if (data?.launchTime) this.launchTime = data.launchTime;
      } catch {
        this.chatbotVisibility = 'NONE';
      } finally {
        this.loaded = true;
      }
    },
  },
});
