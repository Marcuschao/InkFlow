import { ref, onMounted, onUnmounted } from 'vue';

export function useParallax(factor = 0.5) {
  const offsetY = ref(0);

  function onScroll() {
    offsetY.value = window.scrollY * factor;
  }

  onMounted(() => {
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  });

  onUnmounted(() => {
    window.removeEventListener('scroll', onScroll);
  });

  return { offsetY };
}
