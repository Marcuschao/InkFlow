<template>
  <span class="user-avatar-wrap" :class="avatarFrameClass">
    <n-avatar v-if="resolvedSrc" round :size="size" :src="resolvedSrc" />
    <n-avatar v-else round :size="size">{{ initial }}</n-avatar>
  </span>
</template>

<script setup>
import { computed } from 'vue';
import { NAvatar } from 'naive-ui';
import { resolveMediaUrl } from '../utils/mediaUrl';
import { effectClass } from '../utils/itemEffects';

const props = defineProps({
  src: { type: String, default: '' },
  name: { type: String, default: '?' },
  size: { type: [Number, String], default: 40 },
  equippedItems: { type: Array, default: () => [] },
});

const resolvedSrc = computed(() => resolveMediaUrl(props.src));
const initial = computed(() => {
  const text = String(props.name || '?').trim();
  return text.slice(0, 1) || '?';
});
const avatarFrameClass = computed(() => effectClass(props.equippedItems, 'AVATAR_FRAME'));
</script>

<style scoped>
.user-avatar-wrap {
  display: inline-grid;
  place-items: center;
  border-radius: var(--radius-pill);
}

.item-frame-gold {
  padding: var(--space-1);
  border: 1px solid var(--color-warn);
  background: var(--color-surface);
}

.item-frame-color {
  padding: var(--space-1);
  border: 1px solid var(--color-accent-pink);
  background: var(--color-surface);
}
</style>
