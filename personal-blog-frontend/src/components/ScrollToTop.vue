<template>
  <button
    v-show="visible"
    type="button"
    class="scroll-top-btn"
    aria-label="回到顶部"
    @click="scrollTop"
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path d="M5 15l7-7 7 7" />
    </svg>
  </button>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const visible = ref(false);

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
  right: 1rem;
  bottom: calc(5rem + env(safe-area-inset-bottom, 0px));
  z-index: 1310;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast),
    box-shadow var(--transition-fast),
    background var(--transition-fast);
}

.scroll-top-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: var(--shadow-hover);
}

.scroll-top-btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

@media (min-width: 768px) {
  .scroll-top-btn {
    bottom: calc(1.75rem + env(safe-area-inset-bottom, 0px));
  }
}
</style>
