import { watch, onUnmounted } from 'vue';
import { CountUp } from 'countup.js';

export function useCountUp(targetRef, endValue, options = {}) {
  let instance = null;

  function run(val) {
    if (!targetRef.value || val == null) return;
    const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (instance) instance.reset();
    instance = new CountUp(targetRef.value, val, {
      duration: prefersReduced ? 0 : 1.2,
      useEasing: true,
      ...options,
    });
    if (!instance.error) instance.start();
    else targetRef.value.textContent = String(val);
  }

  const stop = watch(
    () => (typeof endValue === 'function' ? endValue() : endValue?.value ?? endValue),
    (val) => run(val),
    { immediate: true }
  );

  onUnmounted(() => {
    stop();
    instance = null;
  });
}
