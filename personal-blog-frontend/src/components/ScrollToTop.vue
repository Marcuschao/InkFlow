<template>
  <button
    v-show="visible"
    type="button"
    class="scroll-top-btn ds-active-scale"
    :class="{ 'scroll-top-btn--article': isArticlePage }"
    aria-label="回到顶部"
    @click="scrollTop"
  >
    <n-icon :component="ChevronUpOutline" :size="20" />
  </button>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { NIcon } from 'naive-ui';
import { ChevronUpOutline } from '@vicons/ionicons5';

const route = useRoute();
const visible = ref(false);
const isArticlePage = computed(() => route.name === 'ArticleDetail');

function onScroll() {
  visible.value = (window.scrollY || 0) > (window.innerHeight || 600) * 0.85;
}

function scrollTop() {
  const reduce = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
  window.scrollTo({ top: 0, behavior: reduce ? 'auto' : 'smooth' });
}

onMounted(() => {
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });
});
onUnmounted(() => window.removeEventListener('scroll', onScroll));
</script>

<style scoped>
.scroll-top-btn {
  position: fixed;
  right: var(--space-4);
  bottom: calc(var(--space-6) + env(safe-area-inset-bottom, 0px));
  z-index: 1310;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-pill);
  border: none;
  background: var(--color-surface);
  color: var(--color-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-card);
  transition:
    color var(--transition-fast),
    box-shadow var(--transition-smooth),
    background var(--transition-fast);
}

.scroll-top-btn:hover {
  color: var(--color-primary);
  box-shadow: var(--shadow-card-hover);
}

.scroll-top-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .scroll-top-btn {
    left: var(--space-4);
    right: auto;
    bottom: var(--layout-scroll-top-bottom);
  }

  .scroll-top-btn--article {
    bottom: var(--layout-scroll-top-bottom-with-fab);
  }
}

@media (min-width: 768px) {
  .scroll-top-btn {
    bottom: calc(var(--space-6) + env(safe-area-inset-bottom, 0px));
  }

  .scroll-top-btn--article {
    bottom: calc(var(--space-4) + 3.25rem + var(--space-3) + env(safe-area-inset-bottom, 0px));
  }
}
</style>
