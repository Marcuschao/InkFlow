<template>
  <div class="badge-strip">
    <router-link v-for="b in visible" :key="b.id" to="/badges" class="badge-item" :title="b.description">
      <img v-if="b.iconUrl" :src="b.iconUrl" :alt="b.name" class="badge-icon" />
      <span v-else class="badge-fallback">{{ b.name?.charAt(0) }}</span>
      <span class="badge-name">{{ b.name }}</span>
    </router-link>
    <router-link v-if="badges.length > max" to="/badges" class="badge-more muted">
      +{{ badges.length - max }}
    </router-link>
    <router-link v-if="!badges.length" to="/badges" class="badge-empty muted">暂无徽章</router-link>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  badges: { type: Array, default: () => [] },
  max: { type: Number, default: 6 },
});

const visible = computed(() => props.badges.slice(0, props.max));
</script>

<style scoped>
.badge-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
  margin-top: var(--space-3);
}

.badge-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  text-decoration: none;
  color: var(--color-text);
  font-size: var(--text-xs);
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.badge-item:hover {
  border-color: var(--color-border-strong);
  background: var(--surface-muted);
}

.badge-icon {
  width: 16px;
  height: 16px;
}

.badge-fallback {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-muted);
  color: var(--color-text-muted);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
}

.badge-more,
.badge-empty {
  font-size: var(--text-xs);
  text-decoration: none;
}

.muted {
  color: var(--color-text-muted);
}
</style>
