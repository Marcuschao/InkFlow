import { defineStore } from 'pinia';

export const useArticleAiChatStore = defineStore('articleAiChat', {
  state: () => ({
    open: false,
    draftQuestion: '',
  }),
  actions: {
    toggle() {
      this.open = !this.open;
    },
    openChat(opts = {}) {
      if (opts.draftQuestion) this.draftQuestion = opts.draftQuestion;
      this.open = true;
    },
    closeChat() {
      this.open = false;
    },
    takeDraft() {
      const d = this.draftQuestion;
      this.draftQuestion = '';
      return d;
    },
  },
});
