<template>
  <n-space class="action-bar">
    <n-button
      :type="liked ? 'primary' : 'default'"
      :loading="busyLike"
      :class="{ pulse: likePulse }"
      @click="onLike"
    >
      <template #icon>
        <n-icon><HeartOutline v-if="!liked" /><Heart v-else /></n-icon>
      </template>
      {{ likeCount }}
    </n-button>
    <n-button
      :type="favorited ? 'primary' : 'default'"
      :loading="busyFav"
      @click="onFavorite"
    >
      <template #icon>
        <n-icon><StarOutline v-if="!favorited" /><Star v-else /></n-icon>
      </template>
      {{ favorited ? '已收藏' : '收藏' }}
    </n-button>
  </n-space>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { NButton, NIcon, NSpace } from 'naive-ui';
import { Heart, HeartOutline, Star, StarOutline } from '@vicons/ionicons5';
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
  margin: 0;
}

.pulse {
  animation: like-pop 0.32s ease;
}

@keyframes like-pop {
  0% { transform: scale(1); }
  40% { transform: scale(1.12); }
  100% { transform: scale(1); }
}

@media (prefers-reduced-motion: reduce) {
  .pulse { animation: none; }
}
</style>
