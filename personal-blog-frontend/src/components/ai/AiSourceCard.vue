<template>
  <component
    :is="linkComponent"
    v-bind="linkProps"
    class="ai-source-card"
    :class="{ 'ai-source-card--active': active }"
  >
    <span class="ai-source-idx">[{{ index }}]</span>
    <div class="ai-source-body">
      <span class="ai-source-title">{{ source.title || '未知来源' }}</span>
      <p v-if="source.snippet" class="ai-source-snippet">{{ source.snippet }}</p>
    </div>
  </component>
</template>

<script setup>
import { computed } from 'vue';
import { RouterLink } from 'vue-router';

const props = defineProps({
  source: { type: Object, required: true },
  index: { type: Number, default: 1 },
  active: { type: Boolean, default: false },
});

const href = computed(() => props.source?.link || '');

const isInternal = computed(() => {
  const l = href.value;
  return l && l.startsWith('/article/');
});

const linkComponent = computed(() => (isInternal.value ? RouterLink : 'a'));

const linkProps = computed(() => {
  const l = href.value;
  if (isInternal.value) {
    if (l.startsWith('/article/')) return { to: l };
    return { to: l };
  }
  if (l && l.startsWith('http')) return { href: l, target: '_blank', rel: 'noopener' };
  return { href: l || '#', onClick: (e) => e.preventDefault() };
});
</script>

<style scoped>
.ai-source-card {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border: var(--border-brutal);
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  text-decoration: none;
  color: inherit;
  transition: box-shadow var(--transition-fast);
}

.ai-source-card:hover {
  box-shadow: var(--shadow-brutal-sm);
}

.ai-source-card--active {
  border-color: var(--color-accent);
  background: rgba(212, 175, 55, 0.18);
}

.ai-source-idx {
  flex-shrink: 0;
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  color: var(--color-accent);
}

.ai-source-title {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.ai-source-snippet {
  margin: var(--space-1) 0 0;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
