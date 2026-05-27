import { defineStore } from 'pinia';
import { fetchUnreadCount } from '../api/notification';

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
  }),
  actions: {
    async refreshUnread() {
      try {
        const res = await fetchUnreadCount();
        this.unreadCount = res.data?.count ?? 0;
      } catch {
        this.unreadCount = 0;
      }
    },
    clearUnread() {
      this.unreadCount = 0;
    },
    incrementUnread(n = 1) {
      this.unreadCount += n;
    },
    applyRealtimeNotification(vo) {
      if (!vo || vo.read) return;
      this.incrementUnread();
    },
    decrementUnread(n = 1) {
      this.unreadCount = Math.max(0, this.unreadCount - n);
    },
  },
});
