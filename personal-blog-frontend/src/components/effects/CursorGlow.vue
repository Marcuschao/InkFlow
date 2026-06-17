<template>
  <div
    v-if="enabled"
    class="cursor-glow"
    :style="{ transform: `translate(${x}px, ${y}px)` }"
    aria-hidden="true"
  />
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const enabled = ref(false);
const x = ref(-9999);
const y = ref(-9999);

function onMove(e) {
  x.value = e.clientX - 200;
  y.value = e.clientY - 200;
}

onMounted(() => {
  const desktop = window.matchMedia('(hover: hover) and (min-width: 1024px)').matches;
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  enabled.value = desktop && !reduced;
  if (enabled.value) {
    window.addEventListener('mousemove', onMove, { passive: true });
  }
});

onUnmounted(() => {
  window.removeEventListener('mousemove', onMove);
});
</script>

<style scoped>
.cursor-glow {
  position: fixed;
  top: 0;
  left: 0;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
  background: radial-gradient(
    circle,
    rgba(99, 102, 241, 0.12) 0%,
    rgba(139, 92, 246, 0.06) 40%,
    transparent 70%
  );
  transition: transform 0.15s ease-out;
  will-change: transform;
}

@media (prefers-reduced-motion: reduce) {
  .cursor-glow {
    display: none;
  }
}
</style>
