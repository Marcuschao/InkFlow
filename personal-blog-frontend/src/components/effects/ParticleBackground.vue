<template>
  <div v-if="enabled" class="particle-bg" aria-hidden="true">
    <vue-particles id="tsparticles" :options="particleOptions" class="particle-canvas" />
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue';

const props = defineProps({
  density: { type: String, default: 'low' },
});

const enabled = ref(false);

const counts = { low: 40, medium: 80 };

const particleOptions = computed(() => ({
  fullScreen: { enable: false },
  background: { color: { value: 'transparent' } },
  fpsLimit: 60,
  particles: {
    number: { value: counts[props.density] || 40, density: { enable: true } },
    color: { value: ['#6366f1', '#8b5cf6', '#d946ef'] },
    opacity: { value: { min: 0.1, max: 0.4 } },
    size: { value: { min: 1, max: 2.5 } },
    move: {
      enable: true,
      speed: 0.4,
      direction: 'none',
      random: true,
      outModes: { default: 'out' },
    },
    links: {
      enable: props.density === 'medium',
      distance: 120,
      color: 'rgba(99, 102, 241, 0.15)',
      opacity: 0.2,
      width: 1,
    },
  },
  detectRetina: true,
}));

onMounted(() => {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const mobile = window.matchMedia('(max-width: 767px)').matches;
  enabled.value = !reduced && !(mobile && props.density !== 'medium');
});
</script>

<style scoped>
.particle-bg {
  position: fixed;
  inset: 0;
  z-index: var(--z-particles);
  pointer-events: none;
  overflow: hidden;
}

.particle-canvas {
  width: 100%;
  height: 100%;
}
</style>
