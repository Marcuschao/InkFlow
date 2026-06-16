import { defineStore } from 'pinia';
import { toggleLike, toggleFavorite, toggleFollow } from '../api/interaction';

export const useInteractionStore = defineStore('interaction', {
  state: () => ({
    likeCache: {},
    favoriteCache: {},
    followCache: {},
  }),

  actions: {
    async optimisticToggleLike(articleId, current) {
      try {
        const res = await toggleLike(articleId);
        const data = res.data || {};
        return {
          liked: data.liked ?? !current.liked,
          likeCount: data.likeCount ?? current.likeCount,
        };
      } catch (e) {
        throw e;
      }
    },

    async optimisticToggleFavorite(articleId, current) {
      try {
        const res = await toggleFavorite(articleId);
        return { favorited: res.data?.favorited ?? !current.favorited };
      } catch (e) {
        throw e;
      }
    },

    async optimisticToggleFollow(userId, currentFollowing) {
      try {
        const res = await toggleFollow(userId);
        return res.data?.following ?? !currentFollowing;
      } catch (e) {
        throw e;
      }
    },
  },
});
