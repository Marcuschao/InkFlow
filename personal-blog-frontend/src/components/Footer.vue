<template>
  <footer class="footer">
    <div class="container footer-inner">
      <div class="footer-top">
        <nav class="footer-nav" aria-label="页脚导航">
          <template v-for="item in footerNavItems" :key="item.key">
            <a
              v-if="item.kind === 'external'"
              :href="item.href"
              class="footer-link"
              target="_blank"
              rel="noopener noreferrer"
            >
              <n-button text>{{ item.label }}</n-button>
            </a>
            <router-link v-else-if="item.kind === 'link'" :to="item.to" class="footer-link">
              <n-button text>{{ item.label }}</n-button>
            </router-link>
            <n-button v-else text @click="item.onClick">{{ item.label }}</n-button>
          </template>
        </nav>

        <form class="sub-form" @submit.prevent="onSubscribe">
          <n-input
            v-model:value="subEmail"
            type="text"
            placeholder="邮件订阅（可选）"
            class="sub-input"
          />
          <n-button type="primary" attr-type="submit" :loading="subBusy">
            订阅
          </n-button>
        </form>
      </div>

      <p v-if="isIosSafari" class="footer-hint">
        iOS：Safari 分享 → 添加到主屏幕；16.4+ 主屏幕应用才可能支持推送。
      </p>

      <n-alert v-if="subMsg" type="error" size="small" class="footer-alert">{{ subMsg }}</n-alert>

      <div class="footer-meta">
        <span class="copyright">&copy; 2026 {{ siteStore.siteTitle }} · All rights reserved.</span>
        <span class="uptime">{{ uptimeText }}</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { NAlert, NButton, NInput } from 'naive-ui';
import { subscribeEmail } from '../api/subscribe';
import { useToastStore } from '../stores/toast';
import { useSiteStore } from '../stores/site';
import { useInstallPrompt } from '../composables/useInstallPrompt';
import { subscribeWebPush } from '../composables/useWebPush';

const { canPrompt, promptInstall, isIosSafari } = useInstallPrompt();

const pushBusy = ref(false);

const uptimeText = ref('');
const subEmail = ref('');
const subBusy = ref(false);
const subMsg = ref('');
const toastStore = useToastStore();
const siteStore = useSiteStore();

const launchAt = ref(null);

const footerNavItems = computed(() => {
  const items = [
    { key: 'rss', kind: 'external', href: '/rss.xml', label: 'RSS' },
    { key: 'links', kind: 'link', to: '/links', label: '友链' },
    { key: 'search', kind: 'link', to: '/search', label: '搜索' },
  ];
  if (canPrompt.value) {
    items.push({ key: 'install', kind: 'button', label: '安装到桌面', onClick: onInstall });
  }
  items.push({
    key: 'push',
    kind: 'button',
    label: pushBusy.value ? '…' : '开启通知',
    onClick: onPush,
  });
  return items;
});

async function onInstall() {
  await promptInstall();
}

async function onPush() {
  if (pushBusy.value) return;
  pushBusy.value = true;
  const failMsg = {
    unsupported: '当前浏览器不支持推送',
    network: '无法连接推送服务',
    disabled: '站点暂未开启推送通知',
    denied: '您拒绝了通知权限',
  };
  try {
    const r = await subscribeWebPush();
    if (r.ok) {
      toastStore.push('已开启推送通知', 'success');
    } else {
      toastStore.push(failMsg[r.reason] || '开启推送失败', 'error');
    }
  } catch {
    /* axios 已 toast */
  } finally {
    pushBusy.value = false;
  }
}

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
  return n < 10 ? '0' + n : n;
}

function parseLaunchTime(value) {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

function updateUptime() {
  const launch = launchAt.value;
  if (!launch) {
    uptimeText.value = '';
    return;
  }
  const diff = Date.now() - launch.getTime();
  if (diff <= 0) {
    uptimeText.value = '运行时间：0天00小时00分00秒';
    return;
  }
  const secs = Math.floor(diff / 1000);
  const days = Math.floor(secs / 86400);
  const hrs = Math.floor((secs % 86400) / 3600);
  const mins = Math.floor((secs % 3600) / 60);
  const remainingSecs = secs % 60;
  uptimeText.value = `运行时间：${days}天${pad(hrs)}小时${pad(mins)}分${pad(remainingSecs)}秒`;
}

let timer;
onMounted(() => {
  launchAt.value = parseLaunchTime(siteStore.launchTime);
  updateUptime();
  timer = setInterval(updateUptime, 1000);
});

watch(
  () => siteStore.launchTime,
  (value) => {
    launchAt.value = parseLaunchTime(value);
    updateUptime();
  }
);

onUnmounted(() => {
  clearInterval(timer);
});
</script>

<style scoped>
.footer {
  margin-top: var(--space-12);
  padding: var(--space-12) 0 calc(var(--space-12) + env(safe-area-inset-bottom, 0px));
  background: var(--gradient-footer);
  color: rgba(255, 255, 255, 0.85);
}

.footer :deep(.n-button) {
  color: rgba(255, 255, 255, 0.85);
}

.footer :deep(.n-button:hover) {
  color: #fff;
}

.footer :deep(.n-input) {
  background: rgba(255, 255, 255, 0.08);
}

.footer :deep(.n-input .n-input__input-el) {
  color: #fff;
}

.footer :deep(.n-input .n-input__placeholder) {
  color: var(--color-text-soft);
}

.footer-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-4);
}

.footer-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  width: 100%;
}

.footer-nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: var(--space-1) var(--space-3);
}

.footer-link {
  text-decoration: none;
  color: inherit;
}

.sub-form {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  max-width: 360px;
}

.sub-input {
  flex: 1;
  min-width: 0;
}

.footer-hint {
  max-width: 28rem;
  color: var(--color-text-soft);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.footer-alert {
  width: 100%;
  max-width: 360px;
}

.footer-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-soft);
  font-size: var(--text-sm);
}

.uptime {
  font-size: var(--text-xs);
}

@media (min-width: 768px) {
  .footer-top {
    flex-direction: row;
    justify-content: center;
    flex-wrap: wrap;
    gap: var(--space-6);
  }

  .footer-meta {
    flex-direction: row;
    justify-content: center;
    flex-wrap: wrap;
    gap: var(--space-4);
  }
}

@media (max-width: 767px) {
  .footer {
    padding-bottom: calc(var(--space-8) + var(--mobile-dock-height) + env(safe-area-inset-bottom, 0px));
  }

  .sub-form {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
