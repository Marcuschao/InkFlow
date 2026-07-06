import { computed } from 'vue';
import { useSiteStore } from '../stores/site';
import { useAuthStore } from '../stores/auth';

export function useAiChatVisibility() {
  const siteStore = useSiteStore();
  const authStore = useAuthStore();

  const visible = computed(() => {
    if (!siteStore.loaded) return false;
    const mode = siteStore.chatbotVisibility;
    if (mode === 'NONE') return false;
    if (mode === 'GUEST') return true;
    if (mode === 'AUTH') return authStore.isLoggedIn;
    return false;
  });

  return {
    visible,
    mode: computed(() => siteStore.chatbotVisibility),
    loaded: computed(() => siteStore.loaded),
  };
}
