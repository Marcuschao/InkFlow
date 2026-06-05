import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { VitePWA } from 'vite-plugin-pwa';

const BACKEND = 'http://localhost:8080';

function buildProxy(env) {
  const proxy = {
    '/api': {
      target: BACKEND,
      changeOrigin: true,
    },
    '/ws': {
      target: BACKEND,
      ws: true,
      changeOrigin: true,
    },
    '/upload': {
      target: BACKEND,
      changeOrigin: true,
    },
    '/rss.xml': {
      target: BACKEND,
      changeOrigin: true,
    },
    '/sitemap.xml': {
      target: BACKEND,
      changeOrigin: true,
    },
    '/robots.txt': {
      target: BACKEND,
      changeOrigin: true,
    },
    '/oauth2': {
      target: BACKEND,
      changeOrigin: true,
    },
    '/login/oauth2': {
      target: BACKEND,
      changeOrigin: true,
    },
  };

  const apiBase = env.VITE_APP_API_BASE_URL || '';
  if (apiBase.startsWith('/') && apiBase.endsWith('/api')) {
    const prefix = apiBase.slice(0, -4);
    proxy[`${prefix}/api`] = {
      target: BACKEND,
      changeOrigin: true,
      rewrite: (path) => path.replace(new RegExp(`^${prefix.replace(/\//g, '\\/')}/api`), '/api'),
    };
    proxy[`${prefix}/ws`] = {
      target: BACKEND,
      ws: true,
      changeOrigin: true,
      rewrite: (path) => path.replace(new RegExp(`^${prefix.replace(/\//g, '\\/')}/ws`), '/ws'),
    };
    proxy[`${prefix}/oauth2`] = {
      target: BACKEND,
      changeOrigin: true,
      rewrite: (path) => path.replace(new RegExp(`^${prefix.replace(/\//g, '\\/')}/oauth2`), '/oauth2'),
    };
    proxy[`${prefix}/login/oauth2`] = {
      target: BACKEND,
      changeOrigin: true,
      rewrite: (path) => path.replace(new RegExp(`^${prefix.replace(/\//g, '\\/')}/login/oauth2`), '/login/oauth2'),
    };
  }

  return proxy;
}

function normalizeViteBase(raw) {
  if (!raw || raw === '/') return '/';
  let base = raw.trim();
  if (!base.startsWith('/')) base = `/${base}`;
  base = base.replace(/\/+$/, '');
  return `${base}/`;
}

function devBaseUrlRewrite(base) {
  const withoutSlash = base.replace(/\/+$/, '') || '/';
  if (withoutSlash === '/') {
    return null;
  }
  const withSlash = `${withoutSlash}/`;
  return {
    name: 'dev-base-url-rewrite',
    apply: 'serve',
    enforce: 'pre',
    configureServer(server) {
      const handler = (req, _res, next) => {
        const raw = req.url || '/';
        const q = raw.indexOf('?');
        const path = q >= 0 ? raw.slice(0, q) : raw;
        const query = q >= 0 ? raw.slice(q) : '';
        if (path === withoutSlash) {
          req.url = `${withSlash}${query}`;
        }
        next();
      };
      server.middlewares.stack.unshift({ route: '', handle: handler });
    },
  };
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const normalizedBase = normalizeViteBase(env.VITE_APP_BASE_URL);
  return {
    base: normalizedBase,
    define: {
      global: 'globalThis',
    },
    plugins: [
      devBaseUrlRewrite(normalizedBase),
      vue(),
      VitePWA({
        registerType: 'prompt',
        strategies: 'injectManifest',
        srcDir: 'src',
        filename: 'sw.js',
        injectManifest: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2,webp,json}'],
        },
        includeAssets: ['favicon.svg', 'icon-192.png', 'icon-512.png', 'apple-touch-icon.png', 'manifest.json'],
        manifest: false,
        devOptions: { enabled: false },
      }),
    ].filter(Boolean),
    server: {
      proxy: buildProxy(env),
    },
  };
});
