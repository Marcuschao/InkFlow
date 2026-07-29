import { createApp } from 'vue';
import { createHead } from '@vueuse/head';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import 'element-plus/dist/index.css';
import { registerSW } from 'virtual:pwa-register';
import App from './App.vue';
import router from './router';
import 'vfonts/Lato.css';
import 'vfonts/FiraCode.css';
import './assets/styles/global.css';
import './assets/styles/element-theme.css';
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
app.use(ElementPlus, { locale: zhCn });

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
