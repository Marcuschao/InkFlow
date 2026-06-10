import { precacheAndRoute, cleanupOutdatedCaches, createHandlerBoundToURL } from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';
import { NetworkFirst, CacheFirst, StaleWhileRevalidate } from 'workbox-strategies';
import { ExpirationPlugin } from 'workbox-expiration';

precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

const indexUrl = `${import.meta.env.BASE_URL.replace(/\/?$/, '/') || '/'}index.html`;

function apiRoots() {
  const configured = (import.meta.env.VITE_APP_API_BASE_URL || '/api').replace(/\/$/, '');
  const normalized = configured.startsWith('/') ? configured : `/${configured}`;
  return [...new Set(['/api', normalized])];
}

const apiDenylist = apiRoots().map((root) => new RegExp(`^${root.replace(/\//g, '\\/')}/`));
registerRoute(new NavigationRoute(createHandlerBoundToURL(indexUrl), { denylist: apiDenylist }));

const API_READ_SEGMENTS = [
  '/articles',
  '/tags',
  '/categories',
  '/about',
  '/search',
  '/links',
  '/site',
  '/share',
];

function isApiReadRequest(url) {
  const path = url.pathname;
  const matchedRoot = apiRoots().some((root) => path === root || path.startsWith(`${root}/`));
  if (!matchedRoot) return false;
  return API_READ_SEGMENTS.some((seg) => path.includes(seg));
}

registerRoute(
  ({ request, url }) => request.method === 'GET' && isApiReadRequest(url),
  new NetworkFirst({
    cacheName: 'api-read',
    networkTimeoutSeconds: 10,
    plugins: [new ExpirationPlugin({ maxEntries: 80, maxAgeSeconds: 86400 })],
  })
);

registerRoute(
  ({ request }) => request.destination === 'image',
  new StaleWhileRevalidate({
    cacheName: 'images',
    plugins: [new ExpirationPlugin({ maxEntries: 60, maxAgeSeconds: 60 * 24 * 3600 })],
  })
);

registerRoute(
  ({ request, url }) =>
    request.method === 'GET' &&
    (url.pathname.startsWith('/upload/') || /\.(?:png|jpg|jpeg|webp|gif|svg|ico)$/i.test(url.pathname)),
  new CacheFirst({
    cacheName: 'static-media',
    plugins: [new ExpirationPlugin({ maxEntries: 40, maxAgeSeconds: 30 * 24 * 3600 })],
  })
);

self.addEventListener('push', (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = { title: 'InkFlow', body: event.data ? event.data.text() : '' };
  }
  const title = data.title || 'InkFlow';
  const scope = self.registration.scope;
  const icon = new URL('icon-192.png', scope).href;
  event.waitUntil(
    self.registration.showNotification(title, {
      body: data.body || '',
      icon,
      data: { url: data.url || '/' },
    })
  );
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const raw = event.notification.data && event.notification.data.url;
  let url = raw || '/';
  try {
    url = new URL(url, self.location.origin).href;
  } catch {
    const path = url.startsWith('/') ? url : `/${url}`;
    url = `${self.location.origin}${path}`;
  }
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const c of clientList) {
        if (c.url === url && 'focus' in c) return c.focus();
      }
      if (self.clients.openWindow) return self.clients.openWindow(url);
    })
  );
});
