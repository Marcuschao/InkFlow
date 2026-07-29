import { onMounted, onUnmounted } from 'vue';
import { gsap } from 'gsap';

export function useGsapEntrance(rootRef, selector = '[data-animate]') {
  let context;

  onMounted(() => {
    const root = rootRef?.value;
    if (!root) return;
    context = gsap.context(() => {
      const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;
      const nodes = root.querySelectorAll(selector);
      if (!nodes.length || reduced) return;
      gsap.from(nodes, {
        autoAlpha: 0,
        y: 14,
        duration: 0.5,
        ease: 'power2.out',
        stagger: 0.055,
        clearProps: 'all',
      });
    }, root);
  });

  onUnmounted(() => context?.revert());
}
