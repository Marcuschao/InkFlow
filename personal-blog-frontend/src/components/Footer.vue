<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div class="footer-accent" aria-hidden="true" />
      <div class="footer-actions">
        <a href="/rss.xml" class="footer-link" target="_blank" rel="noopener noreferrer">RSS</a>
        <router-link to="/links" class="footer-link">友链</router-link>
        <router-link to="/search" class="footer-link">搜索</router-link>
      </div>
      <form class="sub-form" @submit.prevent="onSubscribe">
        <label class="sr-only" for="sub-email">订阅邮箱</label>
        <input
          id="sub-email"
          v-model.trim="subEmail"
          type="email"
          class="ds-input sub-input"
          placeholder="邮件订阅（可选）"
          autocomplete="email"
        />
        <button type="submit" class="ds-btn ds-btn--secondary sub-btn" :disabled="subBusy">
          {{ subBusy ? '…' : '订阅' }}
        </button>
      </form>
      <p v-if="subMsg" class="sub-msg">{{ subMsg }}</p>
      <p class="copyright">&copy; 2026 晓晓博客 · All rights reserved.</p>
      <p class="uptime">{{ uptimeText }}</p>
    </div>
  </footer>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { subscribeEmail } from '../api/subscribe';
import { useToastStore } from '../stores/toast';

const LAUNCH = new Date(2026, 4, 1, 0, 0, 0);

const uptimeText = ref('');
const subEmail = ref('');
const subBusy = ref(false);
const subMsg = ref('');
const toastStore = useToastStore();

async function onSubscribe() {
  subMsg.value = '';
  const e = subEmail.value.trim();
  if (!e) return;
  subBusy.value = true;
  try {
    await subscribeEmail(e);
    toastStore.push('已记录订阅邮箱', 'success');
    subEmail.value = '';
  } catch (err) {
    subMsg.value = err?.message || '';
  } finally {
    subBusy.value = false;
  }
}

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

@media (max-width: 1023px) {
  .footer {
    margin-bottom: calc(25px + env(safe-area-inset-bottom, 0px));
  }
}

.footer-inner {
  text-align: center;
  position: relative;
}

.footer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.65rem;
  justify-content: center;
  margin-bottom: 1rem;
}

.footer-link {
  font-size: 0.82rem;
  font-weight: 650;
  color: var(--color-primary);
  text-decoration: none;
  cursor: pointer;
  transition: color var(--transition-fast);
}

.footer-link:hover {
  color: var(--color-primary-hover);
  text-decoration: underline;
}

.sub-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  justify-content: center;
  align-items: center;
  margin-bottom: 0.65rem;
}

.sub-input {
  flex: 1 1 200px;
  max-width: 280px;
}

.sub-btn {
  flex-shrink: 0;
}

.sub-msg {
  font-size: 0.78rem;
  color: var(--color-danger);
  margin-bottom: 0.5rem;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.footer-accent {
  display: none;
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
