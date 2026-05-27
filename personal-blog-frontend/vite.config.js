import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const base = env.VITE_APP_BASE_URL || '/';
  const normalizedBase = base.endsWith('/') ? base : `${base}/`;
  return {
    base: normalizedBase,
    define: {
      global: 'globalThis',
    },
    plugins: [
      vue(),
      VitePWA({
        registerType: 'prompt',
        strategies: 'injectManifest',
        srcDir: 'src',
        filename: 'sw.js',
        injectManifest: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2,webp,json}'],
        },
        includeAssets: ['favicon.svg', 'manifest.json'],
        manifest: false,
        devOptions: { enabled: false },
      }),
    ],
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ws': {
          target: 'http://localhost:8080',
          ws: true,
          changeOrigin: true,
        },
        '/upload': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/rss.xml': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/sitemap.xml': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/robots.txt': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  };
});
