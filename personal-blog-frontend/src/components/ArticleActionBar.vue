<template>
  <div class="action-bar">
    <button
      type="button"
      class="act-btn"
      :class="{ active: liked, pulse: likePulse }"
      :disabled="busyLike"
      @click="onLike"
    >
      <svg class="act-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"
          :fill="liked ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="1.5"
        />
      </svg>
      <span>{{ likeCount }}</span>
    </button>
    <button
      type="button"
      class="act-btn"
      :class="{ active: favorited }"
      :disabled="busyFav"
      @click="onFavorite"
    >
      <svg class="act-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
          :fill="favorited ? 'currentColor' : 'none'"
          stroke="currentColor"
          stroke-width="1.5"
        />
      </svg>
      <span>{{ favorited ? '已收藏' : '收藏' }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { toggleLike, toggleFavorite } from '../api/interaction';

const props = defineProps({
  articleId: { type: Number, required: true },
  liked: { type: Boolean, default: false },
  favorited: { type: Boolean, default: false },
  likeCount: { type: Number, default: 0 },
});

const emit = defineEmits(['update:liked', 'update:favorited', 'update:likeCount']);

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const busyLike = ref(false);
const busyFav = ref(false);
const likePulse = ref(false);

watch(
  () => props.likeCount,
  () => {
    likePulse.value = true;
    setTimeout(() => { likePulse.value = false; }, 320);
  }
);

function requireLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } });
}

async function onLike() {
  if (!authStore.isLoggedIn) {
    requireLogin();
    return;
  }
  busyLike.value = true;
  try {
    const res = await toggleLike(props.articleId);
    const d = res.data;
    emit('update:liked', d?.liked ?? !props.liked);
    emit('update:likeCount', d?.likeCount ?? props.likeCount);
  } finally {
    busyLike.value = false;
  }
}

async function onFavorite() {
  if (!authStore.isLoggedIn) {
    requireLogin();
    return;
  }
  busyFav.value = true;
  try {
    const res = await toggleFavorite(props.articleId);
    emit('update:favorited', res.data?.favorited ?? !props.favorited);
  } finally {
    busyFav.value = false;
  }
}
</script>

<style scoped>
.action-bar {
  display: flex;
  gap: var(--space-3);
  margin: var(--space-4) 0;
}

.act-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-muted);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-pill);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  cursor: pointer;
  font-family: inherit;
  transition: color var(--transition-fast), border-color var(--transition-fast), transform var(--transition-fast);
}

.act-btn:hover:not(:disabled) {
  color: var(--color-text);
  border-color: var(--color-text-muted);
}

.act-btn.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.act-btn.pulse {
  animation: like-pop 0.32s ease;
}

.act-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.act-icon {
  width: 1.1rem;
  height: 1.1rem;
}

@keyframes like-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.12); }
  100% { transform: scale(1); }
}
</style>
