<template>
  <button
    v-if="show"
    type="button"
    class="follow-btn"
    :class="{ following: isFollowing }"
    :disabled="busy"
    @click="onToggle"
  >
    {{ isFollowing ? '已关注' : '+ 关注' }}
  </button>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { useAuthStore } from '../stores/auth';
import { toggleFollow } from '../api/interaction';

const props = defineProps({
  userId: { type: Number, required: true },
  following: { type: Boolean, default: false },
});

const emit = defineEmits(['update:following', 'toggled']);

const authStore = useAuthStore();
const busy = ref(false);
const isFollowing = ref(props.following);

watch(
  () => props.following,
  (v) => { isFollowing.value = v; }
);

const show = computed(() => {
  if (!authStore.isLoggedIn || !authStore.user?.id) return false;
  return authStore.user.id !== props.userId;
});

async function onToggle() {
  if (!authStore.isLoggedIn) return;
  busy.value = true;
  try {
    const res = await toggleFollow(props.userId);
    isFollowing.value = res.data?.following ?? !isFollowing.value;
    emit('update:following', isFollowing.value);
    emit('toggled', res.data);
  } finally {
    busy.value = false;
  }
}
</script>

<style scoped>
.follow-btn {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  padding: var(--space-1) var(--space-4);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  cursor: pointer;
  font-family: inherit;
}

.follow-btn.following {
  color: var(--color-text-muted);
  background: var(--surface-muted);
}

.follow-btn:disabled {
  opacity: 0.6;
}
</style>
