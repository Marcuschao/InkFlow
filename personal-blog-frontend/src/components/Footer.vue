<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div class="footer-accent" aria-hidden="true" />
      <p class="copyright">&copy; 2026 晓晓博客 · All rights reserved.</p>
      <p class="uptime">{{ uptimeText }}</p>
    </div>
  </footer>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const LAUNCH = new Date(2026, 4, 1, 0, 0, 0);

const uptimeText = ref('');

function pad(n) {
  return String(n).padStart(2, '0');
}

function tick() {
  const now = Date.now();
  const start = LAUNCH.getTime();
  if (now < start) {
    uptimeText.value = '即将上线';
    return;
  }
  const ms = now - start;
  const sec = Math.floor(ms / 1000) % 60;
  const min = Math.floor(ms / 60000) % 60;
  const hour = Math.floor(ms / 3600000) % 24;
  const day = Math.floor(ms / 86400000);
  uptimeText.value = `博客已运行 ${day} 天 ${pad(hour)} 小时 ${pad(min)} 分 ${pad(sec)} 秒`;
}

let timer;

onMounted(() => {
  tick();
  timer = setInterval(tick, 1000);
});

onUnmounted(() => {
  clearInterval(timer);
});
</script>

<style scoped>
.footer {
  margin-top: auto;
  padding: 2.75rem 0;
  background: var(--gradient-footer);
  border-top: 1px solid var(--color-border);
  position: relative;
}

.footer-inner {
  text-align: center;
  position: relative;
}

.footer-accent {
  position: absolute;
  left: 50%;
  top: -1px;
  transform: translateX(-50%);
  width: min(480px, 72%);
  height: 3px;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  background: var(--gradient-cta);
  opacity: 0.65;
}

.copyright {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text-muted);
  letter-spacing: 0.02em;
}

.uptime {
  margin-top: 0.65rem;
  font-size: 0.8rem;
  color: var(--color-text-soft);
  opacity: 0.9;
  font-variant-numeric: tabular-nums;
}

.footer p {
  margin: 0;
}
</style>
