import { reactive } from 'vue';

const profiles = reactive({});

export function useChatUserProfiles() {
  function setProfile(userId, data) {
    if (!userId || !data) return;
    profiles[userId] = {
      username: data.username || '用户',
      avatar: data.avatar ?? null,
    };
  }

  function mergeFromMessages(list) {
    for (const msg of list || []) {
      if (msg?.userId && msg.username) {
        setProfile(msg.userId, { username: msg.username, avatar: msg.avatar });
      }
    }
  }

  function profileOf(userId) {
    return profiles[userId] || { username: '用户', avatar: null };
  }

  return { profiles, setProfile, mergeFromMessages, profileOf };
}
