import { createApp } from 'vue';
import { createHead } from '@vueuse/head';
import { createPinia } from 'pinia';
import { registerSW } from 'virtual:pwa-register';
import Particles from '@tsparticles/vue3';
import { loadSlim } from '@tsparticles/slim';
import App from './App.vue';
import router from './router';
import 'vfonts/Lato.css';
import 'vfonts/FiraCode.css';
import './assets/styles/global.css';
import { stripTrailingSlashInBrowserUrl } from './utils/url';

if ('scrollRestoration' in history) {
  history.scrollRestoration = 'manual';
}

const app = createApp(App);
const pinia = createPinia();
const head = createHead();

app.use(pinia);
app.use(router);
app.use(head);
app.use(Particles, {
  init: async (engine) => {
    await loadSlim(engine);
  },
});

router.isReady().then(() => {
  stripTrailingSlashInBrowserUrl();
});

app.mount('#app');

registerSW({
  onNeedRefresh() {
    if (confirm('新版本可用，是否立即刷新？')) {
      location.reload();
    }
  },
});
