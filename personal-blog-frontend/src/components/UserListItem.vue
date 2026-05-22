<template>
  <div class="user-item">
    <router-link :to="`/user/${user.id}`" class="user-link">
      <img v-if="user.avatar" :src="user.avatar" alt="" class="avatar" />
      <span v-else class="avatar letter">{{ initial }}</span>
      <span class="name">{{ user.nickname || '用户' }}</span>
    </router-link>
    <FollowButton :user-id="user.id" :following="user.following" @toggled="$emit('follow-changed')" />
  </div>
</template>

<script setup>
import { computed } from 'vue';
import FollowButton from './FollowButton.vue';

const props = defineProps({
  user: { type: Object, required: true },
});

defineEmits(['follow-changed']);

const initial = computed(() => (props.user.nickname || '?').slice(0, 1));
</script>

<style scoped>
.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--color-border);
}

.user-link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  text-decoration: none;
  color: var(--color-text);
  min-width: 0;
}

.avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.letter {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-muted);
  font-weight: var(--weight-semibold);
}

.name {
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
}
</style>
